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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;

@RequiredArgsConstructor
final class BatchDeleteResponse {

  private static final String BUNDLE_ID_PREFIX = "Bundle/";

  private final List<HapiBundle> bundles;
  private final Bundle request;
  private final Bundle response;

  private Map<String, HapiBundle> hapiIdIndex;

  private static boolean hasFailed(Bundle.BundleEntryComponent entry) {
    final String status = entry.getResponse().getStatus();
    return status.startsWith("4") || status.startsWith("5");
  }

  List<HapiBundle> getFailures() {
    if (!hasFailures()) {
      return Collections.emptyList();
    }
    try {
      return evaluateResponse();
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to evaluate HAPI FHIR server response.", e);
    }
  }

  private List<HapiBundle> evaluateResponse() {
    final List<HapiBundle> failures = new ArrayList<>();
    final List<Bundle.BundleEntryComponent> responses = response.getEntry();
    for (int i = 0; i < responses.size(); i++) {
      if (hasFailed(responses.get(i))) {
        failures.add(getHapiBundle(i));
      }
    }
    return failures;
  }

  private HapiBundle getHapiBundle(int entryIndex) {
    final String url = request.getEntry().get(entryIndex).getRequest().getUrl();
    final String hapiId =
        url.substring(url.lastIndexOf(BUNDLE_ID_PREFIX) + BUNDLE_ID_PREFIX.length());
    return getHapiBundle(hapiId);
  }

  private HapiBundle getHapiBundle(String hapiId) {
    if (hapiIdIndex == null) {
      hapiIdIndex =
          bundles.stream().collect(Collectors.toMap(HapiBundle::hapiId, Function.identity()));
    }
    return Objects.requireNonNull(hapiIdIndex.get(hapiId), "HAPI ID not found: " + hapiId);
  }

  private boolean hasFailures() {
    return response.getEntry().stream().anyMatch(BatchDeleteResponse::hasFailed);
  }
}
