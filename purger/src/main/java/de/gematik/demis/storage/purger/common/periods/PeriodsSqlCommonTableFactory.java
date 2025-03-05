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

import java.time.Period;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Factory to build selective deletion period configuration as SQL common table expression.
 *
 * <pre>
 *     WITH periods AS (
 *      SELECT 'default-period' AS rule, NULL AS value, 30 AS period
 *      UNION ALL
 *      SELECT 'responsible-department', '1.01.0.53.', 60
 *      UNION ALL
 *      SELECT 'responsible-department', '1.02.0.74.', 50
 *      UNION ALL
 *      SELECT 'bundle-profile', 'https://demis.rki.de/fhir/StructureDefinition/NotificationBundleSequence', 40
 *     )
 * </pre>
 */
@RequiredArgsConstructor
@Slf4j
public final class PeriodsSqlCommonTableFactory implements Supplier<String> {

  private final PeriodsConfiguration periodsConfiguration;

  @Override
  public String get() {
    final StringBuilder sql = new StringBuilder("WITH periods AS (");
    sql.append("\nSELECT 'default-period' AS rule, NULL AS value, ")
        .append(defaultPeriod().getDays())
        .append(" AS period");
    for (ResponsibleDepartmentPeriod responsibleDepartment : responsibleDepartments()) {
      sql.append("\nUNION ALL");
      sql.append("\nSELECT 'responsible-department', '")
          .append(responsibleDepartment.department())
          .append("', ")
          .append(responsibleDepartment.period().getDays());
    }
    for (BundleProfilePeriod bundleProfile : bundleProfiles()) {
      sql.append("\nUNION ALL");
      sql.append("\nSELECT 'bundle-profile', '")
          .append(bundleProfile.uri())
          .append("', ")
          .append(bundleProfile.period().getDays());
    }
    sql.append("\n)");
    String sqlCommonTable = sql.toString();
    log.debug("Prepared periods SQL common table expression:\n{}", sqlCommonTable);
    return sqlCommonTable;
  }

  private Period defaultPeriod() {
    return Objects.requireNonNull(
        periodsConfiguration.defaultPeriod(), "Default period must be set");
  }

  private List<BundleProfilePeriod> bundleProfiles() {
    return Objects.requireNonNullElse(
        periodsConfiguration.bundleProfiles(), Collections.emptyList());
  }

  private List<ResponsibleDepartmentPeriod> responsibleDepartments() {
    return Objects.requireNonNullElse(
        periodsConfiguration.responsibleDepartments(), Collections.emptyList());
  }
}
