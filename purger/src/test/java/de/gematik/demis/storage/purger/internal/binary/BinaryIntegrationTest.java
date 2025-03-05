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

import de.gematik.demis.storage.common.entity.BinaryEntity;
import de.gematik.demis.storage.purger.internal.InternalPurgeIntegrationTest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;

/**
 * Integration test for purging binaries. Tested features:
 *
 * <ul>
 *   <li>default deletion period
 *   <li>selective deletion period "responsible department"
 *   <li>process multiple batches
 *   <li>end on incomplete batch
 * </ul>
 *
 * <h2>Test data</h2>
 *
 * <p>15 binaries are created, 8 of them are expired. 3 batches will be purged with the last one
 * incomplete which signals the end of the process.
 */
class BinaryIntegrationTest extends InternalPurgeIntegrationTest {

  @Autowired private BinaryIntegrationTestRepository repository;
  private ActiveRecords activeRecords;
  private ExpiredRecords expiredRecords;

  @Override
  protected CrudRepository<BinaryEntity, UUID> getRepository() {
    return repository;
  }

  @Override
  protected void initialize(ActiveRecords activeRecords, ExpiredRecords expiredRecords) {
    this.activeRecords = activeRecords;
    this.expiredRecords = expiredRecords;
    createBinaries();
  }

  private void createBinaries() {
    for (int i = 1; i <= 3; i++) {
      createDefaultPeriodBinaries(i);
    }
    for (int i = 1; i <= 2; i++) {
      createSelectivePeriodBinaries(i);
    }
  }

  private void createDefaultPeriodBinaries(int i) {
    activeRecords.accept(
        "binary with normal department and no expired period " + i,
        createBinary(DEPARTMENT_DEFAULT, PERIOD_NOT_EXPIRED));
    expiredRecords.accept(
        "binary with normal department and expired default period " + i,
        createBinary(DEPARTMENT_DEFAULT, PERIOD_DEFAULT_EXPIRED));
    expiredRecords.accept(
        "binary with normal department and expired extended department period " + i,
        createBinary(DEPARTMENT_DEFAULT, PERIOD_DEPARTMENT_EXPIRED));
  }

  private void createSelectivePeriodBinaries(int i) {
    activeRecords.accept(
        "binary with extended department and no expired period " + i,
        createBinary(DEPARTMENT_SPECIAL, PERIOD_NOT_EXPIRED));
    activeRecords.accept(
        "binary with extended department and expired default period " + i,
        createBinary(DEPARTMENT_SPECIAL, PERIOD_DEFAULT_EXPIRED));
    expiredRecords.accept(
        "binary with extended department and expired extended department period " + i,
        createBinary(DEPARTMENT_SPECIAL, PERIOD_DEPARTMENT_EXPIRED));
  }

  private UUID createBinary(String department, Instant lastUpdated) {
    final BinaryEntity binary = new BinaryEntity();
    binary.setResponsibleDepartment(department);
    binary.setData(BINARY);
    binary.setContentType("application/fhir+json");
    binary.setSourceId(UUID.randomUUID().toString());
    repository.save(binary);
    final UUID binaryId = binary.getId();
    repository.updateLastUpdatedTimestamps(toOffsetDateTime(lastUpdated), List.of(binaryId));
    return binaryId;
  }
}
