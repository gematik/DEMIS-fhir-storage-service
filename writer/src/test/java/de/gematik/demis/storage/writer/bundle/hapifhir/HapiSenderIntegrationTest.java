package de.gematik.demis.storage.writer.bundle.hapifhir;

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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.http.RequestMethod.POST;
import static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.newRequestPattern;
import static de.gematik.demis.storage.writer.bundle.hapifhir.HapiSender.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.ContentTypeHeader;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import de.gematik.demis.storage.common.entity.HapiBundleEntity;
import de.gematik.demis.storage.common.entity.HapiSyncedStatus;
import de.gematik.demis.storage.writer.config.FhirConfiguration;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import java.util.List;
import java.util.UUID;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;

@SpringBootTest(
    classes = {HapiSender.class, FhirConfiguration.class},
    properties = "fss.hapi.url=http://localhost:${wiremock.server.port}/fhir")
@AutoConfigureWireMock(port = 0)
class HapiSenderIntegrationTest {
  private static final String ENDPOINT = "/fhir/Bundle";
  public static final String TEST_TRACE_ID = "testTraceId";
  public static final String TEST_PARENT_ID = "testParentId";
  public static final String TEST_SPAN_ID = "testSpanId";

  @MockBean HapiEventRepository repositoryMock;
  @MockBean Tracer tracer;
  @Autowired HapiSender underTest;

  private Bundle bundle;
  private UUID bundleId;
  private HapiBundleEntity hapiBundleEntity;

  private static void setupRemoteService(final ResponseDefinitionBuilder responseDefBuilder) {
    stubFor(post(ENDPOINT).willReturn(responseDefBuilder));
  }

  @BeforeEach
  void setupInput() {
    bundle = new Bundle().setType(BundleType.DOCUMENT);
    bundleId = UUID.randomUUID();
    hapiBundleEntity = new HapiBundleEntity().setBundleId(bundleId);
  }

  @Test
  void success() {
    final String hapiId = "6";
    final String location = "http://localhost:9999/fhir/Bundle/" + hapiId + "/_history/1";
    setupRemoteService(ok().withHeader("Location", location));

    final var expectedHapiEntity =
        new HapiBundleEntity()
            .setBundleId(bundleId)
            .setHapiId(hapiId)
            .setStatus(HapiSyncedStatus.SYNCED)
            .setResponseCode(200);

    Span span = mock(Span.class);
    when(tracer.currentSpan()).thenReturn(span);
    when(span.context()).thenReturn(testTraceContext());

    // When
    underTest.sendToHapiInTx(hapiBundleEntity, bundle);

    // then
    verify(repositoryMock).save(hapiBundleEntity);
    verify(postRequestedFor(urlEqualTo(ENDPOINT)));

    assertThat(hapiBundleEntity).usingRecursiveComparison().isEqualTo(expectedHapiEntity);

    List<LoggedRequest> loggedRequests = findAll(newRequestPattern(POST, urlEqualTo(ENDPOINT)));
    assertThat(loggedRequests).hasSize(1);
    LoggedRequest request = loggedRequests.getFirst();
    assertThat(request.getBodyAsString())
        .isEqualTo("{\"resourceType\":\"Bundle\",\"type\":\"document\"}");
    assertThat(request.contentTypeHeader())
        .returns("application/fhir+json", ContentTypeHeader::mimeTypePart)
        .returns(UTF_8, ContentTypeHeader::charset);
    assertThat(request.getHeader("Prefer")).isEqualTo("return=minimal");

    // assert B3 tracing header
    assertThat(request.getHeader(B3_HEADER_TRACE_ID)).isEqualTo(TEST_TRACE_ID);
    assertThat(request.getHeader(B3_HEADER_SPAN_ID)).isEqualTo(TEST_SPAN_ID);
    assertThat(request.getHeader(B3_HEADER_PARENT_ID)).isEqualTo(TEST_PARENT_ID);
    assertThat(request.getHeader(B3_HEADER_SAMPLED)).isEqualTo("1");
  }

  @Test
  void serverError() {
    setupRemoteService(WireMock.serverError());

    underTest.sendToHapiInTx(hapiBundleEntity, bundle);

    final var expectedHapiEntity =
        new HapiBundleEntity()
            .setBundleId(bundleId)
            .setStatus(HapiSyncedStatus.ERROR)
            .setResponseCode(500)
            .setError("HTTP 500 Server Error");

    assertThat(hapiBundleEntity).usingRecursiveComparison().isEqualTo(expectedHapiEntity);
    verify(repositoryMock).save(hapiBundleEntity);
  }

  private TraceContext testTraceContext() {
    return new TraceContext() {
      @Override
      public @NotNull String traceId() {
        return TEST_TRACE_ID;
      }

      @Override
      public @NotNull String spanId() {
        return TEST_SPAN_ID;
      }

      @Override
      public String parentId() {
        return TEST_PARENT_ID;
      }

      @Override
      public @NotNull Boolean sampled() {
        return true;
      }
    };
  }
}
