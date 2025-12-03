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

import de.gematik.demis.storage.purger.common.periods.PeriodsConfiguration;
import de.gematik.demis.storage.purger.common.periods.ResponsibleDepartmentPeriod;
import de.gematik.demis.storage.purger.test.SqlQueries;
import java.time.Period;
import java.util.Collections;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class ResponsibleDepartmentPeriodsPurgeQueryTest {

  private static final String QUERY_VALID =
      """
      WITH
      periods AS (
        SELECT '1.0.21.0' AS responsible_department, 60 AS period
        UNION ALL
        SELECT '1.0.31.0' AS responsible_department, 90 AS period
        UNION ALL
        SELECT '1.0.42.0' AS responsible_department, 120 AS period
      ),
      to_delete AS (
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
                  WHERE periods.responsible_department = b.responsible_department)
               )
        LIMIT :batchSize
      )
      DELETE FROM ${TABLE} WHERE ctid IN (SELECT ctid FROM to_delete)
      RETURNING id;
    """;

  @Test
  void givenValidInputWhenGetThenStatement() {
    // given
    final ResponsibleDepartmentPeriodsPurgeQuery query =
        new ResponsibleDepartmentPeriodsPurgeQuery(
            new PeriodsConfiguration(
                Period.ofDays(30),
                List.of(
                    new ResponsibleDepartmentPeriod("1.0.21.0", Period.ofDays(60)),
                    new ResponsibleDepartmentPeriod("1.0.31.0", Period.ofDays(90)),
                    new ResponsibleDepartmentPeriod("1.0.42.0", Period.ofDays(120)))));

    // when
    final String result = query.get();

    // then
    assertThat(result).isNotBlank();
    assertThat(SqlQueries.normalize(result))
        .as(result)
        .isEqualTo(SqlQueries.normalize(QUERY_VALID));
  }

  @Test
  void givenInvalidInputWhenGetThenException() {
    final ResponsibleDepartmentPeriodsPurgeQuery query =
        new ResponsibleDepartmentPeriodsPurgeQuery(
            new PeriodsConfiguration(Period.ofDays(30), Collections.emptyList()));
    Assertions.assertThatException().isThrownBy(query::get);
  }
}
