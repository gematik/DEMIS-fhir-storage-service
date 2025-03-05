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

import static de.gematik.demis.storage.writer.error.ErrorCode.VALIDATION_ERROR;
import static de.gematik.demis.storage.writer.test.TestData.BINARY_FHIR_JSON;
import static de.gematik.demis.storage.writer.test.TestData.BUNDLE_DOCUMENT_FHIR_JSON;
import static de.gematik.demis.storage.writer.test.TestData.jsonToResource;
import static de.gematik.demis.storage.writer.test.TestData.readResourceAsString;
import static de.gematik.demis.storage.writer.test.TestData.resourceToJsonString;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.demis.service.base.error.rest.api.ErrorDTO;
import de.gematik.demis.storage.common.entity.AbstractResourceEntity;
import de.gematik.demis.storage.common.entity.BinaryEntity;
import de.gematik.demis.storage.common.entity.BundleEntity;
import de.gematik.demis.storage.common.entity.HapiBundleEntity;
import de.gematik.demis.storage.writer.test.TestData;
import de.gematik.demis.storage.writer.test.TestWithPostgresContainer;
import de.gematik.demis.storage.writer.test.TransactionBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureObservability
@ActiveProfiles("test")
@Slf4j
class FhirWriterIntegrationSystemTest extends TestWithPostgresContainer {

  private static final String ENDPOINT = "/notification-clearing-api/fhir/";

  private static final String TRACE_ID = "091f30ab559170b6c4db82ca25ef6dab";
  private static final String VALID_BINARY_JSON = readResourceAsString(BINARY_FHIR_JSON);
  private static final String VALID_BUNDLE_JSON = readResourceAsString(BUNDLE_DOCUMENT_FHIR_JSON);

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private EntityManager entityManager;

