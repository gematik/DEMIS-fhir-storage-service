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

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
class BatchProcessorTest {

  private static final String RESOURCE = "test-binary";
  private static final String SUFFIX_BATCH_1 = "-batch-1";
  private static final int BATCH_SIZE = 10;

  @Mock private BatchesConfiguration batchesConfiguration;
  @Mock private Logger logger;
  private BatchProcessor batchProcessor;

  static List<UUID> generate(int count) {
    return IntStream.range(0, count).mapToObj(i -> randomUUID()).collect(Collectors.toList());
  }

  @BeforeEach
  void initBatchPurge() {
    final BatchProcessorFactory batchPurgeFactory = new BatchProcessorFactory(batchesConfiguration);
    batchProcessor = batchPurgeFactory.createBatchContext(logger, RESOURCE);
  }

  @Test
  void givenBatchNotInitializedWhenBatchNameThenThrowException() {
    assertThatException().isThrownBy(batchProcessor::name);
  }

  @Test
  void givenBatchInitializedWhenBatchNameThenReturnBatchName() {
    mockBatches();

    assertThat(batchProcessor.initNextBatch()).isTrue();
    assertThat(batchProcessor.name())
        .isNotEmpty()
        .endsWith(SUFFIX_BATCH_1)
        .hasSizeGreaterThan(SUFFIX_BATCH_1.length());
  }

  @Test
  void givenLessThanBatchSizeWhenCompleteThenCompletePurge() {

    // given
    mockBatches();
    List<UUID> incompleteBatchPurgedIds = generate(BATCH_SIZE - 1);

    // when
    assertThat(batchProcessor.initNextBatch()).isTrue();
    batchProcessor.complete(incompleteBatchPurgedIds, Collections.emptyList());

    // then
    assertThat(batchProcessor.future().isDone()).isTrue();
    assertThat(batchProcessor.initNextBatch()).isFalse();
  }

  @Test
  void givenBatchLimitZeroAndFullyPurgedBatchWhenCompleteThenIterate() {

    // given
    mockBatches();
    when(batchesConfiguration.limit()).thenReturn(0);
    List<UUID> completeBatchPurgedIds = generate(BATCH_SIZE);

    // when
    assertThat(batchProcessor.initNextBatch()).as("purging batch 1").isTrue();
    batchProcessor.complete(completeBatchPurgedIds, Collections.emptyList());

    // then
    assertThat(batchProcessor.future().isDone()).isFalse();
    assertThat(batchProcessor.initNextBatch()).as("purging batch 2").isTrue();
  }

  @Test
  void givenBatchLimitOneAndFullyPurgedBatchWhenCompleteThenEndOnBatchLimit() {

    // given
    mockBatches();
    when(batchesConfiguration.limit()).thenReturn(1);
    List<UUID> completeBatchPurgedIds = generate(BATCH_SIZE);

    // when
    assertThat(batchProcessor.initNextBatch()).as("purging batch 1").isTrue();
    batchProcessor.complete(completeBatchPurgedIds, Collections.emptyList());

    // then
    assertThat(batchProcessor.future().isDone()).isTrue();
    assertThat(batchProcessor.initNextBatch()).as("purging batch 2").isFalse();
  }

  private void mockBatches() {
    when(batchesConfiguration.size()).thenReturn(BATCH_SIZE);
  }
}
