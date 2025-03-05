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

import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.demis.storage.common.entity.BinaryEntity;
import de.gematik.demis.storage.common.entity.BundleEntity;
import de.gematik.demis.storage.purger.FhirStoragePurger;
import de.gematik.demis.storage.purger.FhirStoragePurgerJob;
import de.gematik.demis.storage.purger.test.TestLog;
import de.gematik.demis.storage.purger.test.TestWithPostgresContainer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.TestPropertySources;
import org.springframework.test.context.jdbc.Sql;

/**
 * Integration test for purging FHIR bundles from HAPI FHIR server.
 *
 * <h2>Wiremock HAPI FHIR server</h2>
 *
 * Configuration of Wiremock HAPI FHIR server can be found in file <code>
 * src/test/resources/mappings/hapi-test-01.json</code>. This is Wiremock configuration convention.
 */
@SpringBootTest(properties = {"fss.purger.attempts.limit=3"})
@ActiveProfiles("test-hapi")
@AutoConfigureWireMock(port = 0)
@EntityScan(
    basePackageClasses = {BinaryEntity.class, BundleEntity.class, PurgerHapiBundleEntity.class})
@Slf4j
@Sql(
    scripts = "classpath:/external/hapi/hapi-test-01.sql",
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@TestPropertySources({
  @TestPropertySource(properties = "fss.hapi.url=http://localhost:${wiremock.server.port}/fhir"),
})
class HapiPurgeIntegrationTest extends TestWithPostgresContainer {

  @Autowired private FhirStoragePurger fhirStoragePurger;
  @Autowired private HapiPurgeIntegrationTestRepository hapiPurgeIntegrationTestRepository;

  private TestLog auditLog;
  private TestLog hapiPurgeLog;

  @BeforeEach
  final void setup() {
    auditLog = TestLog.audit();
    hapiPurgeLog = HapiPurgeLog.getLog();
  }

  @Test
  void givenHapiTest01WhenRunThenPurge() {

    // given
    assertThat(hapiPurgeIntegrationTestRepository.count()).isEqualTo(7);

    // when
    FhirStoragePurgerJob.run(fhirStoragePurger);

    // then
    assertThat(hapiPurgeIntegrationTestRepository.count()).isEqualTo(6);
    verifyAuditLog();
    verifyPurgeLog();
  }

  private void verifyAuditLog() {
    assertThat(auditLog.hasLogMessageContaining("00000000-0000-0000-0000-000000000001")).isFalse();
    assertThat(auditLog.hasLogMessageContaining("00000000-0000-0000-0000-000000000002")).isFalse();
    assertThat(auditLog.hasLogMessageContaining("00000000-0000-0000-0000-000000000003")).isFalse();
    assertThat(auditLog.hasLogMessageContaining("00000000-0000-0000-0000-000000000004")).isFalse();
    assertThat(auditLog.hasLogMessageContaining("00000000-0000-0000-0000-000000000005")).isFalse();
    assertThat(auditLog.hasLogMessageContaining("00000000-0000-0000-0000-000000000006")).isFalse();
    assertThat(auditLog.hasLogMessageContaining("DeletedIds: 00000000-0000-0000-0000-000000000007"))
        .isTrue();
  }

  private void verifyPurgeLog() {
    assertThat(
            hapiPurgeLog.hasLogMessageContaining(
                "FATAL - Failed to delete bundles from HAPI FHIR server. Maximum number of attempts reached! Maximum: 3 Bundles: [00000000-0000-0000-0000-000000000006]"))
        .isTrue();
  }
}
