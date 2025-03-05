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

import de.gematik.demis.storage.purger.Purge;
import java.net.InetAddress;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequiredArgsConstructor
@Slf4j
final class BatchProcessorFactory {

  private final BatchesConfiguration batchesConfiguration;

  /**
   * Create new state object for batch-based purging
   *
   * @param purge purge
   * @return batch context
   */
  public BatchProcessor createBatchContext(Purge purge) {
    final String resource = purge.name();
    final Logger logger = LoggerFactory.getLogger(purge.getClass());
    return createBatchContext(logger, resource);
  }

  BatchProcessor createBatchContext(Logger logger, String resource) {
    final String podName = podName();
    final AuditLog batchLog = new AuditLog(batchesConfiguration, logger, podName, resource);
    return new BatchProcessor(batchesConfiguration, batchLog, podName);
  }

  private String podName() {
    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (Exception e) {
      log.error("Failed to get pod name", e);
      return "fhir-storage-purger-pod";
    }
  }
}
