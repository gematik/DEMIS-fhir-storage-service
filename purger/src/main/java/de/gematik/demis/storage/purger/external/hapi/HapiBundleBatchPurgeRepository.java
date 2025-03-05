package de.gematik.demis.storage.purger.external.hapi;

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
import de.gematik.demis.storage.purger.external.AttemptsConfiguration;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@Slf4j
class HapiBundleBatchPurgeRepository {

  static final String PARAM_PURGER_ID = "purgerId";

  /**
   * This is a batch-wise marking of records for deletion. The expired bundle records already have
   * been deleted! Here we search records whose column <code>BUNDLE_ID</code> points to a
   * non-existing record in the <code>BUNDLES</code> table.
   *
   * <ol>
   *   <li>The <code>LEFT JOIN</code> clause together with the <code>WHERE b.id IS NULL</code>
   *       clause filters the records that are expired.
   *   <li>The <code>AND h.hapi_id IS NOT NULL</code> clause filters the records that actually exist
   *       in the HAPI FHIR server.
   *   <li>The purger column conditions make sure we only grab records that are not blocked.
   *   <li>The <code>LIMIT</code> clause limits the number of records to delete according to the
   *       configured batch size
   *   <li>The <code>UPDATE</code> statement marks the records and increments the attempt counter.
   *   <li>The <code>RETURNING</code> clause returns everything we need to know about the marked
   *       records.
   * </ol>
   */
  private static final String UPDATE_MARK_BATCH =
      """
        UPDATE hapi_bundles
           SET purger_id = :purgerId,
               purger_started = CURRENT_TIMESTAMP,
               purger_attempt = COALESCE(purger_attempt, 0) + 1
         WHERE bundle_id IN (
            SELECT h.bundle_id
              FROM hapi_bundles h
              LEFT JOIN bundles b ON h.bundle_id = b.id
             WHERE b.id IS NULL
               AND h.hapi_id IS NOT NULL
               AND h.purger_id IS NULL
               AND ((h.purger_attempt IS NULL)
                        OR ((h.purger_attempt < :attempts)
                                AND (FLOOR(EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - h.purger_started)) / 60) >= :timeout)))
             LIMIT :limit
                    )
        RETURNING bundle_id, hapi_id, purger_attempt::int;
      """;

  private static final String DELETE = "DELETE FROM hapi_bundles WHERE purger_id = :purgerId";
  private static final String DELETE_WITH_EXCLUSIONS =
      "DELETE FROM hapi_bundles WHERE purger_id = :purgerId AND bundle_id NOT IN :excludedBundleIds";
  private static final String UPDATE_RESET =
      "UPDATE hapi_bundles SET purger_id = NULL WHERE purger_id = :purgerId";

  @PersistenceContext private final EntityManager entityManager;
  private final AttemptsConfiguration attemptsConfiguration;
  private final BatchesConfiguration batchesConfiguration;

  @Transactional
  @SuppressWarnings("unchecked")
  public List<HapiBundle> markExpiredRecords(String purgerId) {
    return this.entityManager
        .createNativeQuery(UPDATE_MARK_BATCH, HapiBundle.class)
        .setParameter(PARAM_PURGER_ID, purgerId)
        .setParameter("attempts", attemptsConfiguration.limit())
        .setParameter("timeout", attemptsConfiguration.timeout().toMinutes())
        .setParameter("limit", batchesConfiguration.size())
        .getResultList();
  }

  /**
   * Updates purger ID and started timestamp to null for all records with the given purger ID.
   *
   * @param purgerId purger ID
   */
  @Transactional
  public void resetPurge(String purgerId) {
    this.entityManager
        .createNativeQuery(UPDATE_RESET)
        .setParameter(PARAM_PURGER_ID, purgerId)
        .executeUpdate();
  }

  /**
   * Deletes all marked records with the given purger ID.
   *
   * @param purgerId purger ID
   * @param excluded list of bundles to be excluded from deletion
   */
  @Transactional
  public void deleteExpiredRecords(String purgerId, List<HapiBundle> excluded) {
    if (excluded.isEmpty()) {
      deleteBatch(purgerId);
    } else {
      deleteBatchWithExclusions(purgerId, excluded);
    }
  }

  private void deleteBatch(String purgerId) {
    this.entityManager
        .createNativeQuery(DELETE)
        .setParameter(PARAM_PURGER_ID, purgerId)
        .executeUpdate();
  }

  private void deleteBatchWithExclusions(String purgerId, List<HapiBundle> excluded) {
    this.entityManager
        .createNativeQuery(DELETE_WITH_EXCLUSIONS)
        .setParameter(PARAM_PURGER_ID, purgerId)
        .setParameter("excludedBundleIds", excluded.stream().map(HapiBundle::bundleId).toList())
        .executeUpdate();
    resetPurge(purgerId);
  }
}
