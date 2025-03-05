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

import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import de.gematik.demis.storage.common.entity.BundleEntity;
import de.gematik.demis.storage.common.entity.HapiBundleEntity;
import de.gematik.demis.storage.common.entity.HapiSyncedStatus;
import de.gematik.demis.storage.writer.test.TestData;
import de.gematik.demis.storage.writer.test.TestWithPostgresContainer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
@Slf4j
class SynchronizationCronJobIntegrationSystemTest extends TestWithPostgresContainer {

  private final CountDownLatch startSignal = new CountDownLatch(1);
  private final CountDownLatch doneSignal = new CountDownLatch(1);

  @MockBean HapiSender hapiSenderMock;
  @Autowired SynchronizationCronJob underTest;
  @Autowired EntityManager entityManager;
  @Autowired TransactionTemplate transactionTemplate;

  private UUID lockedRowId;
  private UUID rowIdOfToProcessResultingSynced;
  private UUID rowIdOfToProcessResultingError;

  /** setup db data: 3 hapiBundle rows + 3 assigned bundle rows */
  private Void initData() {
    log.info("Initializing test data");
    lockedRowId = createBundleWithHapiRow();
    rowIdOfToProcessResultingSynced = createBundleWithHapiRow();
    rowIdOfToProcessResultingError = createBundleWithHapiRow();
    return null;
  }

  private UUID createBundleWithHapiRow() {
    final BundleEntity bundleEntity = TestData.createBundleEntity();
    entityManager.persist(bundleEntity);
    final var bundleId = bundleEntity.getId();
    final HapiBundleEntity hapiBundleEntity = new HapiBundleEntity().setBundleId(bundleId);
    entityManager.persist(hapiBundleEntity);
    return bundleId;
  }

  @Test
  void run() throws Exception {
    transactionTemplate.execute(status -> initData());

    // lock first row in hapi bundle table in own thread that holds the lock during test execution
    // the lock must not block the execution of the cron job
    supplyAsync(this::lockRow);

    // simulate rest call. One call will result in error state and one in synced state
    doAnswer(this::sendToHapi).when(hapiSenderMock).sendToHapiInTx(any(), any());

    // we wait with cronjob execution until row is locked from other thread
    startSignal.await();
    underTest.sendAllRetainingEvents();
    doneSignal.countDown();

    assertHapiStatus(rowIdOfToProcessResultingSynced, HapiSyncedStatus.SYNCED);
    assertHapiStatus(rowIdOfToProcessResultingError, HapiSyncedStatus.ERROR);
    // the locked row must be skipped
    assertHapiStatus(lockedRowId, HapiSyncedStatus.NEW);
  }

  private Object sendToHapi(final InvocationOnMock invocation) {
    final HapiBundleEntity hapiEntity = invocation.getArgument(0, HapiBundleEntity.class);
    final UUID bundleId = hapiEntity.getBundleId();
    if (rowIdOfToProcessResultingSynced.equals(bundleId)) {
      hapiEntity.setStatus(HapiSyncedStatus.SYNCED);
    } else if (rowIdOfToProcessResultingError.equals(bundleId)) {
      hapiEntity.setStatus(HapiSyncedStatus.ERROR);
    } else {
      throw new IllegalStateException("Unexpected bundle id: " + bundleId);
    }
    return null;
  }

  private Object lockRow() {
    transactionTemplate.execute(
        status -> {
          entityManager.find(HapiBundleEntity.class, lockedRowId, LockModeType.PESSIMISTIC_WRITE);
          log.info("row locked");

          startSignal.countDown();
          // now we wait until cronjob has finished
          try {
            doneSignal.await();
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
          log.info("row released");
          return null;
        });
    return null;
  }

  private void assertHapiStatus(final UUID id, final HapiSyncedStatus expectedStatus) {
    final HapiBundleEntity hapiBundleEntity = entityManager.find(HapiBundleEntity.class, id);
    assertThat(hapiBundleEntity).isNotNull();
    assertThat(hapiBundleEntity.getStatus()).isEqualTo(expectedStatus);
  }
}
