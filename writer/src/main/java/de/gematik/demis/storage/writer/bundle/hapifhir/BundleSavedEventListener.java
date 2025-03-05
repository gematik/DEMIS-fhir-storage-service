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

import static jakarta.transaction.Transactional.TxType.MANDATORY;
import static jakarta.transaction.Transactional.TxType.REQUIRES_NEW;

import de.gematik.demis.storage.common.entity.BundleEntity;
import de.gematik.demis.storage.common.entity.HapiBundleEntity;
import de.gematik.demis.storage.common.entity.HapiSyncedStatus;
import de.gematik.demis.storage.writer.common.ResourceSavedEvent;
import de.gematik.demis.storage.writer.config.ConditionalOnHapiSync;
import jakarta.transaction.Transactional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@RequiredArgsConstructor
@ConditionalOnHapiSync
@Service
@Slf4j
public class BundleSavedEventListener {

  private final HapiEventRepository repository;
  private final HapiSender hapiSender;

  /**
   * Creates and saves a hapi bundle entity with the same id as the bundle. This is done inside the
   * transaction of the caller. That means, if the bundle save action is rollback, this hapi row
   * will be also rollback.
   */
  @EventListener
  @Transactional(MANDATORY)
  public void onBundleSavedInTx(
      final ResourceSavedEvent<? extends Bundle, ? extends BundleEntity> event) {
    final HapiBundleEntity hapiBundleEntity = new HapiBundleEntity();
    hapiBundleEntity.setBundleId(event.entity().getId());
    repository.save(hapiBundleEntity);
    log.debug("Hapi bundle Sync event created {}", hapiBundleEntity);
  }

  /**
   * sends the bundle to the Hapi Fhir Server. This is done asynchronous after commit. That means
   * this actions has no impact on the save action.
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Async("hapiFhirExecutor")
  @Transactional(REQUIRES_NEW)
  public void onBundleSavedAfterCommit(
      final ResourceSavedEvent<? extends Bundle, ? extends BundleEntity> event) {
    log.debug("onBundleSavedAfterCommit");
    final UUID bundleId = event.entity().getId();
    final HapiBundleEntity hapiBundleEntity = repository.findByBundleId(bundleId).orElseThrow();
    if (hapiBundleEntity.getStatus() == HapiSyncedStatus.NEW) {
      hapiSender.sendToHapiInTx(hapiBundleEntity, event.resource());
    } else {
      log.debug("bundle {} already synchronized", bundleId);
    }
  }
}
