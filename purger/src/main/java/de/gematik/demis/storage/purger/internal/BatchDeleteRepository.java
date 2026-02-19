package de.gematik.demis.storage.purger.internal;

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

import de.gematik.demis.storage.purger.common.batches.BatchesConfiguration;
import de.gematik.demis.storage.purger.common.periods.PeriodsConfiguration;
import de.gematik.demis.storage.purger.internal.purgequery.PurgeQuery;
import de.gematik.demis.storage.purger.internal.purgequery.PurgeQueryBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

/**
 * Repository for deleting expired records in batches. As our two resource tables, binaries and
 * bundles, are treated the same on purging, the SQL statements and execution is defined abstractly.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public abstract class BatchDeleteRepository {

  @PersistenceContext private final EntityManager entityManager;
  private final BatchesConfiguration batchesConfiguration;
  private final PeriodsConfiguration periodsConfiguration;

  private PurgeQuery purgeQuery;

  @PostConstruct
  public void initialize() {
    purgeQuery =
        new PurgeQueryBuilder()
            .setPeriodsConfiguration(periodsConfiguration)
            .setTableName(getTable())
            .build();
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
    return entityManager
        .createNativeQuery(purgeQuery.get())
        .setParameter(
            purgeQuery.getDefaultPeriodParameterName(),
            periodsConfiguration.defaultPeriod().getDays())
        .setParameter(purgeQuery.getBatchSizeParameterName(), batchesConfiguration.size())
        .getResultList();
  }

  protected abstract String getTable();
}
