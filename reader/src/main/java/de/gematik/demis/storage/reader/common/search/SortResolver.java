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

import static de.gematik.demis.storage.common.entity.AbstractResourceEntity.COLUMN_LAST_UPDATED;
import static de.gematik.demis.storage.reader.api.ParameterNames.*;

import de.gematik.demis.service.base.error.ServiceException;
import de.gematik.demis.storage.reader.error.ErrorCode;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.util.MultiValueMap;

/** Factory of Spring data sort objects */
@RequiredArgsConstructor
public final class SortResolver {

  private static final Order ASC_LAST_UPDATED = Order.asc(COLUMN_LAST_UPDATED);

  /**
   * Apply sort parameters to the given page request
   *
   * @param requestParams the request parameters
   * @throws ServiceException if the sort is too complex
   */
  public Sort createSort(MultiValueMap<String, String> requestParams) {
    verifySimplicity(requestParams);
    return Sort.by(ASC_LAST_UPDATED);
  }

  private void verifySimplicity(MultiValueMap<String, String> requestParams) {
    final List<String> ascending = getAscendingColumns(requestParams);
    if (!ascending.isEmpty()) {
      if (ascending.size() > 1) {
        throw ErrorCode.UNSUPPORTED_SORT.exception(
            "Only one ascending sort parameter is allowed. Found: " + ascending.size());
      }
      if (!ascending.getFirst().equals(PARAM_LAST_UPDATED)) {
        throw ErrorCode.UNSUPPORTED_SORT.exception(
            "Only ascending sort by lastUpdated is allowed. Found: " + ascending.getFirst());
      }
    }
    final List<String> descending = getDescendingColumns(requestParams);
    if (!descending.isEmpty()) {
      throw ErrorCode.UNSUPPORTED_SORT.exception(
          "Descending sort is not allowed. Found: " + descending.size());
    }
  }

  private List<String> getAscendingColumns(MultiValueMap<String, String> requestParams) {
    final List<String> ascending = new LinkedList<>();
    // explicitly set ascending columns
    List<String> columns = requestParams.get(PARAM_SORT_ASC);
    if (columns != null) {
      ascending.addAll(columns);
    }
    // implicitly set ascending columns
    columns = requestParams.get(PARAM_SORT);
    if (columns != null) {
      ascending.addAll(columns);
    }
    return ascending;
  }

  private List<String> getDescendingColumns(MultiValueMap<String, String> requestParams) {
    final List<String> columns = requestParams.get(PARAM_SORT_DESC);
    if (columns == null) {
      return Collections.emptyList();
    }
    return columns;
  }
}
