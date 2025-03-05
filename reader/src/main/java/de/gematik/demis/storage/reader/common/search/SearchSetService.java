package de.gematik.demis.storage.reader.common.search;

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

import static de.gematik.demis.storage.reader.api.ParameterNames.PARAM_OFFSET;

import de.gematik.demis.storage.reader.common.fhir.SearchSetBuilder;
import jakarta.annotation.Nullable;
import java.util.Date;
import java.util.function.Supplier;
import lombok.Setter;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Setter // just for testing
public class SearchSetService {

  private Supplier<UriComponentsBuilder> uriComponentsBuilderSupplier =
      ServletUriComponentsBuilder::fromCurrentRequest;

  @Value("${fss.reader.server-url}")
  private String serverUrl;

  @Value("${fss.reader.context-path}")
  private String contextPath;

  public Bundle createSearchSet(final Page<? extends Resource> resourcePage) {
    final UriComponentsBuilder linkBuilder = getLinkBuilder();
    final String selfLink = linkBuilder.build(true).toUriString();
    final String nextLink = getNextLink(linkBuilder, resourcePage);

    return new SearchSetBuilder()
        .setLastUpdated(new Date())
        .setResources(resourcePage.getContent())
        .setTotalElements((int) resourcePage.getTotalElements())
        .setResourceBaseUrl(getResourceBaseUrl())
        .setSelfLink(selfLink)
        .setNextLink(nextLink)
        .build();
  }

  private String getResourceBaseUrl() {
    return UriComponentsBuilder.fromUriString(serverUrl).path(contextPath).build().toUriString();
  }

  @Nullable
  private String getNextLink(
      final UriComponentsBuilder uriBuilder, final Page<? extends Resource> resourcePage) {
    if (resourcePage.hasNext()) {
      final Pageable nextPageable = resourcePage.nextPageable();
      return uriBuilder
          .replaceQueryParam(PARAM_OFFSET, nextPageable.getOffset())
          .build(true)
          .toString();
    } else {
      return null;
    }
  }

  private UriComponentsBuilder getLinkBuilder() {
    final UriComponentsBuilder currentUriBuilder = uriComponentsBuilderSupplier.get();
    final UriComponents currentUri = currentUriBuilder.build(true);
    final UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(serverUrl);
    final String currentPath = currentUri.getPath();
    if (currentPath != null) {
      uriBuilder.path(currentPath);
    }
    uriBuilder.query(currentUri.getQuery());
    return uriBuilder;
  }
}
