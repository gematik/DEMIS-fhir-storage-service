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

import static ca.uhn.fhir.rest.api.PreferReturnEnum.MINIMAL;
import static de.gematik.demis.storage.writer.util.ExceptionChecker.hasCause;
import static jakarta.transaction.Transactional.TxType.MANDATORY;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.gclient.ICreateTyped;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import de.gematik.demis.storage.common.entity.HapiBundleEntity;
import de.gematik.demis.storage.common.entity.HapiSyncedStatus;
import de.gematik.demis.storage.writer.config.ConditionalOnHapiSync;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import jakarta.transaction.Transactional;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@ConditionalOnHapiSync
@Service
@RequiredArgsConstructor
@Slf4j
class HapiSender {

  public static final String B3_HEADER_TRACE_ID = "X-B3-TraceId";
  public static final String B3_HEADER_PARENT_ID = "X-B3-ParentSpanId";
  public static final String B3_HEADER_SPAN_ID = "X-B3-SpanId";
  public static final String B3_HEADER_SAMPLED = "X-B3-Sampled";
  public static final String SAMPLED_ACCEPT = "1";
  public static final String SAMPLED_DENY = "0";

  private final HapiEventRepository repository;
  private final FhirContext fhirContext;
  private final Tracer tracer;

  @Value("${fss.hapi.url}")
  private String url;

  @Value("${fss.hapi.log-requests:false}")
  private boolean logRequests;

  @Transactional(MANDATORY)
  public void sendToHapiInTx(final HapiBundleEntity hapiBundleEntity, final Bundle bundle) {
    log.debug("send bundle {} to Hapi", hapiBundleEntity.getBundleId());

    try {
      final MethodOutcome methodOutcome = postBundleToHapiFhirServer(bundle);

      hapiBundleEntity.setStatus(HapiSyncedStatus.SYNCED);
      hapiBundleEntity.setResponseCode(methodOutcome.getResponseStatusCode());
      hapiBundleEntity.setHapiId(methodOutcome.getId().getIdPart());

      log.info(
          "Hapi bundle successfully synced: {} -> {}",
          hapiBundleEntity.getBundleId(),
          hapiBundleEntity.getHapiId());
    } catch (final RuntimeException ex) {
      log.error("Hapi sync failed -> {} - {}", ex.getClass().getSimpleName(), ex.getMessage());

      hapiBundleEntity.setStatus(getHapiSyncStatus(ex));
      if (ex instanceof BaseServerResponseException responseException) {
        hapiBundleEntity.setResponseCode(responseException.getStatusCode());
      }
      hapiBundleEntity.setError(ex.getMessage());
    }

    repository.save(hapiBundleEntity);
  }

  private MethodOutcome postBundleToHapiFhirServer(final Bundle bundle) {
    final IGenericClient client = fhirContext.newRestfulGenericClient(url);
    if (logRequests) {
      client.registerInterceptor(new LoggingInterceptor(false));
    }
    ICreateTyped hapiClient = client.create().resource(bundle).prefer(MINIMAL).encodedJson();
    propagateTracingContext(hapiClient);
    return hapiClient.execute();
  }

  private void propagateTracingContext(ICreateTyped hapiClient) {
    Span span = tracer.currentSpan();
    if (span == null) {
      return;
    }
    TraceContext context = span.context();

    hapiClient.withAdditionalHeader(B3_HEADER_TRACE_ID, context.traceId());
    hapiClient.withAdditionalHeader(B3_HEADER_SPAN_ID, context.spanId());

    String parentId = context.parentId();
    if (parentId != null) {
      hapiClient.withAdditionalHeader(B3_HEADER_PARENT_ID, parentId);
    }

    Boolean sampled = context.sampled();
    // noinspection ConstantConditions - intellij is confused if 'sampled' is nullable
    if (sampled != null) {
      hapiClient.withAdditionalHeader(B3_HEADER_SAMPLED, sampled ? SAMPLED_ACCEPT : SAMPLED_DENY);
    }
  }

  private HapiSyncedStatus getHapiSyncStatus(final Exception ex) {
    if (hasCause(ex, ConnectException.class)) {
      return HapiSyncedStatus.SERVER_UNAVAILABLE;
    } else if (hasCause(ex, SocketTimeoutException.class)) {
      return HapiSyncedStatus.SERVER_TIMEOUT;
    } else {
      return HapiSyncedStatus.ERROR;
    }
  }
}
