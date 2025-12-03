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

import de.gematik.demis.storage.purger.common.periods.PeriodsConfiguration;
import java.util.Objects;
import lombok.Setter;

/** Builder for creating native SQL queries used in the purger. */
@Setter
public final class PurgeQueryBuilder {

  private String tableName;
  private PeriodsConfiguration periodsConfiguration;

  /**
   * Builds a new native query.
   *
   * @return a new native query
   */
  public PurgeQuery build() {
    final var periods = getPeriodsConfiguration();
    return new CachingPurgeQuery(
        new TableNameReplacingPurgeQuery(
            getTableName(),
            new OptimizingPurgeQuery(
                periods,
                new DefaultPeriodPurgeQuery(),
                new ResponsibleDepartmentPeriodsPurgeQuery(periods))));
  }

  private PeriodsConfiguration getPeriodsConfiguration() {
    return Objects.requireNonNull(periodsConfiguration);
  }

  private String getTableName() {
    return Objects.requireNonNull(tableName);
  }
}
