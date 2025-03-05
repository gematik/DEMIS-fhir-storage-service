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

import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.demis.storage.common.entity.BinaryEntity;
import de.gematik.demis.storage.common.entity.BundleEntity;
import de.gematik.demis.storage.purger.FhirStoragePurger;
import de.gematik.demis.storage.purger.FhirStoragePurgerJob;
import de.gematik.demis.storage.purger.external.hapi.PurgerHapiBundleEntity;
import de.gematik.demis.storage.purger.test.TestLog;
import de.gematik.demis.storage.purger.test.TestWithPostgresContainer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for purging a single type of resource: binary or bundle.
 *
 * <ul>
 *   <li>Save resource entities in testcontainer Postgres
 *   <li>Run the purger job
 *   <li>Verify that the expired resources are deleted and the active resources are not
 * </ul>
 */
@SpringBootTest(properties = {"fss.purger.batches.size=3", "fss.purger.batches.limit=3"})
@ActiveProfiles("test")
@EntityScan(
    basePackageClasses = {BundleEntity.class, PurgerHapiBundleEntity.class, BinaryEntity.class})
@Slf4j
public abstract class InternalPurgeIntegrationTest extends TestWithPostgresContainer {

  protected static final byte[] BINARY =
      "010203abba-binary-data-of-an-encoded-fhir-json-document".getBytes(StandardCharsets.UTF_8);
  protected static final Instant PERIOD_DEFAULT_EXPIRED =
      Instant.now().minus(1, ChronoUnit.HOURS).minus(30, ChronoUnit.DAYS);
  protected static final Instant PERIOD_NOT_EXPIRED = Instant.now().minus(5, ChronoUnit.DAYS);

  /** department with extended deletion period */
  protected static final String DEPARTMENT_SPECIAL = "1.01.0.53.";

  /** department with default deletion period */
  protected static final String DEPARTMENT_DEFAULT = "1.01.0.42.";

  protected static final Instant PERIOD_DEPARTMENT_EXPIRED =
      Instant.now().minus(1, ChronoUnit.HOURS).minus(60, ChronoUnit.DAYS);

  private final Map<String, UUID> expectedActiveRecords = new HashMap<>();
  private final Map<String, UUID> expectedExpiredRecords = new HashMap<>();

  @Autowired private FhirStoragePurger fhirStoragePurger;
  private TestLog auditLog;

  /**
   * Initialize everything needed for the test: log listeners, database, etc.
   *
   * @param activeRecords register test cases with active records
   * @param expiredRecords register test cases with expired records
   */
  protected abstract void initialize(ActiveRecords activeRecords, ExpiredRecords expiredRecords);

  protected abstract CrudRepository<?, UUID> getRepository();

  @BeforeEach
  final void setup() {
    auditLog = TestLog.audit();
    log.info("Preparing database");
    initialize(this::putActiveRecord, this::putExpiredRecord);
  }

  private void putActiveRecord(String testCase, UUID key) {
    if (expectedActiveRecords.containsKey(testCase)) {
      throw new IllegalArgumentException("Duplicate active test case: " + testCase);
    }
    expectedActiveRecords.put(testCase, key);
  }

  private void putExpiredRecord(String testCase, UUID key) {
    if (expectedExpiredRecords.containsKey(testCase)) {
      throw new IllegalArgumentException("Duplicate expired test case: " + testCase);
    }
    expectedExpiredRecords.put(testCase, key);
  }

  @Test
  final void test() {
    log.info("Running job");
    FhirStoragePurgerJob.run(fhirStoragePurger);
    log.info("Waiting for job to finish");
    verify();
  }

  protected final OffsetDateTime toOffsetDateTime(Instant instant) {
    return OffsetDateTime.ofInstant(instant, ZoneId.of("Europe/Berlin"));
  }

  protected void verify() {
    verifyExpiredRecords();
    verifyActiveRecords();
  }

  private void verifyExpiredRecords() {
    expectedExpiredRecords.keySet().stream().sorted().forEach(this::verifyExpired);
  }

  private void verifyExpired(String testCase) {
    final UUID key = expectedExpiredRecords.get(testCase);
    assertThat(getRepository().findById(key)).as(testCase).isEmpty();
    assertThat(auditLog.hasLogMessageContaining(key.toString())).as(testCase).isTrue();
  }

  private void verifyActiveRecords() {
    expectedActiveRecords.keySet().stream().sorted().forEach(this::verifyActive);
    assertThat(getRepository().count())
        .as("total number of bundles")
        .isEqualTo(expectedActiveRecords.size());
  }

  private void verifyActive(String testCase) {
    final UUID key = expectedActiveRecords.get(testCase);
    assertThat(getRepository().findById(key)).as(testCase).isNotEmpty();
    assertThat(auditLog.hasLogMessageContaining(key.toString())).as(testCase).isFalse();
  }

  public interface ActiveRecords extends BiConsumer<String, UUID> {}

  public interface ExpiredRecords extends BiConsumer<String, UUID> {}
}