  private static Stream<Arguments> storeResourcesSuccessfully() {
    final Bundle transactionBundle =
        new TransactionBuilder()
            .addResource(jsonToResource(Bundle.class, VALID_BUNDLE_JSON))
            .addResource(jsonToResource(Binary.class, VALID_BINARY_JSON))
            .build();

    return Stream.of(
        Arguments.of(Named.of("Binary", VALID_BINARY_JSON), List.of(TestData.createBinaryEntity())),
        Arguments.of(Named.of("Bundle", VALID_BUNDLE_JSON), List.of(TestData.createBundleEntity())),
        Arguments.of(
            Named.of("Transaction", resourceToJsonString(transactionBundle)),
            List.of(TestData.createBundleEntity(), TestData.createBinaryEntity())));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource
  void storeResourcesSuccessfully(
      final String inputFhirJson, final List<AbstractResourceEntity> expectedEntities)
      throws JsonProcessingException {

    final long rowCountBefore = countResourceRowsInDatabase();
    final String body = executeCall(inputFhirJson, HttpStatus.OK);
    final long rowCountAfter = countResourceRowsInDatabase();

    final String[] idStrings = objectMapper.readValue(body, String[].class);
    assertThat(idStrings).hasSize(expectedEntities.size());

    final SoftAssertions assertions = new SoftAssertions();
    for (int i = 0; i < expectedEntities.size(); i++) {
      final var dbResult = loadResourceFromDb(idStrings[i]);

      assertions
          .assertThat(dbResult)
          .as("resource %s: %s", i, idStrings[i])
          .isNotNull()
          .returns(TRACE_ID, AbstractResourceEntity::getSourceId)
          .usingRecursiveComparison()
          .ignoringFields("id", "sourceId", "lastUpdated")
          .isEqualTo(expectedEntities.get(i));

      assertions
          .assertThat(dbResult.getLastUpdated())
          .as("lastUpdated set by database")
          .isNotNull();

      assertHapiSync(assertions, dbResult);
    }

    assertions.assertAll();

    assertThat(rowCountAfter - rowCountBefore)
        .as("created rows in database")
        .isEqualTo(expectedEntities.size());
  }

  private void assertHapiSync(
      final SoftAssertions assertions, final AbstractResourceEntity dbResult) {
    final var hapiBundleEntity = entityManager.find(HapiBundleEntity.class, dbResult.getId());
    if (dbResult instanceof BundleEntity) {
      assertions.assertThat(hapiBundleEntity).as("bundle -> hapi synced entity").isNotNull();
    } else {
      assertions.assertThat(hapiBundleEntity).as("binary -> no hapi entity").isNull();
    }
  }

  private AbstractResourceEntity loadResourceFromDb(final String resourceId) {
    final String[] idParts = resourceId.split("/");
    final Class<? extends AbstractResourceEntity> clazz =
        switch (idParts[0]) {
          case "Bundle" -> BundleEntity.class;
          case "Binary" -> BinaryEntity.class;
          default -> Assertions.fail("invalid resourceId %s", resourceId);
        };
    final UUID id = UUID.fromString(idParts[1]);
    return entityManager.find(clazz, id);
  }

  private long countResourceRowsInDatabase() {
    Query query = entityManager.createQuery("SELECT COUNT(*) FROM binaries");
    final long binariesCount = (long) query.getSingleResult();
    query = entityManager.createQuery("SELECT COUNT(*) FROM bundles ");
    final long bundlesCount = (long) query.getSingleResult();
    return binariesCount + bundlesCount;
  }

  @Test
  void transactionRollback() {
    final Binary invalidBinary = jsonToResource(Binary.class, VALID_BINARY_JSON);
    invalidBinary.setContentType(
        "This mandatory field is set thus pre validation check is passed. But this string is much to long for the database. djkfgfdkgjfdlkgjlfdkgjlfdkjglkdfjglsfdjglkfdjgksfdjgfdkjgklfdjglfdjglfdgjlfdkjglfdkjglfdkjglfdgjlfdjglfdsgjlsfgjffdlsögjfdlgjdfdöklsgjlfdösätrzhpktrn,fvg-.nhmktphkRTÄh,bfgh,FGhlfgöhfghlgöhlgöhlfghlghlfäghlghjkgpjklgj,göhlfg#höfgühlfgh,fg-h,fkhtrlhöäf,n-fg,nhfghfglhglhäöglhäfglhgöhläghlghlg");

    final Bundle transactionBundle =
        new TransactionBuilder()
            .addResource(jsonToResource(Bundle.class, VALID_BUNDLE_JSON))
            .addResource(invalidBinary)
            .build();

    final String input = resourceToJsonString(transactionBundle);

    final long rowCountBefore = countResourceRowsInDatabase();
    executeCall(input, HttpStatus.INTERNAL_SERVER_ERROR);
    final long rowCountAfter = countResourceRowsInDatabase();

    // ensure that no resource entities were inserted into the database. The insert of the valid
    // bundle must be roll backed.
    assertThat(rowCountAfter - rowCountBefore).isZero();
  }

  @Test
  void validationError() throws JsonProcessingException {
    final Binary binary = jsonToResource(Binary.class, VALID_BINARY_JSON);
    binary.setContentType(null); // this is an input error since the field is mandatory
    final String input = resourceToJsonString(binary);

    final String body = executeCall(input, HttpStatus.UNPROCESSABLE_ENTITY);

    final ErrorDTO errorDTO = objectMapper.readValue(body, ErrorDTO.class);
    assertThat(errorDTO)
        .returns(VALIDATION_ERROR.getCode(), ErrorDTO::errorCode)
        .returns("{[0]=[element 'ContentType' is null but mandatory]}", ErrorDTO::detail);
  }

  private String executeCall(final String input, final HttpStatus expectedStatus) {
    final var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setAcceptLanguageAsLocales(List.of(Locale.GERMAN));
    headers.set("X-B3-TraceId", TRACE_ID);
    final HttpEntity<String> request = new HttpEntity<>(input, headers);

    final ResponseEntity<String> response =
        restTemplate.postForEntity(ENDPOINT, request, String.class);
    log.info("Response {}", response);

    final String body = response.getBody();

    assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
    assertThat(body).isNotBlank();

    log.info("Response Body: {}", body);

    return body;
  }
}
