package de.gematik.demis.storage.purger.common.batches;

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

import java.util.List;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AllArgsConstructor
final class AuditLog {

  private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");
  private static final int DELETION_LOG_BATCH_SIZE = 100;

  private final long startTime = System.currentTimeMillis();
  private final BatchesConfiguration batchesConfiguration;
  private final Logger log;
  private final String podName;
  private final String resource;

  public void start() {
    log.info(
        "Starting purging {} based on batches. Pod: {} BatchSize: {} BatchLimit: {}",
        resource,
        podName,
        batchesConfiguration.size(),
        batchesConfiguration.limit());
  }

  public void endOnCompletion(int total) {
    if (log.isInfoEnabled()) {
      if (total > 0) {
        log.info(
            "Ended purging {} by reaching end of expired records. Total: {} Duration: {}",
            resource,
            total,
            createDurationText());
      } else {
        log.info("Ended purging {}. No resources found to purge.", resource);
      }
    }
  }

  private String createDurationText() {
    long durationMillis = System.currentTimeMillis() - startTime;
    return DurationFormatUtils.formatDuration(durationMillis, "[d'd'H'h'm'm's's']");
  }

  public void endOnBatchLimit(int total) {
    if (log.isInfoEnabled()) {
      log.info(
          "Ended purging {} by batch limit. Total: {} Duration: {} BatchSize: {} BatchLimit: {}",
          resource,
          total,
          createDurationText(),
          batchesConfiguration.size(),
          batchesConfiguration.limit());
    }
  }

  public void endExceptionally(int total, Throwable t) {
    log.error("Ended purging {} due to an exception. Total: {}", resource, total, t);
  }

  public void startBatch(String batch) {
    log.info("Starting batch purging {}. Batch: {}", resource, batch);
  }

  public void endBatch(String batch, List<String> ids, List<String> failedIds) {
    if (log.isInfoEnabled()) {
      log.info(
          "Completed batch purging of {}. Batch: {} Deleted: {} Failed: {}",
          resource,
          batch,
          ids.size(),
          failedIds.size());
      int end;
      for (int i = 0; i < ids.size(); i += DELETION_LOG_BATCH_SIZE) {
        end = Math.min(i + DELETION_LOG_BATCH_SIZE, ids.size());
        AUDIT.info(
            "Purger deleted FHIR: {}! Batch: {} Block: {} DeletedIds: {}",
            resource,
            batch,
            i + 1,
            String.join(", ", ids.subList(i, end)));
      }
    }
  }
}
