package de.gematik.demis.storage.reader;

/*-
 * #%L
 * fhir-storage-reader
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

import static de.gematik.demis.storage.reader.test.TestData.BINARY_FHIR_JSON;
import static de.gematik.demis.storage.reader.test.TestData.BUNDLE_DOCUMENT_FHIR_JSON;
import static de.gematik.demis.storage.reader.test.TestData.readAsString;
import static de.gematik.demis.storage.reader.test.TestUtil.getJsonParser;
import static org.assertj.core.api.Assertions.assertThat;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import de.gematik.demis.storage.reader.test.TestData;
import de.gematik.demis.storage.reader.test.TestWithPostgresContainer;
import java.net.URI;
import java.util.List;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriComponentsBuilder;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Slf4j
class ReaderSystemTest extends TestWithPostgresContainer {
  private static final String ENDPOINT_BUNDLE_SEARCH = "/notification-clearing-api/fhir/Bundle";
  private static final String ENDPOINT_BINARY_SEARCH = "/notification-clearing-api/fhir/Binary";
  private static final String ENDPOINT_BUNDLE_ID = "/notification-clearing-api/fhir/Bundle/{id}";
  private static final String ENDPOINT_BINARY_ID = "/notification-clearing-api/fhir/Binary/{id}";

  @Autowired private TestRestTemplate restTemplate;

  private static void log(ResponseEntity<String> response) {
    log.info("Response Status: {} - Body: {}", response.getStatusCode(), response.getBody());
  }

  private ResponseEntity<String> executeSearch(final String endpoint, final String query) {
    final HttpHeaders headers = TestData.createHttpHeaders();
    final String uri = UriComponentsBuilder.fromPath(endpoint).query(query).build().toUriString();
    log.info("call url {}", uri);

    final ResponseEntity<String> response =
        restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    log(response);

    return response;
  }

  @Nested
  @Sql(
      scripts = "classpath:/sql/findbyid-data.sql",
      executionPhase = ExecutionPhase.BEFORE_TEST_CLASS)
  @Sql(scripts = "classpath:/sql/clean.sql", executionPhase = ExecutionPhase.AFTER_TEST_CLASS)
  class FindByIdTest {

    static Stream<Arguments> findById_success() {
      return Stream.of(
          Arguments.of(
              ENDPOINT_BUNDLE_ID,
              "9e69e954-310a-4385-8365-ffc37bec8b4c",
              BUNDLE_DOCUMENT_FHIR_JSON),
          Arguments.of(
              ENDPOINT_BINARY_ID, "84d6cb09-924a-42c2-a786-0ec0d9271d4a", BINARY_FHIR_JSON));
    }

    static Stream<Arguments> findById_forbidden() {
      return Stream.of(
          Arguments.of(ENDPOINT_BUNDLE_ID, "11111111-1111-1111-1111-111111111111"),
          Arguments.of(ENDPOINT_BINARY_ID, "22222222-2222-2222-2222-222222222222"));
    }

    @ParameterizedTest
    @MethodSource
    void findById_success(final String endpoint, final String id, final String expectedResponse) {
      final ResponseEntity<String> response = executeFindBy(endpoint, id);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).isEqualToIgnoringWhitespace(readAsString(expectedResponse));
    }

    @ParameterizedTest
    @ValueSource(strings = {ENDPOINT_BUNDLE_ID, ENDPOINT_BINARY_ID})
    void findById_notFound(final String endpoint) {
      final ResponseEntity<String> response =
          executeFindBy(endpoint, "aaaaaaaa-1111-2222-3333-bbbbbbbbbbbb");
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @ParameterizedTest
    @MethodSource
    void findById_forbidden(final String endpoint, final String id) {
      // The JWT Token has no permission for the requested resource
      // binary - other responsible department
      // bundle - other profile
      final ResponseEntity<String> response = executeFindBy(endpoint, id);
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private ResponseEntity<String> executeFindBy(final String endpoint, final String id) {
      final HttpHeaders headers = TestData.createHttpHeaders();
      final URI uri = new DefaultUriBuilderFactory().expand(endpoint, id);
      final ResponseEntity<String> response =
          restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), String.class);

      log(response);
      return response;
    }
  }

  @Nested
  @Sql(
      scripts = "classpath:/sql/search-data.sql",
      executionPhase = ExecutionPhase.BEFORE_TEST_CLASS)
  @Sql(scripts = "classpath:/sql/clean.sql", executionPhase = ExecutionPhase.AFTER_TEST_CLASS)
  class SearchTest {

    static Stream<Arguments> bundleSearch() {
      final String ID_2025_01_17_14_59_LABORATORY_1_01_0_53 =
          "00000000-0000-0000-0000-000000000001";
      final String ID_2025_01_17_15_02_LABORATORY_1_20_4 = "00000000-0000-0000-0000-000000000002";
      final String ID_2025_01_17_14_55_DISEASE_1_01_0_53 = "00000000-0000-0000-0000-000000000003";
      final String ID_IDENTIFIER_9999 = "00000000-0000-0000-0000-000000000003";
      final String ID_RELATED_NOTIFICATION_AAAAAA = "00000000-0000-0000-0000-000000000002";

      return Stream.of(
          // all
          Arguments.of(
              "",
              List.of(
                  ID_2025_01_17_14_55_DISEASE_1_01_0_53,
                  ID_2025_01_17_14_59_LABORATORY_1_01_0_53,
                  ID_2025_01_17_15_02_LABORATORY_1_20_4)),
          // nothing
          Arguments.of("_lastUpdated=lt2025-01-17", List.of()),
          // all from 2025-01-17T14:58:48.147+01:00
          Arguments.of(
              "_lastUpdated=ge2025-01-17T14:58:48.147+01:00",
              List.of(
                  ID_2025_01_17_14_59_LABORATORY_1_01_0_53, ID_2025_01_17_15_02_LABORATORY_1_20_4)),
          // _sort=_lastUpdated is allowed but has no impact since we always sort by lastUpdated
          // ascending
          Arguments.of(
              "_lastUpdated=ge2025-01-17T14:58:48.147+01:00&_sort=_lastUpdated",
              List.of(
                  ID_2025_01_17_14_59_LABORATORY_1_01_0_53, ID_2025_01_17_15_02_LABORATORY_1_20_4)),
          // all from 2025-01-17 14:58 until 2025-01-17 15:02
          Arguments.of(
              "_lastUpdated=ge2025-01-17T14:58:48.147+01:00&_lastUpdated=lt2025-01-17T15:02:00.000+01:00",
              List.of(ID_2025_01_17_14_59_LABORATORY_1_01_0_53)),
          // parameter _summary=text has no impact
          Arguments.of(
              "_lastUpdated=ge2025-01-17T14:58:48.147+01:00&_lastUpdated=lt2025-01-17T15:02:00.000+01:00&_summary=text",
              List.of(ID_2025_01_17_14_59_LABORATORY_1_01_0_53)),
          // just profile disease
          Arguments.of(
              "_profile=https://demis.rki.de/fhir/StructureDefinition/NotificationBundleDisease",
              List.of(ID_2025_01_17_14_55_DISEASE_1_01_0_53)),
          // profile with time range
          Arguments.of(
              "_profile=https://demis.rki.de/fhir/StructureDefinition/NotificationBundleLaboratory&_lastUpdated=le2025-01-17T15:00:08.147+01:00",
              List.of(ID_2025_01_17_14_59_LABORATORY_1_01_0_53)),
          // paging
          Arguments.of(
              "_lastUpdated=ge2025-01-17&_count=2",
              List.of(
                  ID_2025_01_17_14_55_DISEASE_1_01_0_53, ID_2025_01_17_14_59_LABORATORY_1_01_0_53)),
          Arguments.of(
              "_lastUpdated=ge2025-01-17&_count=2&_offset=2",
              List.of(ID_2025_01_17_15_02_LABORATORY_1_20_4)),
          Arguments.of("_lastUpdated=ge2025-01-17&_count=2&_offset=4", List.of()),
          // notification bundle id (bundle->identifier->value)
          Arguments.of("identifier=9999", List.of(ID_IDENTIFIER_9999)),
          // responsible department
          Arguments.of(
              "_tag=https://demis.rki.de/fhir/CodeSystem/ResponsibleDepartment|1.01.0.53.",
              List.of(
                  ID_2025_01_17_14_55_DISEASE_1_01_0_53, ID_2025_01_17_14_59_LABORATORY_1_01_0_53)),
          // some tag
          Arguments.of(
              "_tag=https://demis.rki.de/fhir/CodeSystem/RelatedNotificationBundle|aaaaaaaa-0000-0000-0000-000000000001",
              List.of(ID_RELATED_NOTIFICATION_AAAAAA)));
    }

    static Stream<Arguments> binarySearch() {
      final String ID_2025_01_10 = "00000000-0000-0000-0001-000000000002";
      final String ID_2025_01_12 = "00000000-0000-0000-0001-000000000001";
      final String ID_NOTIFIER_1_11_0_11_01 = "00000000-0000-0000-0001-000000000002";

      return Stream.of(
          // all
          Arguments.of("", List.of(ID_2025_01_10, ID_2025_01_12)),
          // nothing
          Arguments.of("_lastUpdated=le2020-01-01", List.of()),
          // all from 2025-01-12
          Arguments.of("_lastUpdated=ge2025-01-12T00:00:00+01:00", List.of(ID_2025_01_12)),
          // _sort=_lastUpdated is allowed but has no impact since we always sort by lastUpdated
          // ascending
          Arguments.of(
              "_lastUpdated=ge2025-01-12T00:00:00+01:00&_sort=_lastUpdated",
              List.of(ID_2025_01_12)),
          // all from 2025-01-10 until 2025-01-11
          Arguments.of(
              "_lastUpdated=ge2025-01-10T00:00:00+01:00&_lastUpdated=lt2025-01-11T00:00:00+01:00",
              List.of(ID_2025_01_10)),
          // paging
          Arguments.of("_lastUpdated=ge2025-01-01&_count=1", List.of(ID_2025_01_10)),
          Arguments.of("_lastUpdated=ge2025-01-01&_count=1&_offset=1", List.of(ID_2025_01_12)),
          Arguments.of("_lastUpdated=ge2025-01-01&_count=1&_offset=2", List.of()),
          // own responsible department filter has no impact
          Arguments.of(
              "_lastUpdated=ge2025-01-01&_tag=https://demis.rki.de/fhir/CodeSystem/ResponsibleDepartment|1.01.0.53.",
              List.of(ID_2025_01_10, ID_2025_01_12)),
          // some tag
          Arguments.of(
              "_tag=https://demis.rki.de/fhir/CodeSystem/ResponsibleDepartmentNotifier|1.11.0.11.01.",
              List.of(ID_NOTIFIER_1_11_0_11_01)));
    }

    static Stream<Arguments> badRequest() {
      return Stream.of(
          Arguments.of(
              ENDPOINT_BUNDLE_SEARCH,
              "_sort=unsupported",
              "Only ascending sort by lastUpdated is allowed. Found: unsupported"),
          Arguments.of(
              ENDPOINT_BINARY_SEARCH,
              "_sort=unsupported",
              "Only ascending sort by lastUpdated is allowed. Found: unsupported"));
    }

    @ParameterizedTest
    @MethodSource
    void bundleSearch(final String query, final List<String> expectedSearchResultIdList) {
      executeSearchOkayTest(ENDPOINT_BUNDLE_SEARCH, query, expectedSearchResultIdList);
    }

    @ParameterizedTest
    @MethodSource
    void binarySearch(final String query, final List<String> expectedSearchResultIdList) {
      executeSearchOkayTest(ENDPOINT_BINARY_SEARCH, query, expectedSearchResultIdList);
    }

    private void executeSearchOkayTest(
        final String endpoint, final String query, final List<String> expectedSearchResultIdList) {
      final ResponseEntity<String> response = executeSearch(endpoint, query);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

      final Bundle searchResult = getJsonParser().parseResource(Bundle.class, response.getBody());
      final List<String> searchResultIds =
          searchResult.getEntry().stream()
              .map(Bundle.BundleEntryComponent::getResource)
              .map(Resource::getIdElement)
              .map(IdType::getIdPart)
              .toList();
      assertThat(searchResultIds).containsExactlyElementsOf(expectedSearchResultIdList);
    }

    @ParameterizedTest
    @ValueSource(strings = {ENDPOINT_BINARY_SEARCH, ENDPOINT_BUNDLE_SEARCH})
    void xmlFormat(final String endpoint) {
      final ResponseEntity<String> response = executeSearch(endpoint, "_format=xml");
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      final String body = response.getBody();
      final IParser xmlParser = FhirContext.forR4Cached().newXmlParser();
      final Bundle searchResult = xmlParser.parseResource(Bundle.class, body);
      assertThat(searchResult).isNotNull();
      assertThat(searchResult.getType()).isEqualTo(Bundle.BundleType.SEARCHSET);
    }

    @ParameterizedTest
    @MethodSource
    void badRequest(final String endpoint, final String query, final String expectedBody) {
      final ResponseEntity<String> response = executeSearch(endpoint, query);
      final String body = response.getBody();
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(body).contains(expectedBody);
    }
  }

  @Nested
  class ForbiddenTestWithoutDbAccess {

    @ParameterizedTest
    @ValueSource(strings = {ENDPOINT_BINARY_SEARCH, ENDPOINT_BUNDLE_SEARCH})
    void search_WithoutAuthorizationToken(final String endpoint) {
      final String uri = UriComponentsBuilder.fromPath(endpoint).toUriString();
      final HttpEntity<String> headers = new HttpEntity<>(null, new HttpHeaders());
      final ResponseEntity<String> response =
          restTemplate.exchange(uri, HttpMethod.GET, headers, String.class);
      log(response);
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void binarySearch_OtherHealthOfficeIsResponsible() {
      // header token for another user is set
      final String query =
          "_tag=https://demis.rki.de/fhir/CodeSystem/ResponsibleDepartment|2.01.0.53.";
      final ResponseEntity<String> response = executeSearch(ENDPOINT_BINARY_SEARCH, query);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
      assertThat(response.getBody()).contains("1.01.0.53. is not allowed to filter by 2.01.0.53.");
    }

    @Test
    void bundleSearch_ValidTokenButProfileIsNotAuthorized() {
      // token has roles pathogen-notification-fetcher, vaccine-injury-fetcher and
      // disease-notification-fetcher
      final HttpHeaders headers = TestData.createHttpHeaders();
      // profile requires role igs-notification-data-fetcher
      final String profile =
          "https://demis.rki.de/fhir/igs/StructureDefinition/NotificationBundleSequence";
      final String queryUri =
          UriComponentsBuilder.fromPath(ENDPOINT_BUNDLE_SEARCH)
              .query("_profile=" + profile)
              .toUriString();
      final HttpEntity<String> request = new HttpEntity<>(null, headers);
      final ResponseEntity<String> response =
          restTemplate.exchange(queryUri, HttpMethod.GET, request, String.class);
      log(response);
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
  }
}
