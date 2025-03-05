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

import de.gematik.demis.storage.common.entity.BinaryEntity;
import de.gematik.demis.storage.reader.binary.BinaryReadController;
import de.gematik.demis.storage.reader.binary.BinaryReadService;
import de.gematik.demis.storage.reader.common.ReadService;
import de.gematik.demis.storage.reader.common.search.Filter;
import org.hl7.fhir.r4.model.Binary;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(BinaryReadController.class)
class BinaryReadControllerIntegrationTest extends CommonControllerTest<BinaryEntity, Binary> {

  private static final String BASE_PATH = "/notification-clearing-api/fhir/Binary";
  private static final String ENDPOINT_FIND_BY_ID = BASE_PATH + "/{id}";
  private static final String ENDPOINT_SEARCH = BASE_PATH;

  private static final String EXPECTED_ID_RESULT_JSON =
      """
{"resourceType":"Binary","contentType":"application/cms"}
""";
  private static final String EXPECTED_ID_RESULT_XML =
      """
<Binary xmlns="http://hl7.org/fhir"><contentType value="application/cms"></contentType></Binary>
""";

  @MockitoBean BinaryReadService readService;

  public BinaryReadControllerIntegrationTest() {
    super(ENDPOINT_SEARCH, ENDPOINT_FIND_BY_ID);
  }

  @ParameterizedTest
  @MethodSource("messageTypePermutation")
  @SuppressWarnings("java:S2699") // false positive. there are many assertions!
  void search_success(final ResponseFormat responseFormat) throws Exception {
    executeAndAssertSearchSuccess(responseFormat);
  }

  @ParameterizedTest
  @MethodSource("messageTypePermutation")
  @SuppressWarnings("java:S2699") // false positive. there are many assertions!
  void findById_success(final ResponseFormat responseFormat) throws Exception {
    final var binary = new Binary();
    binary.setContentType("application/cms");
    executeAndAssertFindByIdSuccess(
        responseFormat, binary, EXPECTED_ID_RESULT_JSON, EXPECTED_ID_RESULT_XML);
  }

  @ParameterizedTest
  @MethodSource("messageTypePermutation")
  @SuppressWarnings("java:S2699") // false positive. there are many assertions!
  void findById_error(final ResponseFormat responseFormat) throws Exception {
    executeAndAssertFindByIdError(responseFormat);
  }

  @ParameterizedTest
  @MethodSource("messageTypePermutation")
  @SuppressWarnings("java:S2699") // false positive. there are many assertions!
  void search_error(final ResponseFormat responseFormat) throws Exception {
    executeAndAssertSearchError(responseFormat);
  }

  @Override
  protected ReadService<BinaryEntity, Binary, Filter> getReadService() {
    return readService;
  }
}
