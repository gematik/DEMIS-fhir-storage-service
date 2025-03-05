package de.gematik.demis.storage.writer.bundle.hapifhir;

/*-
 * #%L
 * fhir-storage-writer
 * %%
 * Copyright (C) 2025 gematik GmbH
 * %%
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the
 * European Commission – subsequent versions of the EUPL (the "Licence").
 * You may not use this work except in compliance with the Licence.
 *
 * You find a copy of the Licence in the "Licence" file or at
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either expressed or implied.
 * In case of changes by gematik find details in the "Readme" file.
 *
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 * #L%
 */

import static de.gematik.demis.storage.writer.test.LogUtil.hasLogMessage;
import static de.gematik.demis.storage.writer.test.LogUtil.listenToLog;
import static de.gematik.demis.storage.writer.test.TestData.jsonToResource;
import static de.gematik.demis.storage.writer.test.TestData.readResourceAsString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.byLessThan;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import de.gematik.demis.storage.common.entity.BinaryEntity;
import de.gematik.demis.storage.common.entity.BundleEntity;
import de.gematik.demis.storage.common.entity.HapiBundleEntity;
import de.gematik.demis.storage.common.entity.HapiSyncedStatus;
import de.gematik.demis.storage.writer.common.ResourceSavedEvent;
import de.gematik.demis.storage.writer.test.TestData;
import de.gematik.demis.storage.writer.test.TestWithPostgresContainer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
class BundleSavedEventListenerSystemTest extends TestWithPostgresContainer {

  private final CountDownLatch sendSignal = new CountDownLatch(1);
  private final CountDownLatch assertSignal = new CountDownLatch(1);

  @MockBean HapiSender hapiSenderMock;

  @Autowired private ApplicationEventPublisher eventPublisher;

  @Autowired private EntityManager entityManager;
  @Autowired private TransactionTemplate transactionTemplate;

  private ResourceSavedEvent<Bundle, BundleEntity> createBundleEvent() {
    final BundleEntity bundleEntity = TestData.createBundleEntity();
    final Bundle bundle =
        jsonToResource(Bundle.class, readResourceAsString(TestData.BUNDLE_DOCUMENT_FHIR_JSON));
    transactionTemplate.execute(
        txDef -> {
          entityManager.persist(bundleEntity);
          entityManager.flush();
          return null;
        });
    return new ResourceSavedEvent<>(bundle, bundleEntity);
  }

  @Test
  void publishBundleSavedEvent() throws InterruptedException {
    final var event = createBundleEvent();
    final UUID bundleId = event.entity().getId();

    doAnswer(this::sendToHapi).when(hapiSenderMock).sendToHapiInTx(any(), any());

    transactionTemplate.execute(
        txDef -> {
          eventPublisher.publishEvent(event);
          return null;
        });

    final HapiBundleEntity hapiBundleEntity = entityManager.find(HapiBundleEntity.class, bundleId);
    assertThat(hapiBundleEntity).isNotNull();
    assertThat(hapiBundleEntity.getStatus()).isEqualTo(HapiSyncedStatus.NEW);
    assertThat(hapiBundleEntity.getModifiedAt())
        .isCloseTo(LocalDateTime.now(), byLessThan(1, ChronoUnit.SECONDS));
    assertThat(hapiBundleEntity.getHapiId()).isNull();

    sendSignal.countDown();
    assertTrue(assertSignal.await(1, TimeUnit.SECONDS));

    transactionTemplate.execute(
        txDef -> {
          // Note: this sql statement blocks until the asynchronous send action transaction is
          // finished
          final HapiBundleEntity entity =
              entityManager.find(HapiBundleEntity.class, bundleId, LockModeType.PESSIMISTIC_WRITE);
          assertThat(entity).isNotNull();
          assertThat(entity.getStatus()).isEqualTo(HapiSyncedStatus.SYNCED);
          return null;
        });
  }

  private Object sendToHapi(final InvocationOnMock invocationOnMock) throws InterruptedException {
    assertTrue(sendSignal.await(1, TimeUnit.SECONDS));
    assertSignal.countDown();
    final HapiBundleEntity hapiEntity = invocationOnMock.getArgument(0, HapiBundleEntity.class);
    hapiEntity.setStatus(HapiSyncedStatus.SYNCED);
    return null;
  }

  @Test
  void transactionRollback() {
    final var event = createBundleEvent();
    final UUID bundleId = event.entity().getId();

    transactionTemplate.execute(
        txDef -> {
          eventPublisher.publishEvent(event);
          txDef.setRollbackOnly();
          return null;
        });

    final HapiBundleEntity hapiBundleEntity = entityManager.find(HapiBundleEntity.class, bundleId);
    assertThat(hapiBundleEntity).isNull();

    Mockito.verifyNoInteractions(hapiSenderMock);
  }

  @Test
  void alreadySynced() {
    final var logAppender = listenToLog(BundleSavedEventListener.class);

    final var event = createBundleEvent();
    final UUID bundleId = event.entity().getId();

    transactionTemplate.execute(
        txDef -> {
          eventPublisher.publishEvent(event);
          final HapiBundleEntity hapiBundleEntity =
              entityManager.find(HapiBundleEntity.class, bundleId);
          hapiBundleEntity.setStatus(HapiSyncedStatus.SYNCED);
          return null;
        });

    final String expectedLogMessage = String.format("bundle %s already synchronized", bundleId);
    await().atMost(1, TimeUnit.SECONDS).until(() -> hasLogMessage(logAppender, expectedLogMessage));

    // wait until asynchron action is finished
    transactionTemplate.execute(
        txDef -> {
          entityManager.find(HapiBundleEntity.class, bundleId, LockModeType.PESSIMISTIC_WRITE);
          return null;
        });

    Mockito.verifyNoInteractions(hapiSenderMock);
  }

  @Test
  void missingTx() {
    final var event = createBundleEvent();
    final UUID bundleId = event.entity().getId();
    Assertions.assertThrows(
        IllegalTransactionStateException.class, () -> eventPublisher.publishEvent(event));
  }

  @Test
  void ignoreBinaryEvent() throws InterruptedException {
    final UUID id = UUID.randomUUID();
    final BinaryEntity binaryEntity = TestData.createBinaryEntity();
    binaryEntity.setId(id);
    final Binary binary =
        jsonToResource(Binary.class, readResourceAsString(TestData.BINARY_FHIR_JSON));
    final var event = new ResourceSavedEvent<>(binary, binaryEntity);

    transactionTemplate.execute(
        txDef -> {
          eventPublisher.publishEvent(event);
          return null;
        });

    final HapiBundleEntity hapiBundleEntity = entityManager.find(HapiBundleEntity.class, id);
    assertThat(hapiBundleEntity).isNull();

    Mockito.verifyNoInteractions(hapiSenderMock);
  }
}
