package de.gematik.demis.storage.purger.external.hapi;

/*-
 * #%L
 * fhir-storage-purger
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

import de.gematik.demis.storage.purger.common.batches.Batch;
import de.gematik.demis.storage.purger.common.batches.BatchPurge;
import de.gematik.demis.storage.purger.common.batches.BatchesConfiguration;
import de.gematik.demis.storage.purger.external.AttemptsConfiguration;
import de.gematik.demis.storage.purger.external.ExternalPurge;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Purges expired bundles from HAPI FHIR server. Basic concept is that expired bundles have already
 * been deleted from table BUNDLES. Now there are rows in table HAPI_BUNDLES with unresolved values
 * in column HAPI_BUNDLES.BUNDLE_ID. We proceed in batches. For each batch we first mark the rows
 * with in column PURGER_ID. Then we try to delete the bundles from HAPI FHIR server. On success,
 * they are also deleted from the table.
 */
@ConditionalOnHapiSync
@Service
@Slf4j
class HapiBundleBatchPurge extends BatchPurge implements ExternalPurge {

  private final HapiBundleBatchPurgeRepository repository;
  private final HapiFhirServerService hapiFhirServerService;
  private final AttemptsConfiguration attemptsConfiguration;

  HapiBundleBatchPurge(
      BatchesConfiguration batchesConfiguration,
      AttemptsConfiguration attemptsConfiguration,
      HapiBundleBatchPurgeRepository repository,
      HapiFhirServerService hapiFhirServerService) {
    super(batchesConfiguration);
    this.attemptsConfiguration = attemptsConfiguration;
    this.repository = repository;
    this.hapiFhirServerService = hapiFhirServerService;
  }

  private static List<String> toBundleIds(List<HapiBundle> bundles) {
    return bundles.stream().map(HapiBundle::bundleId).map(UUID::toString).toList();
  }

  @Override
  protected void purgeBatch(Batch batch) {
    final List<HapiBundle> bundles = markBatch(batch);
    if (bundles.isEmpty()) {
      batch.complete(Collections.emptyList());
    } else {
      deleteFromHapiFhirServer(batch, bundles);
    }
  }

  private void deleteFromHapiFhirServer(Batch batch, List<HapiBundle> bundles) {
    final List<HapiBundle> failures;
    try {
      failures = hapiFhirServerService.purge(bundles);
    } catch (Exception e) {
      logFailedAttempts(bundles);
      completeExceptionally(batch, e);
      return;
    }
    logFailedAttempts(failures);
    complete(batch, bundles, failures);
  }

  private void complete(Batch batch, List<HapiBundle> bundles, List<HapiBundle> failures) {
    if (failures.size() == bundles.size()) {
      completeExceptionally(
          batch,
          new IllegalStateException(
              "All bundles of batch failed to be purged from HAPI FHIR server."));
    } else {
      repository.deleteExpiredRecords(batch.name(), failures);
      batch.complete(getBundleIds(bundles), getBundleIds(failures));
    }
  }

  private void completeExceptionally(Batch batch, Exception e) {
    repository.resetPurge(batch.name());
    batch.completeExceptionally(e);
  }

  private List<HapiBundle> markBatch(Batch batch) {
    final List<HapiBundle> bundles = repository.markExpiredRecords(batch.name());
    log.info("Loaded bundles for purging. Batch: {} Bundles: {}", batch.name(), bundles.size());
    return bundles;
  }

  private List<UUID> getBundleIds(List<HapiBundle> bundles) {
    return bundles.stream().map(HapiBundle::bundleId).toList();
  }

  private void logFailedAttempts(List<HapiBundle> failures) {
    if (!failures.isEmpty()) {
      final int maxAttempts = attemptsConfiguration.limit();
      final List<HapiBundle> exhaustedIds =
          failures.stream().filter(b -> b.purgerAttempt() >= maxAttempts).toList();
      final List<HapiBundle> increasedIds = new ArrayList<>(failures);
      increasedIds.removeAll(exhaustedIds);
      if (!increasedIds.isEmpty()) {
        log.warn(
            "Failed to delete bundles from HAPI FHIR server. Raised attempt counters. Bundles: {}",
            toBundleIds(increasedIds));
      }
      if (!exhaustedIds.isEmpty()) {
        log.error(
            "FATAL - Failed to delete bundles from HAPI FHIR server. Maximum number of attempts reached! Maximum: {} Bundles: {}",
            maxAttempts,
            toBundleIds(exhaustedIds));
      }
    }
  }

  @Override
  public String name() {
    return "hapi-bundle";
  }
}
