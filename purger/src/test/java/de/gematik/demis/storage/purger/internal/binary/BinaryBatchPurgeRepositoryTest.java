package de.gematik.demis.storage.purger.internal.binary;

/*-
 * #%L
 * fhir-storage-purger
 * %%
 * Copyright (C) 2025 - 2026 gematik GmbH
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
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik,
 * find details in the "Readme" file.
 * #L%
 */

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.gematik.demis.storage.purger.common.batches.BatchesConfiguration;
import de.gematik.demis.storage.purger.common.periods.PeriodsConfiguration;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.Period;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BinaryBatchPurgeRepositoryTest {

  private static final int LIMIT_VALUE = 10;
  private static final String LIMIT_PARAM_NAME = "batchSize";
  private static final UUID DELETED_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final String DEFAULT_PERIOD_PARAM_NAME = "defaultPeriodInDays";
  private static final int DEFAULT_PERIOD_VALUE = 30;

  @Mock private EntityManager entityManager;
  @Mock private BatchesConfiguration batchesConfiguration;
  @Mock private PeriodsConfiguration periodsConfiguration;
  @Mock private Query query;
  @InjectMocks private BinaryBatchPurgeRepository repository;

  @Test
  void givenValidInputsWhenDeleteExpiredBinariesThenReturnList() {

    // given
    mockConfiguration();
    mockJpa();
    repository.initialize();

    // when
    List<UUID> batch = repository.purgeExpiredRecords();

    // then
    verifyNativeQuery();
    assertThat(batch).hasSize(1).containsExactly(DELETED_ID);
  }

  private void mockConfiguration() {
    when(batchesConfiguration.size()).thenReturn(LIMIT_VALUE);
    when(periodsConfiguration.defaultPeriod()).thenReturn(Period.ofDays(DEFAULT_PERIOD_VALUE));
  }

  private void mockJpa() {
    // delete
    when(entityManager.createNativeQuery(any())).thenReturn(query);
    when(query.setParameter(LIMIT_PARAM_NAME, LIMIT_VALUE)).thenReturn(query);
    when(query.setParameter(DEFAULT_PERIOD_PARAM_NAME, DEFAULT_PERIOD_VALUE)).thenReturn(query);
    when(query.getResultList()).thenReturn(List.of(DELETED_ID));
  }

  private void verifyNativeQuery() {
    ArgumentCaptor<String> capturedNativeQueries = ArgumentCaptor.forClass(String.class);
    verify(entityManager, times(1)).createNativeQuery(capturedNativeQueries.capture());
    List<String> nativeQueries = capturedNativeQueries.getAllValues();
    assertThat(nativeQueries).hasSize(1);
    assertThat(nativeQueries.getFirst())
        .contains("binaries")
        .contains(DEFAULT_PERIOD_PARAM_NAME)
        .contains(LIMIT_PARAM_NAME);
    verify(query, times(1)).setParameter(LIMIT_PARAM_NAME, LIMIT_VALUE);
    verify(query, times(1)).setParameter(DEFAULT_PERIOD_PARAM_NAME, DEFAULT_PERIOD_VALUE);
    verify(query, times(1)).getResultList();
  }
}
