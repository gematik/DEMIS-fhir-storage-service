package de.gematik.demis.storage.reader.api;

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

import static de.gematik.demis.fhirparserlibrary.MessageType.JSON;
import static de.gematik.demis.fhirparserlibrary.MessageType.XML;
import static org.hl7.fhir.r4.model.Bundle.BundleType.SEARCHSET;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.gematik.demis.fhirparserlibrary.MessageType;
import de.gematik.demis.service.base.error.ServiceException;
import de.gematik.demis.storage.common.entity.AbstractResourceEntity;
import de.gematik.demis.storage.reader.common.ReadService;
import de.gematik.demis.storage.reader.common.fhir.FhirConverter;
import de.gematik.demis.storage.reader.common.security.AuthorizationService;
import de.gematik.demis.storage.reader.common.security.Caller;
import de.gematik.demis.storage.reader.config.FhirConfiguration;
import de.gematik.demis.storage.reader.error.ErrorCode;
import de.gematik.demis.storage.reader.test.TestData;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.hamcrest.Matchers;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.util.MultiValueMapAdapter;

@Import({FhirConverter.class, FhirConfiguration.class})
@RequiredArgsConstructor
abstract class CommonControllerTest<E extends AbstractResourceEntity, R extends Resource> {

  private static final String FORMAT_PARAM = "_format";

  private static final String EXPECTED_SEARCH_RESULT_JSON =
      """
{"resourceType":"Bundle","type":"searchset","total":0}
""";
  private static final String EXPECTED_SEARCH_RESULT_XML =
      """
<Bundle xmlns="http://hl7.org/fhir"><type value="searchset"></type><total value="0"></total></Bundle>
""";

  private static final String EXPECTED_ERROR_JSON =
      """
{"resourceType":"OperationOutcome","issue":[{"severity":"error","code":"processing","details":{"coding":[{"code":"%CODE%"}]},"diagnostics":"%MESSAGE%","location":
""";
  private static final String EXPECTED_ERROR_XML =
      """
<OperationOutcome xmlns="http://hl7.org/fhir"><issue><severity value="error"></severity><code value="processing"></code><details><coding><code value="%CODE%"></code></coding></details><diagnostics value="%MESSAGE%"></diagnostics><location value=
""";

  private final String ENDPOINT_SEARCH;
  private final String ENDPOINT_FIND_BY_ID;

  @Autowired MockMvc mockMvc;
  @MockitoBean AuthorizationService authorizationServiceMock;

  public static Stream<Arguments> messageTypePermutation() {
    return Stream.of(
        // correct accept header - no query param
        Arguments.of(new ResponseFormat("application/json", null, JSON)),
        Arguments.of(new ResponseFormat("application/xml", null, XML)),
        // query param
        Arguments.of(new ResponseFormat("*", "json", JSON)),
        Arguments.of(new ResponseFormat("*", "xml", XML)),
        // query parameter prio is higher
        Arguments.of(new ResponseFormat("application/xml", "json", JSON)),
        Arguments.of(new ResponseFormat("application/json", "xml", XML)),
        // nothing set - default is json
        Arguments.of(new ResponseFormat("*", null, JSON)),
        // we accept everything. if not supported, JSON is taken, no 406
        Arguments.of(new ResponseFormat("text/plain", null, JSON)));
  }

  private static MultiValueMapAdapter<String, String> createMutableQueryParameterMap() {
    final Map<String, List<String>> map = new HashMap<>();
    map.put("_tag", List.of("my-system|my-code", "my-system-2|my-code-2"));
    return new MultiValueMapAdapter<>(map);
  }

  private static @NotNull String getExpectedContentType(final MessageType expectedMessageType) {
    return expectedMessageType == JSON ? "application/fhir+json" : "application/fhir+xml";
  }

  protected abstract ReadService<E, R, ?> getReadService();

  protected final void executeAndAssertSearchSuccess(final ResponseFormat responseFormat)
      throws Exception {
    final HttpHeaders headers = TestData.createHttpHeaders();
    headers.add(HttpHeaders.ACCEPT, responseFormat.acceptHeader());

    final Bundle searchSetBundle = new Bundle();
    searchSetBundle.setType(SEARCHSET);
    searchSetBundle.setTotal(0);

    final Caller caller = new Caller("1.53", () -> Set.of("myProfile"));
    when(authorizationServiceMock.getCaller(headers)).thenReturn(caller);
    final var queryParams = createMutableQueryParameterMap();
    when(getReadService().search(caller, queryParams)).thenReturn(searchSetBundle);

    if (responseFormat.queryParam() != null) {
      queryParams.add(FORMAT_PARAM, responseFormat.queryParam());
    }

    final String expectedContent =
        responseFormat.expectedMessageType() == JSON
            ? EXPECTED_SEARCH_RESULT_JSON
            : EXPECTED_SEARCH_RESULT_XML;
    final String expectedContentType = getExpectedContentType(responseFormat.expectedMessageType());

    mockMvc
        .perform(get(ENDPOINT_SEARCH).headers(headers).queryParams(queryParams))
        .andExpectAll(
            status().isOk(),
            content().contentTypeCompatibleWith(expectedContentType),
            content().encoding(StandardCharsets.UTF_8),
            content().string(Matchers.equalToCompressingWhiteSpace(expectedContent)));
  }

