package de.gematik.demis.storage.writer.api;

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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.gematik.demis.service.base.error.ServiceException;
import de.gematik.demis.service.base.error.rest.ErrorHandlerConfiguration;
import de.gematik.demis.storage.writer.common.WriteService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WriteController.class)
@Import(ErrorHandlerConfiguration.class)
class WriteControllerIntegrationTest {
  private static final String ENDPOINT = "/notification-clearing-api/fhir/";

  private static final String FHIR_INPUT = "does not matter";
  @MockBean WriteService writeService;
  @Autowired MockMvc mockMvc;

  @Test
  void success() throws Exception {
    final var ids = List.of("Bundle/10", "Binary/5");
    when(writeService.store(FHIR_INPUT)).thenReturn(ids);

    mockMvc
        .perform(post(ENDPOINT).contentType("application/fhir+json").content(FHIR_INPUT))
        .andExpectAll(
            status().isOk(),
            content().contentTypeCompatibleWith("application/json"),
            jsonPath("$", hasSize(ids.size())),
            jsonPath("$[0]", is(ids.get(0))),
            jsonPath("$[1]", is(ids.get(1))));
  }

  @ParameterizedTest
  @ValueSource(strings = {"text/json", "text/json+fhir", "text/xml+fhir", "unknown/json"})
  void unsupportedMediaType(final String contentType) throws Exception {
    mockMvc
        .perform(
            post(ENDPOINT)
                .contentType(contentType)
                .content(FHIR_INPUT)
                .header(HttpHeaders.ACCEPT, "*/*"))
        .andExpectAll(
            status().isUnsupportedMediaType(),
            content().contentTypeCompatibleWith("application/json"));

    verifyNoInteractions(writeService);
  }

  @Test
  void emptyBody() throws Exception {
    mockMvc
        .perform(post(ENDPOINT).contentType(APPLICATION_JSON_VALUE))
        .andExpectAll(
            status().is(400), jsonPath("$.status").value(400), jsonPath("$.path").value(ENDPOINT));

    verifyNoInteractions(writeService);
  }

  @Test
  void processingBusinessException() throws Exception {
    when(writeService.store(FHIR_INPUT))
        .thenThrow(new ServiceException(HttpStatus.BAD_REQUEST, "just for test", null));

    mockMvc
        .perform(post(ENDPOINT).contentType("application/json").content(FHIR_INPUT))
        .andExpectAll(
            status().is(400), jsonPath("$.status").value(400), jsonPath("$.path").value(ENDPOINT));
  }
}
