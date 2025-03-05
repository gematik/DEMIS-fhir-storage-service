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
import java.util.concurrent.CompletableFuture;

/** Purges resources in batches */
public abstract class BatchPurge implements Purge {

  private final BatchesConfiguration batchesConfiguration;
  private BatchProcessor batchProcessor;

  protected BatchPurge(BatchesConfiguration batchesConfiguration) {
    this.batchesConfiguration = batchesConfiguration;
  }

  @Override
  public final CompletableFuture<Void> run() {
    initialize();
    purge();
    return batchProcessor.future();
  }

  private void initialize() {
    if (batchProcessor == null) {
      batchProcessor = new BatchProcessorFactory(batchesConfiguration).createBatchContext(this);
    }
  }

  private void purge() {
    while (batchProcessor.initNextBatch()) {
      try {
        purgeBatch(batchProcessor);
      } catch (Exception cause) {
        batchProcessor
            .future()
            .completeExceptionally(
                new IllegalStateException(
                    "Failed to purge batch: " + batchProcessor.name(), cause));
      }
    }
  }

  /**
   * Purge a batch of resources. On completion, call {@link Batch#complete} to register the result.
   *
   * @param batch batch to purge
   */
  protected abstract void purgeBatch(Batch batch);
}
