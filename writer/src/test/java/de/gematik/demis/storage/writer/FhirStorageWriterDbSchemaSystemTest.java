package de.gematik.demis.storage.writer;

/*-
 * #%L
 * fhir-storage-writer
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

import static de.gematik.demis.storage.writer.test.TestData.BUNDLE_DOCUMENT_FHIR_JSON;
import static de.gematik.demis.storage.writer.test.TestData.createBinaryEntity;
import static de.gematik.demis.storage.writer.test.TestData.createBundleEntity;
import static de.gematik.demis.storage.writer.test.TestData.readResourceAsString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.boot.test.context.SpringBootTest.UseMainMethod.ALWAYS;

import de.gematik.demis.storage.common.entity.AbstractResourceEntity;
import de.gematik.demis.storage.common.entity.HapiBundleEntity;
import de.gematik.demis.storage.common.entity.HapiSyncedStatus;
import de.gematik.demis.storage.writer.test.TestWithPostgresContainer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(useMainMethod = ALWAYS)
@ActiveProfiles("test")
@Transactional
class FhirStorageWriterDbSchemaSystemTest extends TestWithPostgresContainer {

  @Autowired EntityManager entityManager;

  private static Stream<AbstractResourceEntity> databaseTableIsCreatedAndMatchesEntity() {
    return Stream.of(createBinaryEntity(), createBundleEntity());
  }

  @ParameterizedTest
  @MethodSource
  void databaseTableIsCreatedAndMatchesEntity(final AbstractResourceEntity entity) {
    entityManager.persist(entity);
    final var generatedId = entity.getId();
    assertThat(generatedId).isNotNull();
    entityManager.flush();
    entityManager.clear();

    final AbstractResourceEntity dbResult = entityManager.find(entity.getClass(), generatedId);
    assertThat(dbResult)
        .isNotNull()
        .usingRecursiveComparison()
        .ignoringFields("lastUpdated")
        .isEqualTo(entity);

    assertThat(dbResult.getLastUpdated()).isNotNull();
  }

  @Test
  void bundleContentIsCompressed() {
    final var entity = createBundleEntity();
    entityManager.persist(entity);
    entityManager.flush();

    final Query query =
        entityManager.createNativeQuery("select content from bundles where id = ?1");
    query.setParameter(1, entity.getId());
    final byte[] nativeContentInDb = (byte[]) query.getSingleResult();

    final byte[] uncompressedContent = entity.getContent().getBytes(StandardCharsets.UTF_8);
    Assertions.assertThat(nativeContentInDb)
        .isNotNull()
        .isNotEqualTo(uncompressedContent)
        .hasSizeLessThan(uncompressedContent.length);
  }

  @Test
  void binaryDataIsCompressed() {
    final var entity = createBinaryEntity();
    // set a longer content otherwise compression does not work
    entity.setData(
        readResourceAsString(BUNDLE_DOCUMENT_FHIR_JSON).getBytes(StandardCharsets.UTF_8));
    entityManager.persist(entity);
    entityManager.flush();

    final Query query = entityManager.createNativeQuery("select data from binaries where id = ?1");
    query.setParameter(1, entity.getId());
    final byte[] nativeContentInDb = (byte[]) query.getSingleResult();

    final byte[] uncompressedContent = entity.getData();
    Assertions.assertThat(nativeContentInDb)
        .isNotNull()
        .isNotEqualTo(uncompressedContent)
        .hasSizeLessThan(uncompressedContent.length);
  }

  @Test
  void hapiBundlesTableIsCreatedAndMatchesEntity() {
    final var bundleEntity = createBundleEntity();
    entityManager.persist(bundleEntity);

    final HapiBundleEntity hapiBundleEntity = new HapiBundleEntity();
    final UUID bundleId = bundleEntity.getId();
    hapiBundleEntity.setBundleId(bundleId);

    entityManager.persist(hapiBundleEntity);
    entityManager.flush();
    entityManager.clear();

    final HapiBundleEntity dbResult = entityManager.find(HapiBundleEntity.class, bundleId);
    assertThat(dbResult).isNotNull();
    assertThat(dbResult.getStatus()).isEqualTo(HapiSyncedStatus.NEW);
    assertThat(dbResult.getModifiedAt())
        .isCloseTo(LocalDateTime.now(), within(500, ChronoUnit.MILLIS));
  }
}
