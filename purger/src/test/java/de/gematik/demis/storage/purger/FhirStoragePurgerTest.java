package de.gematik.demis.storage.purger;

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

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.gematik.demis.storage.purger.internal.InternalPurge;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FhirStoragePurgerTest {

  @Mock private InternalPurge internalPurge1;
  @Mock private InternalPurge internalPurge2;
  private FhirStoragePurger fhirStoragePurger;

  @BeforeEach
  void init() {
    fhirStoragePurger =
        new FhirStoragePurger(
            List.of(internalPurge1, internalPurge2), Collections.emptyList(), true);
  }

  @Test
  void givenPurgeListWhenRunThenRunAllPurges() {

    // given
    when(internalPurge1.run()).thenReturn(CompletableFuture.completedFuture(null));
    when(internalPurge2.run()).thenReturn(CompletableFuture.completedFuture(null));

    // when
    runJobWithWatchdog();

    // then
    verify(internalPurge1, times(1)).run();
    verify(internalPurge2, times(1)).run();
  }

  @Test
  void givenFutureExceptionWhenRunThenLogError() {

    // given
    when(internalPurge1.name()).thenReturn("test-resource");
    when(internalPurge1.run())
        .thenReturn(
            CompletableFuture.failedFuture(new IllegalStateException("Simulating purging error")));
    when(internalPurge2.run()).thenReturn(CompletableFuture.completedFuture(null));

    // when
    runJobWithWatchdog();

    // then
    verify(internalPurge1, times(1)).run();
    verify(internalPurge2, times(1)).run();
  }

  @Test
  void givenExceptionWhenRunThenLogError() {

    // given
    when(internalPurge1.run()).thenThrow(new IllegalStateException("Simulating purging error"));
    when(internalPurge2.run()).thenReturn(CompletableFuture.completedFuture(null));

    // when
    runJobWithWatchdog();

    // then
    verify(internalPurge1, times(1)).run();
    verify(internalPurge2, times(1)).run();
  }

  private void runJobWithWatchdog() {
    await("purger job terminates").atMost(10L, TimeUnit.SECONDS).until(this::runJob);
  }

  private boolean runJob() {
    fhirStoragePurger.run();
    return true;
  }
}
