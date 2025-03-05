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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.mockito.Mockito.when;

import de.gematik.demis.service.base.error.ServiceException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.util.MultiValueMap;

@ExtendWith(MockitoExtension.class)
class SortResolverTest {

  @Mock private MultiValueMap<String, String> requestParams;
  private Sort sort;

  private void applySortParameters() {
    this.sort = new SortResolver().createSort(this.requestParams);
  }

  @Test
  void givenEmptySortParametersWhenApplyThenSortAscLastUpdated() {
    applySortParameters();
    verifyAscLastUpdated();
  }

  @Test
  void givenSortAscLastUpdatedSortParametersWhenApplyThenSortAscLastUpdated() {
    when(this.requestParams.get("_sort:asc")).thenReturn(List.of("_lastUpdated"));
    applySortParameters();
    verifyAscLastUpdated();
  }

  @Test
  void givenSortLastUpdatedSortParametersWhenApplyThenSortAscLastUpdated() {
    when(this.requestParams.get("_sort")).thenReturn(List.of("_lastUpdated"));
    when(this.requestParams.get("_sort:asc")).thenReturn(null);
    applySortParameters();
    verifyAscLastUpdated();
  }

  @Test
  void givenSortDescLastUpdatedSortParametersWhenApplyThenThrowTooComplexSearchException() {
    when(this.requestParams.get("_sort")).thenReturn(null);
    when(this.requestParams.get("_sort:asc")).thenReturn(null);
    when(this.requestParams.get("_sort:desc")).thenReturn(List.of("_lastUpdated"));
    assertThatException()
        .isThrownBy(this::applySortParameters)
        .isInstanceOf(ServiceException.class)
        .withMessage("Descending sort is not allowed. Found: 1");
  }

  @Test
  void givenUnknownSortParametersWhenApplyThenThrowTooComplexSearchException() {
    when(this.requestParams.get("_sort:asc")).thenReturn(List.of("unknown"));
    assertThatException()
        .isThrownBy(this::applySortParameters)
        .isInstanceOf(ServiceException.class)
        .withMessage("Only ascending sort by lastUpdated is allowed. Found: unknown");
  }

  private void verifyAscLastUpdated() {
    assertThat(this.sort).isNotNull();
    Sort.Order order = this.sort.getOrderFor("last_updated");
    assertThat(order).isNotNull();
    assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
  }
}
