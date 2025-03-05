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

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.gclient.ITransactionTyped;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.stereotype.Service;

/** Communication with HAPI FHIR server. */
@ConditionalOnHapiSync
@RequiredArgsConstructor
@Service
@Slf4j
class HapiFhirServerService {

  private static final String B3_HEADER_TRACE_ID = "X-B3-TraceId";
  private static final String B3_HEADER_PARENT_ID = "X-B3-ParentSpanId";
  private static final String B3_HEADER_SPAN_ID = "X-B3-SpanId";
  private static final String B3_HEADER_SAMPLED = "X-B3-Sampled";
  private static final String SAMPLED_ACCEPT = "1";
  private static final String SAMPLED_DENY = "0";

  private final Tracer tracer;
  private final FhirContext fhirContext;
  private final HapiFhirServerConfiguration hapiFhirServerConfiguration;
  private final HapiFhirConfiguration hapiFhirConfiguration;

  /**
   * Purges FHIR bundle resources at HAPI FHIR server.
   *
   * @param bundles List of bundle IDs to be purged
   * @return List of bundle IDs failed to be purged
   */
  List<HapiBundle> purge(List<HapiBundle> bundles) {
    log.info(
        "Purging bundles at HAPI FHIR server. Url: {} Bundles: {}",
        hapiFhirServerConfiguration.url(),
        bundles.size());
    return purgeBundles(bundles);
  }

  private List<HapiBundle> purgeBundles(List<HapiBundle> bundles) {

    // prepare
    final Bundle batchDelete = new BatchDeleteRequest(bundles).createBundle();
    final IGenericClient client = createHapiFhirClient();

    // execute
    final Bundle response;
    try {
      final ITransactionTyped<Bundle> request =
          client.transaction().withBundle(batchDelete).encodedJson();
      propagateTracingContext(request);
      logRequest(batchDelete);
      response = request.execute();
    } catch (Exception e) {
      throw new IllegalStateException(
          "Failed to purge bundles at HAPI FHIR server. Bundles: " + bundles.size(), e);
    }

    // process
    logResponse(response);
    return new BatchDeleteResponse(bundles, batchDelete, response).getFailures();
  }

  private void logRequest(Bundle batchDelete) {
    if (log.isDebugEnabled()) {
      log.debug(
          "Sending request to HAPI FHIR server: {}",
          fhirContext.newJsonParser().setPrettyPrint(false).encodeResourceToString(batchDelete));
    }
  }

  private void logResponse(Bundle response) {
    if (log.isDebugEnabled()) {
      log.debug(
          "Received response from HAPI FHIR server: {}",
          fhirContext.newJsonParser().setPrettyPrint(false).encodeResourceToString(response));
    }
  }

  private IGenericClient createHapiFhirClient() {
    final IGenericClient client =
        fhirContext.newRestfulGenericClient(hapiFhirServerConfiguration.url());
    if (hapiFhirConfiguration.isClientLogRequests()) {
      client.registerInterceptor(new LoggingInterceptor(false));
    }
    return client;
  }

  private void propagateTracingContext(ITransactionTyped<Bundle> request) {
    final Span span = tracer.currentSpan();
    if (span == null) {
      return;
    }
    final TraceContext context = span.context();
    request.withAdditionalHeader(B3_HEADER_TRACE_ID, context.traceId());
    request.withAdditionalHeader(B3_HEADER_SPAN_ID, context.spanId());
    final String parentId = context.parentId();
    if (parentId != null) {
      request.withAdditionalHeader(B3_HEADER_PARENT_ID, parentId);
    }
    final Boolean sampled = context.sampled();
    // noinspection ConstantConditions - intellij is confused if 'sampled' is nullable
    if (sampled != null) {
      request.withAdditionalHeader(B3_HEADER_SAMPLED, sampled ? SAMPLED_ACCEPT : SAMPLED_DENY);
    }
  }
}
