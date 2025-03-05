package de.gematik.demis.storage.reader.common.fhir;

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

import java.util.Date;
import java.util.List;
import lombok.Setter;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.SearchEntryMode;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.web.util.UriComponentsBuilder;

@Setter
public class SearchSetBuilder {

  private static final String LINK_RELATION_SELF = "self";
  private static final String LINK_RELATION_NEXT = "next";

  private Date lastUpdated;
  private List<? extends Resource> resources;
  private int totalElements;
  private String resourceBaseUrl;
  private String selfLink;
  private String nextLink;

  public Bundle build() {
    final Bundle bundle = new Bundle();
    // Note: we do not set the id of the searchset bundle. Change to NCAPI
    bundle.setType(Bundle.BundleType.SEARCHSET);
    bundle.setTotal(totalElements);
    bundle.getMeta().setLastUpdated(lastUpdated);

    addLinks(bundle);
    addEntries(bundle);
    return bundle;
  }

  private void addLinks(final Bundle bundle) {
    if (selfLink != null) {
      bundle.addLink().setRelation(LINK_RELATION_SELF).setUrl(selfLink);
    }

    if (nextLink != null) {
      bundle.addLink().setRelation(LINK_RELATION_NEXT).setUrl(nextLink);
    }
  }

  private void addEntries(final Bundle bundle) {
    for (final Resource resource : resources) {
      bundle
          .addEntry()
          .setResource(resource)
          .setFullUrl(buildFullUrl(resource))
          .getSearch()
          .setMode(SearchEntryMode.MATCH);
    }
  }

  private String buildFullUrl(final Resource resource) {
    return UriComponentsBuilder.fromUriString(resourceBaseUrl)
        .pathSegment(resource.fhirType(), resource.getId())
        .build()
        .toUriString();
  }
}
