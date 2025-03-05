package de.gematik.demis.storage.purger.common.periods;

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
import static org.mockito.Mockito.when;

import java.time.Period;
import java.util.Collections;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PeriodsSqlCommonTableFactoryTest {

  @Mock private PeriodsConfiguration periodsConfiguration;

  @Test
  void givenNullDefaultPeriodWhenCreateSqlCommonTableThenException() {
    when(periodsConfiguration.defaultPeriod()).thenReturn(null);
    final PeriodsSqlCommonTableFactory factory =
        new PeriodsSqlCommonTableFactory(periodsConfiguration);
    Assertions.assertThatException()
        .isThrownBy(factory::get)
        .withMessage("Default period must be set");
  }

  @Test
  void givenDefaultPeriodWhenAsSqlCommonTableThenTable() {

    // given
    when(periodsConfiguration.defaultPeriod()).thenReturn(Period.ofDays(30));

    // when
    final String sqlCommonTable = new PeriodsSqlCommonTableFactory(periodsConfiguration).get();

    // then
    assertThat(sqlCommonTable)
        .isEqualTo(
            "WITH periods AS (\nSELECT 'default-period' AS rule, NULL AS value, 30 AS period\n)");
  }

  @Test
  void givenResponsibleDepartmentAndBundleProfileWhenAsSqlCommonTableThenTable() {

    // given
    when(periodsConfiguration.defaultPeriod()).thenReturn(Period.ofDays(30));
    when(periodsConfiguration.responsibleDepartments())
        .thenReturn(
            Collections.singletonList(
                new ResponsibleDepartmentPeriod("dep.42", Period.ofDays(60))));
    when(periodsConfiguration.bundleProfiles())
        .thenReturn(
            Collections.singletonList(
                new BundleProfilePeriod(
                    "https://demis.rki.de/fhir/StructureDefinition/NotificationBundleSequence",
                    Period.ofDays(10))));

    // when
    final String sqlCommonTable = new PeriodsSqlCommonTableFactory(periodsConfiguration).get();

    // then
    assertThat(sqlCommonTable)
        .isEqualTo(
            """
                                    WITH periods AS (
                                    SELECT 'default-period' AS rule, NULL AS value, 30 AS period
                                    UNION ALL
                                    SELECT 'responsible-department', 'dep.42', 60
                                    UNION ALL
                                    SELECT 'bundle-profile', 'https://demis.rki.de/fhir/StructureDefinition/NotificationBundleSequence', 10
                                    )""");
  }

  @Test
  void givenSingleResponsibleDepartmentWhenAsSqlCommonTableThenTable() {

    // given
    when(periodsConfiguration.defaultPeriod()).thenReturn(Period.ofDays(30));
    when(periodsConfiguration.responsibleDepartments())
        .thenReturn(
            Collections.singletonList(
                new ResponsibleDepartmentPeriod("dep.42", Period.ofDays(60))));

    // when
    final String sqlCommonTable = new PeriodsSqlCommonTableFactory(periodsConfiguration).get();

    // then
    assertThat(sqlCommonTable)
        .isEqualTo(
            """
                                    WITH periods AS (
                                    SELECT 'default-period' AS rule, NULL AS value, 30 AS period
                                    UNION ALL
                                    SELECT 'responsible-department', 'dep.42', 60
                                    )""");
  }

  @Test
  void givenTwoResponsibleDepartmentsWhenAsSqlCommonTableThenTable() {

    // given
    when(periodsConfiguration.defaultPeriod()).thenReturn(Period.ofDays(30));
    when(periodsConfiguration.responsibleDepartments())
        .thenReturn(
            List.of(
                new ResponsibleDepartmentPeriod("dep.42", Period.ofDays(60)),
                new ResponsibleDepartmentPeriod("dep.43", Period.ofDays(50))));

    // when
    final String sqlCommonTable = new PeriodsSqlCommonTableFactory(periodsConfiguration).get();

    // then
    assertThat(sqlCommonTable)
        .isEqualTo(
            """
                                    WITH periods AS (
                                    SELECT 'default-period' AS rule, NULL AS value, 30 AS period
                                    UNION ALL
                                    SELECT 'responsible-department', 'dep.42', 60
                                    UNION ALL
                                    SELECT 'responsible-department', 'dep.43', 50
                                    )""");
  }

  @Test
  void givenSingleBundleProfileWhenAsSqlCommonTableThenTable() {

    // given
    when(periodsConfiguration.defaultPeriod()).thenReturn(Period.ofDays(30));
    when(periodsConfiguration.bundleProfiles())
        .thenReturn(
            Collections.singletonList(
                new BundleProfilePeriod(
                    "https://demis.rki.de/fhir/StructureDefinition/NotificationBundleSequence",
                    Period.ofDays(10))));

    // when
    final String sqlCommonTable = new PeriodsSqlCommonTableFactory(periodsConfiguration).get();

    // then
    assertThat(sqlCommonTable)
        .isEqualTo(
            """
                                            WITH periods AS (
                                            SELECT 'default-period' AS rule, NULL AS value, 30 AS period
                                            UNION ALL
                                            SELECT 'bundle-profile', 'https://demis.rki.de/fhir/StructureDefinition/NotificationBundleSequence', 10
                                            )""");
  }

  @Test
  void givenTwoBundleProfilesWhenAsSqlCommonTableThenTable() {

    // given
    when(periodsConfiguration.defaultPeriod()).thenReturn(Period.ofDays(30));
    when(periodsConfiguration.bundleProfiles())
        .thenReturn(
            List.of(
                new BundleProfilePeriod(
                    "https://demis.rki.de/fhir/StructureDefinition/NotificationBundleSequence",
                    Period.ofDays(10)),
                new BundleProfilePeriod(
                    "https://demis.rki.de/fhir/StructureDefinition/NotificationBundleVaccineInjury",
                    Period.ofDays(20))));

    // when
    final String sqlCommonTable = new PeriodsSqlCommonTableFactory(periodsConfiguration).get();

    // then
    assertThat(sqlCommonTable)
        .isEqualTo(
            """
                                            WITH periods AS (
                                            SELECT 'default-period' AS rule, NULL AS value, 30 AS period
                                            UNION ALL
                                            SELECT 'bundle-profile', 'https://demis.rki.de/fhir/StructureDefinition/NotificationBundleSequence', 10
                                            UNION ALL
                                            SELECT 'bundle-profile', 'https://demis.rki.de/fhir/StructureDefinition/NotificationBundleVaccineInjury', 20
                                            )""");
  }
}
