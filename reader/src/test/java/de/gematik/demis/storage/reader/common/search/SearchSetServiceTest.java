package de.gematik.demis.storage.reader.common.search;

/*-
 * #%L
 * fhir-storage-reader
 * %%
 * Copyright (C) 2025 - 2026 gematik GmbH
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
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik,
 * find details in the "Readme" file.
 * #L%
 */

import static de.gematik.demis.storage.reader.test.TestUtil.assertFhirResource;
import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.demis.storage.reader.config.FssReaderConfigProperties;
import de.gematik.demis.storage.reader.test.TestUtil;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleLinkComponent;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.util.UriComponentsBuilder;

class SearchSetServiceTest {

  // Note: meta.lastUpdated is asserted separately

  private static final String EXPECTED =
"""
    {
      "resourceType": "Bundle",
      "type": "searchset",
      "total": 5,
      "link": [ {
        "relation": "self",
        "url": "https://demis.de/Bundle?_lastUpdated=gt2024-01-01&_count=2"
      }, {
        "relation": "next",
        "url": "https://demis.de/Bundle?_lastUpdated=gt2024-01-01&_count=2&_offset=2"
      } ],
      "entry": [ {
        "fullUrl": "https://demis.de/Bundle/Test-1",
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
        "fullUrl": "https://demis.de/Bundle/Test-2",
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
    }
""";

  private final String LINKS_TEMPLATE =
"""
      {
        "resourceType": "Bundle",
        "link": [ {
          "relation": "self",
          "url": "%s"
        }, {
          "relation": "next",
          "url": "%s"
        }, {
          "relation": "previous",
          "url": "%s"
        } ]
      }
""";

  private static final String EMPTY_EXPECTED =
"""
{
  "resourceType": "Bundle",
  "type": "searchset",
  "total": 0,
  "link": [ {
    "relation": "self",
    "url": "https://demis.de/Bundle?_lastUpdated=gt2024-01-01&_count=2"
  } ]
}
""";

  private final SearchSetService underTest =
      new SearchSetService(
          new FssReaderConfigProperties.SearchProps(100, 10, Set.of("_pretty", "other")));

  private static Bundle createBundle(final String identifier) {
    final var bundle = new Bundle();
    bundle.setType(Bundle.BundleType.DOCUMENT);
    bundle.setId(identifier);
    bundle.getMeta().setVersionId("1");
    return bundle;
  }

  @BeforeEach
  void setupService() {
    final var requestUrl = "http://localhost:9091/Bundle?_lastUpdated=gt2024-01-01&_count=2";

    setupSelfLink(requestUrl);
    underTest.setServerUrl("https://demis.de");
    underTest.setContextPath("/");
  }

  @Test
  void createSearchSetForFirstPage() {
    final var resources = List.of(createBundle("Test-1"), createBundle("Test-2"));
    final var page = new PageImpl<>(resources, Pageable.ofSize(2), 5);

    final Bundle searchSetBundle = underTest.createSearchSet(page);

    assertThat(searchSetBundle.getMeta().getLastUpdated()).isCloseTo(new Date(), 1000);
    searchSetBundle.getMeta().setLastUpdated(null);
    assertFhirResource(searchSetBundle, EXPECTED);
  }

  @Test
  void middlePageWithNextAndPreviousLinks() {
    final String self = "https://demis.de/Bundle?_lastUpdated=gt2024-01-01&_count=2&_offset=2";
    final String next = "https://demis.de/Bundle?_lastUpdated=gt2024-01-01&_count=2&_offset=4";
    final String prev = "https://demis.de/Bundle?_lastUpdated=gt2024-01-01&_count=2&_offset=0";
    final String expectedLinks = LINKS_TEMPLATE.formatted(self, next, prev);
    setupSelfLink(self);

    final var resources = List.of(createBundle("Test-1"), createBundle("Test-2"));
    final var page = new PageImpl<>(resources, Pageable.ofSize(2).withPage(1), 5);

    final Bundle searchSetBundle = underTest.createSearchSet(page);
    final List<BundleLinkComponent> links = searchSetBundle.getLink();
    TestUtil.assertFhirResource(new Bundle().setLink(links), expectedLinks);
  }

  @Test
  void noNextLink() {
    final var resources = List.of(createBundle("Test-1"), createBundle("Test-2"));
    final var page = new PageImpl<>(resources, Pageable.ofSize(2), 2);

    final Bundle searchSetBundle = underTest.createSearchSet(page);
    final List<BundleLinkComponent> links = searchSetBundle.getLink();
    assertThat(links).hasSize(1);
    final var selfLink = links.getFirst();
    assertThat(selfLink.getRelation()).isEqualTo("self");
  }

  @Test
  void emptyResult() {
    final Page<Resource> page = new PageImpl<>(List.of(), Pageable.ofSize(2), 0);
    final Bundle searchSetBundle = underTest.createSearchSet(page);
    searchSetBundle.getMeta().setLastUpdated(null);
    assertFhirResource(searchSetBundle, EMPTY_EXPECTED);
  }

  @Test
  void cleanLink() {
    final String requestUrl =
        "https://demis.de/Bundle?_format=json&_format=json&_format=json&_profile=a&_profile=b&_pretty=true&_count=2";
    final String cleanedUri = "https://demis.de/Bundle?_format=json&_profile=a&_profile=b&_count=2";
    final String expectedLinks =
        LINKS_TEMPLATE.formatted(cleanedUri, cleanedUri + "&_offset=4", cleanedUri + "&_offset=0");
    setupSelfLink(requestUrl);

    final var resources = List.of(createBundle("Test-1"), createBundle("Test-2"));
    final var page = new PageImpl<>(resources, Pageable.ofSize(2).withPage(1), 5);

    final Bundle searchSetBundle = underTest.createSearchSet(page);
    final List<BundleLinkComponent> links = searchSetBundle.getLink();
    TestUtil.assertFhirResource(new Bundle().setLink(links), expectedLinks);
  }

  private void setupSelfLink(final String selfLinkUri) {
    underTest.setUriComponentsBuilderSupplier(
        () -> UriComponentsBuilder.fromUriString(selfLinkUri));
  }
}
