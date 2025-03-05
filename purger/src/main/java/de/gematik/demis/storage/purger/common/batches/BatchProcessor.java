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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
final class BatchProcessor implements Batch {

  private final CompletableFuture<Void> future = new CompletableFuture<>();

  private final BatchesConfiguration batchesConfiguration;
  private final AuditLog auditLog;
  private final String podName;

  private String batchName;
  private int iteration;
  private int total;

  private static List<String> printIds(List<UUID> entities) {
    return entities.stream().map(UUID::toString).toList();
  }

  boolean initNextBatch() {
    if (future.isDone()) {
      return false;
    }
    iteration++;
    batchName = podName + "-batch-" + iteration;
    if (iteration == 1) {
      auditLog.start();
    }
    auditLog.startBatch(batchName);
    return true;
  }

  @Override
  public String name() {
    return Objects.requireNonNull(batchName);
  }

  @Override
  public void complete(List<UUID> batch) {
    complete(batch, Collections.emptyList());
  }

  @Override
  public void complete(List<UUID> batch, List<UUID> failures) {
    final List<UUID> succeeded = new ArrayList<>(batch);
    succeeded.removeAll(failures);
    registerCompletedBatch(succeeded, failures);
    completeIfFullyPurged(succeeded);
  }

  private void registerCompletedBatch(List<UUID> succeeded, List<UUID> failures) {
    total += succeeded.size();
    auditLog.endBatch(batchName, printIds(succeeded), printIds(failures));
  }

  private void completeIfFullyPurged(List<UUID> batch) {
    if (batch.size() < batchesConfiguration.size()) {
      auditLog.endOnCompletion(total);
      future.complete(null);
    } else if (iteration == batchesConfiguration.limit()) {
      auditLog.endOnBatchLimit(total);
      future.complete(null);
    }
  }

  @Override
  public void completeExceptionally(Throwable t) {
    auditLog.endExceptionally(total, t);
    future.completeExceptionally(t);
  }

  CompletableFuture<Void> future() {
    return future;
  }
}