  protected final void executeAndAssertFindByIdSuccess(
      final ResponseFormat responseFormat,
      final R returnedResource,
      final String expectedJson,
      final String expectedXml)
      throws Exception {
    final UUID uuid = UUID.randomUUID();
    final HttpHeaders headers = TestData.createHttpHeaders();
    headers.add(HttpHeaders.ACCEPT, responseFormat.acceptHeader());
    final Caller caller = new Caller("1.53", () -> Set.of("myProfile"));

    when(authorizationServiceMock.getCaller(headers)).thenReturn(caller);
    when(getReadService().findById(any(), any())).thenReturn(returnedResource);

    final MockHttpServletRequestBuilder request = get(ENDPOINT_FIND_BY_ID, uuid.toString());

    if (responseFormat.queryParam != null) {
      request.queryParam(FORMAT_PARAM, responseFormat.queryParam);
    }

    final String expectedContent =
        responseFormat.expectedMessageType() == JSON ? expectedJson : expectedXml;

    final String expectedContentType = getExpectedContentType(responseFormat.expectedMessageType());

    mockMvc
        .perform(request.headers(headers))
        .andExpectAll(
            status().isOk(),
            content().contentTypeCompatibleWith(expectedContentType),
            content().encoding(StandardCharsets.UTF_8),
            content().string(Matchers.equalToCompressingWhiteSpace(expectedContent)));

    verify(getReadService()).findById(caller, uuid);
  }

  protected final void executeAndAssertFindByIdError(final ResponseFormat responseFormat)
      throws Exception {
    final ServiceException exc = ErrorCode.RESOURCE_NOT_FOUND.exception("test");
    when(getReadService().findById(any(), any())).thenThrow(exc);

    final MockHttpServletRequestBuilder request =
        get(ENDPOINT_FIND_BY_ID, UUID.randomUUID().toString());

    executeAndAssertErrorRequest(request, responseFormat, exc);
  }

  protected final void executeAndAssertSearchError(final ResponseFormat responseFormat)
      throws Exception {
    final ServiceException exc = ErrorCode.INVALID_FILTER.exception("test");
    when(getReadService().search(any(), any())).thenThrow(exc);

    executeAndAssertErrorRequest(get(ENDPOINT_SEARCH), responseFormat, exc);
  }

  protected final void executeAndAssertSearchAuthorizationError(final ResponseFormat responseFormat)
      throws Exception {
    final ServiceException exc = ErrorCode.FORBIDDEN.exception("test");
    when(getReadService().search(any(), any())).thenThrow(exc);

    executeAndAssertErrorRequest(get(ENDPOINT_SEARCH), responseFormat, exc);
  }

  private void executeAndAssertErrorRequest(
      final MockHttpServletRequestBuilder request,
      final ResponseFormat responseFormat,
      final ServiceException exc)
      throws Exception {
    request.accept(responseFormat.acceptHeader());
    if (responseFormat.queryParam() != null) {
      request.queryParam(FORMAT_PARAM, responseFormat.queryParam());
    }

    String expectedContent =
        responseFormat.expectedMessageType() == JSON ? EXPECTED_ERROR_JSON : EXPECTED_ERROR_XML;
    expectedContent =
        expectedContent
            .trim()
            .replace("%CODE%", exc.getErrorCode())
            .replace("%MESSAGE%", exc.getMessage());

    final String expectedContentType = getExpectedContentType(responseFormat.expectedMessageType());

    mockMvc
        .perform(request)
        .andExpectAll(
            status().is(exc.getResponseStatus().value()),
            content().contentTypeCompatibleWith(expectedContentType),
            content().encoding(StandardCharsets.UTF_8),
            content().string(Matchers.startsWith(expectedContent)));
  }

  protected record ResponseFormat(
      String acceptHeader, String queryParam, MessageType expectedMessageType) {}
}
