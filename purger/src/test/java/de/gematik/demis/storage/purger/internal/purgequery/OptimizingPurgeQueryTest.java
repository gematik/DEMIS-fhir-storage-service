package de.gematik.demis.storage.purger.internal.purgequery;

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
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik,
 * find details in the "Readme" file.
 * #L%
 */

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import de.gematik.demis.storage.purger.common.periods.PeriodsConfiguration;
import de.gematik.demis.storage.purger.common.periods.ResponsibleDepartmentPeriod;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OptimizingPurgeQueryTest {

  @Mock private PeriodsConfiguration periodsConfiguration;
  @Mock private DefaultPeriodPurgeQuery defaultPeriodPurgeQuery;
  @Mock private ResponsibleDepartmentPeriodsPurgeQuery responsibleDepartmentPeriodsPurgeQuery;
  @Mock private ResponsibleDepartmentPeriod responsibleDepartmentPeriod;
  @InjectMocks private OptimizingPurgeQuery optimizingPurgeQuery;

  @Test
  void givenNoResponsibleDepartmentPeriodsWhenGetThenDefaultPeriodPurgeQuery() {
    // given
    final String defaultPeriodQuery = "default period query";
    when(defaultPeriodPurgeQuery.get()).thenReturn(defaultPeriodQuery);

    // when
    final String query = optimizingPurgeQuery.get();

    // then
    assertThat(query).isEqualTo(defaultPeriodQuery);
  }

  @Test
  void givenResponsibleDepartmentPeriodsWhenGetThenResponsibleDepartmentPeriodsPurgeQuery() {
    // given
    final String responsibleDepartmentQuery = "responsible department query";
    when(periodsConfiguration.responsibleDepartments())
        .thenReturn(List.of(responsibleDepartmentPeriod));
    when(responsibleDepartmentPeriodsPurgeQuery.get()).thenReturn(responsibleDepartmentQuery);

    // when
    final String query = optimizingPurgeQuery.get();

    // then
    assertThat(query).isEqualTo(responsibleDepartmentQuery);
  }
}
