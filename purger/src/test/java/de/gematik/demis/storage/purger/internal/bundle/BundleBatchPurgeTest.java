package de.gematik.demis.storage.purger.internal.bundle;

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

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.gematik.demis.storage.purger.common.batches.Batch;
import de.gematik.demis.storage.purger.common.batches.BatchesConfiguration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BundleBatchPurgeTest {

  @Mock private BatchesConfiguration batchesConfiguration;
  @Mock private BundleBatchPurgeRepository repository;
  @Mock private Batch batch;
  @InjectMocks private BundleBatchPurge bundleBatchPurge;

  @Test
  void givenNormalStateWhenNameThenBundle() {
    assertEquals("bundle", bundleBatchPurge.name());
  }

  @Test
  void givenBatchSizeFiveWhenPurgeBatchThenPurgeFiveAtRepositoryAndComplete() {

    // given
    final List<UUID> purgedBundles =
        IntStream.range(0, 5).mapToObj(i -> randomUUID()).collect(Collectors.toList());
    when(repository.purgeExpiredRecords()).thenReturn(purgedBundles);

    // when
    bundleBatchPurge.purgeBatch(batch);

    // then
    verify(repository, times(1)).purgeExpiredRecords();
    verify(batch, times(1)).complete(purgedBundles);
  }

  @Test
  void givenExceptionWhenPurgeExpiredRecordsThenCompleteExceptionally() {

    // given
    mockBatches();
    when(repository.purgeExpiredRecords())
        .thenThrow(new IllegalStateException("Simulating database failure"));

    // when
    CompletableFuture<Void> future = bundleBatchPurge.run();
    Awaitility.await("future is completed exceptionally")
        .atMost(5L, TimeUnit.SECONDS)
        .until(future::isCompletedExceptionally);

    // then
    verify(repository, times(1)).purgeExpiredRecords();
    verify(batch, times(0)).complete(Collections.emptyList(), Collections.emptyList());
  }

  private void mockBatches() {
    when(batchesConfiguration.size()).thenReturn(5);
  }
}
