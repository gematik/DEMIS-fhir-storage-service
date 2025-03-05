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

import static de.gematik.demis.storage.common.fhir.DemisFhirNames.RESPONSIBLE_HEALTH_DEPARTMENT_CODING_SYSTEM;
import static de.gematik.demis.storage.common.util.DateUtil.toOffsetDataTime;
import static de.gematik.demis.storage.reader.api.ParameterNames.PARAM_LAST_UPDATED;
import static de.gematik.demis.storage.reader.api.ParameterNames.PARAM_SOURCE;
import static de.gematik.demis.storage.reader.api.ParameterNames.PARAM_TAG;
import static de.gematik.demis.storage.reader.error.ErrorCode.INVALID_FILTER;

import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.rest.api.QualifiedParamList;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.demis.storage.common.entity.Tag;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.util.MultiValueMap;

@RequiredArgsConstructor
public class RequestParamFilterResolver<F extends Filter> {

  private static final Map<String, BiConsumer<List<String>, Filter>> COMMON_FILTER_PARAMETER =
      Map.ofEntries(
          Map.entry(PARAM_LAST_UPDATED, RequestParamFilterResolver::lastUpdated),
          Map.entry(PARAM_TAG, RequestParamFilterResolver::tag),
          Map.entry(PARAM_SOURCE, RequestParamFilterResolver::source));

  private final Supplier<F> filterInstanceSupplier;
  private final Map<String, BiConsumer<List<String>, F>> additionalParameter;

  public F createFilterFromRequestParameters(final MultiValueMap<String, String> requestParams) {
    final F filter = filterInstanceSupplier.get();
    COMMON_FILTER_PARAMETER.forEach(
        (name, action) -> setFilterFromRequestParameter(name, action, requestParams, filter));
    additionalParameter.forEach(
        (name, action) -> setFilterFromRequestParameter(name, action, requestParams, filter));
    return filter;
  }

  private static <T extends Filter> void setFilterFromRequestParameter(
      final String name,
      final BiConsumer<List<String>, T> action,
      final MultiValueMap<String, String> requestParams,
      final T filter) {
    final List<String> values = requestParams.get(name);
    if (values != null) {
      action.accept(values, filter);
    }
  }

  private static void lastUpdated(final List<String> values, final Filter filter) {
    final List<QualifiedParamList> params =
        values.stream().map(QualifiedParamList::singleton).toList();
    final DateRangeParam dateRangeParam = new DateRangeParam();
    try {
      dateRangeParam.setValuesAsQueryTokens(null, null, params);
    } catch (final InvalidRequestException | DataFormatException ex) {
      throw INVALID_FILTER.exception(ex.getMessage());
    }
    filter.setLastUpdatedLowerBound(toOffsetDataTime(dateRangeParam.getLowerBoundAsInstant()));
    filter.setLastUpdatedUpperBound(toOffsetDataTime(dateRangeParam.getUpperBoundAsInstant()));
  }

  private static void tag(final List<String> tags, final Filter filter) {
    for (final String tagString : tags) {
      final TokenParam tokenParam = new TokenParam();
      tokenParam.setValueAsQueryToken(null, null, null, tagString);

      if (RESPONSIBLE_HEALTH_DEPARTMENT_CODING_SYSTEM.equals(tokenParam.getSystem())) {
        filter.setResponsibleDepartment(tokenParam.getValue());
      } else {
        filter.addTag(new Tag().setSystem(tokenParam.getSystem()).setCode(tokenParam.getValue()));
      }
    }
  }

  private static void source(final List<String> values, final Filter filter) {
    if (values.size() > 1) {
      throw INVALID_FILTER.exception("at most one source query parameter");
    }
    filter.setSourceId(values.getFirst());
  }
}
