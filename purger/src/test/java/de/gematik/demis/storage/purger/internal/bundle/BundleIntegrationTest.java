package de.gematik.demis.storage.purger.internal.bundle;

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

import de.gematik.demis.storage.common.entity.BundleEntity;
import de.gematik.demis.storage.purger.internal.InternalPurgeIntegrationTest;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;

class BundleIntegrationTest extends InternalPurgeIntegrationTest {

  @Autowired private BundleIntegrationTestRepository repository;
  private ActiveRecords activeRecords;
  private ExpiredRecords expiredRecords;

  @Override
  protected CrudRepository<BundleEntity, UUID> getRepository() {
    return repository;
  }

  @Override
  protected void initialize(ActiveRecords activeRecords, ExpiredRecords expiredRecords) {
    this.activeRecords = activeRecords;
    this.expiredRecords = expiredRecords;
    createBundles();
  }

  private void createBundles() {
    for (int i = 1; i <= 3; i++) {
      createDefaultPeriodBundles(i);
    }
    for (int i = 1; i <= 2; i++) {
      createSelectivePeriodBundles(i);
    }
  }

  private void createDefaultPeriodBundles(int i) {
    activeRecords.accept(
        "bundle with normal department and no expired period " + i,
        createBundle(DEPARTMENT_DEFAULT, PERIOD_NOT_EXPIRED));
    expiredRecords.accept(
        "bundle with normal department and expired default period " + i,
        createBundle(DEPARTMENT_DEFAULT, PERIOD_DEFAULT_EXPIRED));
    expiredRecords.accept(
        "bundle with normal department and expired extended department period " + i,
        createBundle(DEPARTMENT_DEFAULT, PERIOD_DEPARTMENT_EXPIRED));
  }

  private void createSelectivePeriodBundles(int i) {
    activeRecords.accept(
        "bundle with extended department and no expired period " + i,
        createBundle(DEPARTMENT_SPECIAL, PERIOD_NOT_EXPIRED));
    activeRecords.accept(
        "bundle with extended department and expired default period " + i,
        createBundle(DEPARTMENT_SPECIAL, PERIOD_DEFAULT_EXPIRED));
    expiredRecords.accept(
        "bundle with extended department and expired extended department period " + i,
        createBundle(DEPARTMENT_SPECIAL, PERIOD_DEPARTMENT_EXPIRED));
  }

  private UUID createBundle(String department, Instant lastUpdated) {
    final BundleEntity bundle = new BundleEntity();
    bundle.setResponsibleDepartment(department);
    bundle.setContent("binary content");
    bundle.setNotificationBundleId(UUID.randomUUID().toString());
    bundle.setNotificationId(UUID.randomUUID().toString());
    bundle.setProfile("https://demis.rki.de/fhir/StructureDefinition/NotificationBundleDisease");
    bundle.setTags(Collections.emptyList());
    bundle.setSourceId(UUID.randomUUID().toString());
    repository.save(bundle);
    final UUID id = bundle.getId();
    repository.updateLastUpdatedTimestamps(toOffsetDateTime(lastUpdated), List.of(id));
    return id;
  }
}
