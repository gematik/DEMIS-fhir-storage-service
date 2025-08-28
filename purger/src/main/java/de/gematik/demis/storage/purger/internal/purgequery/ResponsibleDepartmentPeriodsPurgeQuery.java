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
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 * #L%
 */

import de.gematik.demis.storage.purger.common.periods.PeriodsConfiguration;
import de.gematik.demis.storage.purger.common.periods.ResponsibleDepartmentPeriod;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@Slf4j
final class ResponsibleDepartmentPeriodsPurgeQuery implements PurgeQuery {

  /**
   * SQL statement to delete expired records from the database. Applied are the default period and
   * the individual responsible department periods. The responsible department periods are added
   * through a common table expression (CTE) in the SQL statement, namely a WITH statement that adds
   * a table named <i>periods</i> that contains only two columns: <i>responsible_department</i> and
   * <i>period</i>.
   *
   * <p>There are two sections in the WHERE clause:
   *
   * <ol>
   *   <li>records that are not in the responsible department periods and are older than the default
   *       period
   *   <li>records that are in the responsible department periods and are older than the individual
   *       period
   * </ol>
   */
  private static final String STATEMENT_PURGE_DEPARTMENT_PERIODS =
      """
              , to_delete AS (
                  SELECT ctid
                  FROM ${TABLE} b
                  WHERE (
                      b.responsible_department NOT IN (SELECT responsible_department FROM periods)
                      AND b.last_updated < (CURRENT_TIMESTAMP - make_interval(days => :defaultPeriodInDays))
                  )
                  OR (
                      b.responsible_department IN (SELECT responsible_department FROM periods)
                      AND b.last_updated < CURRENT_TIMESTAMP - INTERVAL '1 day' * (
                          SELECT period
                          FROM periods
                          WHERE periods.responsible_department = b.responsible_department
                      )
                  )
                  LIMIT :batchSize
              )
              DELETE FROM ${TABLE}
              WHERE ctid IN (SELECT ctid FROM to_delete)
              RETURNING id;
            """;

  private final PeriodsConfiguration periodsConfiguration;

  @Override
  public String get() {
    return createResponsibleDepartmentPeriodsCommonTable()
        + "\n"
        + STATEMENT_PURGE_DEPARTMENT_PERIODS;
  }

  private String createResponsibleDepartmentPeriodsCommonTable() {
    final List<ResponsibleDepartmentPeriod> periods = periodsConfiguration.responsibleDepartments();
    if ((periods == null) || periods.isEmpty()) {
      throw new IllegalArgumentException("No responsible department periods found");
    }
    final StringBuilder sql = new StringBuilder("WITH periods AS (");
    ResponsibleDepartmentPeriod period;
    for (int i = 0; i < periods.size(); ++i) {
      if (i > 0) {
        sql.append("\nUNION ALL");
      }
      period = periods.get(i);
      sql.append("\nSELECT '")
          .append(period.department())
          .append("' AS responsible_department, ")
          .append(period.period().getDays())
          .append(" AS period");
    }
    sql.append("\n)");
    String sqlCommonTable = sql.toString();
    log.info(
        "Created periods SQL common table for responsible department periods:\n{}", sqlCommonTable);
    return sqlCommonTable;
  }
}
