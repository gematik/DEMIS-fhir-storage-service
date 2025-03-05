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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.gematik.demis.storage.purger.common.batches.BatchesConfiguration;
import de.gematik.demis.storage.purger.external.AttemptsConfiguration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HapiBundleBatchPurgeTest {

  private static final int LIMIT = 10;

  private static final List<HapiBundle> HAPI_BUNDLES =
      List.of(
          new HapiBundle(UUID.randomUUID(), "hapiId1", 1),
          new HapiBundle(UUID.randomUUID(), "hapiId2", 1),
          new HapiBundle(UUID.randomUUID(), "hapiId3", 1));

  @Mock private AttemptsConfiguration attemptsConfiguration;
  @Mock private BatchesConfiguration batchesConfiguration;
  @Mock private HapiBundleBatchPurgeRepository repository;
  @Mock private HapiFhirServerService hapiFhirServerService;
  @InjectMocks private HapiBundleBatchPurge hapiBundleBatchPurge;

  /**
   * This is the happy day test. We give us an incomplete first batch. So there are just a few rows
   * to delete in the database. The HAPI FHIR server responds nicely, and we can delete all
   * resources.
   */
  @Test
  void givenIncompleteBatchWhenRunThenDelete() {
    // given
    when(batchesConfiguration.size()).thenReturn(LIMIT);
    when(repository.markExpiredRecords(any())).thenReturn(HAPI_BUNDLES);
    when(hapiFhirServerService.purge(HAPI_BUNDLES)).thenReturn(Collections.emptyList());

    // when
    final CompletableFuture<Void> purge = hapiBundleBatchPurge.run();

    // then
    assertThat(purge)
        .as("successful completion of purge")
        .isCompleted()
        .isNotCompletedExceptionally();
    verify(repository, times(1)).markExpiredRecords(any());
    verify(repository, times(1)).deleteExpiredRecords(any(), any());
    verify(repository, times(0)).resetPurge(any());
    verify(hapiFhirServerService, times(1)).purge(HAPI_BUNDLES);
  }

  @Test
  void givenPartialFailureWhenHapiThenPartialDelete() {

    // given
    when(attemptsConfiguration.limit()).thenReturn(3);
    when(batchesConfiguration.size()).thenReturn(LIMIT);
    when(repository.markExpiredRecords(any())).thenReturn(HAPI_BUNDLES);
    when(hapiFhirServerService.purge(HAPI_BUNDLES)).thenReturn(List.of(HAPI_BUNDLES.getLast()));

    // when
    final CompletableFuture<Void> purge = hapiBundleBatchPurge.run();

    // then
    assertThat(purge)
        .as("successful completion of purge")
        .isCompleted()
        .isNotCompletedExceptionally();
    verify(repository, times(1)).markExpiredRecords(any());
    verify(repository, times(1)).deleteExpiredRecords(any(), argThat(l -> l.size() == 1));
    verify(hapiFhirServerService, times(1)).purge(HAPI_BUNDLES);
  }

  @Test
  void givenExceptionWhenHapiThenExceptionallyComplete() {

    // given
    when(batchesConfiguration.size()).thenReturn(LIMIT);
    when(repository.markExpiredRecords(any())).thenReturn(HAPI_BUNDLES);
    when(hapiFhirServerService.purge(HAPI_BUNDLES))
        .thenThrow(new IllegalStateException("Simulating HAPI FHIR server failure"));

    // when
    final CompletableFuture<Void> purge = hapiBundleBatchPurge.run();

    // then
    assertThat(purge).as("exceptionally completion of purge").isCompletedExceptionally();
    verify(repository, times(1)).markExpiredRecords(any());
    verify(repository, times(0)).deleteExpiredRecords(any(), any());
    verify(repository, times(1)).resetPurge(any());
    verify(hapiFhirServerService, times(1)).purge(HAPI_BUNDLES);
  }

  @Test
  void givenEmptyBatchWhenRunThenComplete() {
    // given
    when(batchesConfiguration.size()).thenReturn(LIMIT);
    when(repository.markExpiredRecords(any())).thenReturn(Collections.emptyList());

    // when
    final CompletableFuture<Void> purge = hapiBundleBatchPurge.run();

    // then
    assertThat(purge)
        .as("successful completion of purge")
        .isCompleted()
        .isNotCompletedExceptionally();
    verify(repository, times(1)).markExpiredRecords(any());
    verify(repository, times(0)).deleteExpiredRecords(any(), any());
    verify(repository, times(0)).resetPurge(any());

    verify(hapiFhirServerService, times(0)).purge(any());
  }
}
