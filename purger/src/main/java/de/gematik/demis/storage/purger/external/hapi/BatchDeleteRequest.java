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

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;

@RequiredArgsConstructor
final class BatchDeleteRequest {

  private final List<HapiBundle> bundles;
  private final Bundle batch = new Bundle();

  Bundle createBundle() {
    batch.setType(Bundle.BundleType.BATCH);
    bundles.stream().map(HapiBundle::hapiId).sorted().forEach(this::add);
    return batch;
  }

  private void add(String bundleId) {
    final Bundle.BundleEntryComponent entry = new Bundle.BundleEntryComponent();
    entry.setRequest(
        new Bundle.BundleEntryRequestComponent()
            .setMethod(Bundle.HTTPVerb.DELETE)
            .setUrl("Bundle/" + bundleId));
    batch.addEntry(entry);
  }
}
