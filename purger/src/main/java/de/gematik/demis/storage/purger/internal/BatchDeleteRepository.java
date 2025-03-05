package de.gematik.demis.storage.purger.internal;

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

import de.gematik.demis.storage.purger.common.batches.BatchesConfiguration;
import de.gematik.demis.storage.purger.common.periods.PeriodsConfiguration;
import de.gematik.demis.storage.purger.common.periods.PeriodsSqlCommonTableFactory;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

/** Repository for deleting expired records in batches. */
@Repository
@RequiredArgsConstructor
@Slf4j
public abstract class BatchDeleteRepository {

  /**
   * This statement will be executed with an upfront WITH-table that contains the periods!
   *
   * <pre>WITH periods AS (SELECT 'default-period' AS rule, NULL AS value, 30 AS period)</pre>
   *
   * The following steps are executed:
   *
   * <ol>
   *   <li>The <code>JOIN</code> clause adds the default period column to the table (binaries or
   *       bundles).
   *   <li>the <code>LEFT JOIN</code> clause adds the responsible department period column to the
   *       table.
   *   <li>The <code>COALESCE</code> function selects the right period for each record.
   *   <li>The <code>WHERE</code> clause filters the records that are expired.
   *   <li>The <code>LIMIT</code> clause limits the number of records to delete according to the
   *       configured batch size.
   *   <li>The <code>DELETE</code> statement directly deletes the records from the binaries/bundle
   *       table.
   *   <li>The <code>RETURNING</code> clause returns the IDs of the deleted records.
   * </ol>
   */
  private static final String DELETE =
      """
        DELETE
          FROM ${TABLE}
         WHERE id IN (
          SELECT id
            FROM (
              SELECT b.id,
                     b.last_updated,
                     COALESCE(p2.period, p1.period) AS period_applied
                FROM ${TABLE} b
                JOIN periods p1 ON p1.rule = 'default-period'
                LEFT JOIN periods p2 ON p2.rule = 'responsible-department' AND b.responsible_department = p2.value
                ) AS b1
               WHERE (EXTRACT(DAY FROM (CURRENT_TIMESTAMP - b1.last_updated))) >= period_applied
               LIMIT :limit
               )
        RETURNING id;
      """;

  @PersistenceContext private final EntityManager entityManager;
  private final BatchesConfiguration batchesConfiguration;
  private final PeriodsConfiguration periodsConfiguration;

  private String periodsTable;

  @PostConstruct
  public void initialize() {
    if (periodsTable == null) {
      periodsTable = new PeriodsSqlCommonTableFactory(periodsConfiguration).get();
    }
  }

  /**
   * Deletes expired binary records from the database. The number of records to delete is limited by
   * the <code>limit</code> parameter.
   *
   * @return list of deleted binary IDs
   */
  @SuppressWarnings("unchecked")
  @Transactional
  public List<UUID> purgeExpiredRecords() {
    return this.entityManager
        .createNativeQuery(nativeQuery())
        .setParameter("limit", batchesConfiguration.size())
        .getResultList();
  }

  private String nativeQuery() {
    return periodsTable + DELETE.replace("${TABLE}", getTable());
  }

  protected abstract String getTable();
}
