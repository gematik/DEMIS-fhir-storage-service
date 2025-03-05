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

import de.gematik.demis.storage.common.entity.BundleEntity;
import de.gematik.demis.storage.common.entity.HapiBundleEntity;
import de.gematik.demis.storage.common.entity.HapiSyncedStatus;
import de.gematik.demis.storage.common.reader.BundleMapper;
import de.gematik.demis.storage.writer.config.ConditionalOnHapiSync;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

@ConditionalOnHapiSync
@Service
@RequiredArgsConstructor
@Slf4j
public class SynchronizationCronJob {

  private final TransactionTemplate transactionTemplate;
  private final HapiEventRepository repository;
  private final HapiSender hapiSender;
  private final EntityManager entityManager;
  private final BundleMapper bundleMapper;

  @Scheduled(cron = "${fss.hapi.cron}")
  public void sendAllRetainingEvents() {
    log.info("start cron job to send all new bundles to hapi fhir jpa server");
    Boolean found;
    do {
      found = transactionTemplate.execute(this::processNextNewBundle);
    } while (found != null && found);
    log.info("finished");
  }

  private boolean processNextNewBundle(final TransactionStatus status) {
    final Optional<HapiBundleEntity> dbResult =
        repository.findFirstByStatusEquals(HapiSyncedStatus.NEW);
    final boolean found = dbResult.isPresent();
    if (found) {
      final HapiBundleEntity hapiBundleEntity = dbResult.get();
      final UUID bundleId = hapiBundleEntity.getBundleId();
      final Bundle bundle = loadBundle(bundleId);
      hapiSender.sendToHapiInTx(hapiBundleEntity, bundle);
      Assert.state(hapiBundleEntity.getStatus() != HapiSyncedStatus.NEW, "must be processed.");
    }
    return found;
  }

  private Bundle loadBundle(final UUID bundleId) {
    final BundleEntity bundleEntity = entityManager.find(BundleEntity.class, bundleId);
    return bundleMapper.entityToResource(bundleEntity);
  }
}
