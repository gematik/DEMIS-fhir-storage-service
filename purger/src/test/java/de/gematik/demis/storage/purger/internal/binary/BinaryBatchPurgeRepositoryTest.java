package de.gematik.demis.storage.purger.internal.binary;

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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.gematik.demis.storage.purger.common.batches.BatchesConfiguration;
import de.gematik.demis.storage.purger.common.periods.PeriodsConfiguration;
import de.gematik.demis.storage.purger.test.SqlQueries;
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

  private static final int LIMIT = 10;
  private static final String LIMIT_PARAM_NAME = "limit";
  private static final UUID DELETED_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final String SQL_DELETE_BATCH =
      """
WITH periods AS (SELECT 'default-period' AS rule, NULL AS value, 30 AS period)    DELETE
      FROM binaries
     WHERE id IN (SELECT id
                    FROM (SELECT b.id,
                                 b.last_updated,
                                 COALESCE(p2.period, p1.period) AS period_applied
                            FROM binaries b
                            JOIN periods p1 ON p1.rule = 'default-period'
                       LEFT JOIN periods p2 ON p2.rule = 'responsible-department' AND b.responsible_department = p2.value
                         ) AS b1
                   WHERE (EXTRACT(DAY FROM (CURRENT_TIMESTAMP - b1.last_updated))) >= period_applied
                   LIMIT :limit
                 )
    RETURNING id;
""";

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
    final List<String> nativeQueries = getNativeQueries();
    verifyDeleteStatement(nativeQueries);
    assertThat(batch).hasSize(1).containsExactly(DELETED_ID);
  }

  private void mockConfiguration() {
    when(batchesConfiguration.size()).thenReturn(LIMIT);
    when(periodsConfiguration.defaultPeriod()).thenReturn(Period.ofDays(30));
    when(periodsConfiguration.responsibleDepartments()).thenReturn(List.of());
    when(periodsConfiguration.bundleProfiles()).thenReturn(List.of());
  }

  private void mockJpa() {
    // delete
    when(entityManager.createNativeQuery(any())).thenReturn(query);
    when(query.setParameter(LIMIT_PARAM_NAME, LIMIT)).thenReturn(query);
    when(query.getResultList()).thenReturn(List.of(DELETED_ID));
  }

  private List<String> getNativeQueries() {
    ArgumentCaptor<String> capturedNativeQueries = ArgumentCaptor.forClass(String.class);
    verify(entityManager, times(1)).createNativeQuery(capturedNativeQueries.capture());
    return capturedNativeQueries.getAllValues();
  }

  private void verifyDeleteStatement(List<String> nativeQueries) {
    assertThat(SqlQueries.normalize(nativeQueries.getFirst()))
        .as("delete binaries statement with selective deletion periods table")
        .isEqualTo(SqlQueries.normalize(SQL_DELETE_BATCH));
    verify(query, times(1)).setParameter(LIMIT_PARAM_NAME, LIMIT);
    verify(query, times(1)).getResultList();
  }
}
