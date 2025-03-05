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

import static de.gematik.demis.storage.reader.test.TestUtil.assertFhirResource;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.Test;

class SearchSetBuilderTest {

  private static final String EXPECTED =
      """
{
  "resourceType": "Bundle",
  "meta": {
    "lastUpdated": "2024-08-01T17:17:31.906+02:00"
  },
  "type": "searchset",
  "total": 5,
  "link": [ {
    "relation": "self",
    "url": "https://demis.de/notification-clearing-api/fhir/Bundle?_lastUpdated=gt2024-01-01&_count=2"
  }, {
    "relation": "next",
    "url": "https://demis.de/notification-clearing-api/fhir/Bundle?_lastUpdated=gt2024-01-01&_count=2&_offset=2"
  } ],
  "entry": [ {
    "fullUrl": "https://demis.de/notification-clearing-api/fhir/Bundle/Test-1",
    "resource": {
      "resourceType": "Bundle",
      "id": "Test-1",
      "meta": {
        "versionId": "1"
      },
      "type": "document"
    },
    "search": {
      "mode": "match"
    }
  }, {
    "fullUrl": "https://demis.de/notification-clearing-api/fhir/Bundle/Test-2",
    "resource": {
      "resourceType": "Bundle",
      "id": "Test-2",
      "meta": {
        "versionId": "1"
      },
      "type": "document"
    },
    "search": {
      "mode": "match"
    }
  } ]
}""";

  private static Bundle createBundle(final String identifier) {
    final var bundle = new Bundle();
    bundle.setType(Bundle.BundleType.DOCUMENT);
    bundle.setId(identifier);
    bundle.getMeta().setVersionId("1");
    return bundle;
  }

  @Test
  void test() {
    final Date lastUpdated =
        Date.from(ZonedDateTime.parse("2024-08-01T17:17:31.906+02:00").toInstant());
    final var resources = List.of(createBundle("Test-1"), createBundle("Test-2"));
    final SearchSetBuilder underTest = new SearchSetBuilder();
    underTest
        .setResources(resources)
        .setLastUpdated(lastUpdated)
        .setTotalElements(5)
        .setResourceBaseUrl("https://demis.de/notification-clearing-api/fhir")
        .setSelfLink(
            "https://demis.de/notification-clearing-api/fhir/Bundle?_lastUpdated=gt2024-01-01&_count=2")
        .setNextLink(
            "https://demis.de/notification-clearing-api/fhir/Bundle?_lastUpdated=gt2024-01-01&_count=2&_offset=2");

    final Bundle searchSetBundle = underTest.build();

    assertFhirResource(searchSetBundle, EXPECTED);
  }
}
