package de.gematik.demis.storage.reader.common;

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

import static de.gematik.demis.storage.reader.api.ParameterNames.PARAM_COUNT;
import static de.gematik.demis.storage.reader.api.ParameterNames.PARAM_FORMAT;
import static de.gematik.demis.storage.reader.api.ParameterNames.PARAM_OFFSET;
import static de.gematik.demis.storage.reader.api.ParameterNames.PARAM_SORT;
import static de.gematik.demis.storage.reader.api.ParameterNames.PARAM_SORT_ASC;
import static de.gematik.demis.storage.reader.api.ParameterNames.PARAM_SORT_DESC;
import static de.gematik.demis.storage.reader.error.ErrorCode.RESOURCE_NOT_FOUND;
import static java.util.Optional.ofNullable;

import de.gematik.demis.storage.common.entity.AbstractResourceEntity;
import de.gematik.demis.storage.reader.common.search.Filter;
import de.gematik.demis.storage.reader.common.search.RequestParamFilterResolver;
import de.gematik.demis.storage.reader.common.search.SearchSetService;
import de.gematik.demis.storage.reader.common.search.SortResolver;
import de.gematik.demis.storage.reader.common.security.Caller;
import de.gematik.demis.storage.reader.config.FssReaderConfigProperties.SearchProps;
import de.gematik.demis.storage.reader.error.ErrorCode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.util.MultiValueMap;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;

@RequiredArgsConstructor
@Slf4j
public abstract class ReadService<
    E extends AbstractResourceEntity, R extends Resource, F extends Filter> {

  private static final Logger QUERY_RESULT_LOGGER = LoggerFactory.getLogger("queryresult");

  private static final Set<String> SUPPORTED_NO_FILTER_PARAMS =
      Set.of(PARAM_SORT, PARAM_SORT_ASC, PARAM_SORT_DESC, PARAM_COUNT, PARAM_OFFSET, PARAM_FORMAT);

  private final ResourceReadonlyRepository<E> repository;
  private final EntityResourceMapper<E, R> mapper;
  private final RequestParamFilterResolver<F> requestParamFilterResolver;
  private final SearchProps searchProps;
  private final SearchSetService searchSetService;
  private final SortResolver sortResolver = new SortResolver();
  private final JsonMapper jsonMapper =
      JsonMapper.builder().disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY).build();

  private static Integer toInteger(final String s) {
    return s == null ? null : Integer.valueOf(s);
  }

  public final R findById(final Caller caller, final UUID id) {
    log.info("Id query: Caller={}, Type={}, Id={}", caller.getName(), getResourceType(), id);
    final E entity =
        repository.findById(id).orElseThrow(() -> RESOURCE_NOT_FOUND.exception(id.toString()));
    checkResourcePermission(caller, entity);
    return mapper.entityToResource(entity);
  }

  /**
   * @return SearchSet Bundle independent of the resources type
   */
  public final Bundle search(
      final Caller caller, final MultiValueMap<String, String> requestParams) {
    log.info(
        "Search query: Caller={}, Type={}, Parameters={}",
        caller.getName(),
        getResourceType(),
        requestParams);

    validateRequestParams(requestParams);
    final F filter = requestParamFilterResolver.createFilterFromRequestParameters(requestParams);

    enforceFilter(caller, filter);
    checkFilterPermission(caller, filter);

    final var pageable =
        createPageable(requestParams).withSort(sortResolver.createSort(requestParams));

    log.debug("Search query prepared database statement. filter={}, pageable={}", filter, pageable);
    final Page<E> result = repository.search(filter, pageable);
    log.debug(
        "Search query executed database statement. EntityIds={}",
        result.map(AbstractResourceEntity::getId));

    final Page<R> resources = result.map(mapper::entityToResource);
    logQueryResult(requestParams, resources);
    return searchSetService.createSearchSet(resources);
  }

  private void logQueryResult(
      final MultiValueMap<String, String> requestParams, final Page<R> resources) {
    if (QUERY_RESULT_LOGGER.isInfoEnabled()) {
      try {
        final List<String> ids = resources.get().map(this::getBusinessId).toList();
        final Map<String, Object> query = new LinkedHashMap<>();
        query.put("resource", getResourceType());
        query.put("parameters", requestParams.toString());
        final Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", resources.getTotalElements());
        result.put("ids", ids);
        final Map<String, Object> queryResult = new LinkedHashMap<>();
        queryResult.put("query", query);
        queryResult.put("result", result);
        QUERY_RESULT_LOGGER.info(jsonMapper.writeValueAsString(queryResult));
      } catch (Exception e) {
        log.warn("Failed to log query result", e);
      }
    }
  }

  protected abstract void checkResourcePermission(final Caller caller, final E entity);

  protected abstract void enforceFilter(final Caller caller, final F filter);

  protected abstract void checkFilterPermission(final Caller caller, final F filter);

  protected abstract String getResourceType();

  protected abstract String getBusinessId(R resource);

  private PageRequest createPageable(final MultiValueMap<String, String> requestParams) {
    final int count =
        Math.min(
            ofNullable(toInteger(requestParams.getFirst(PARAM_COUNT)))
                .orElse(searchProps.defaultPageSize()),
            searchProps.maxPageSize());
    final int offset = ofNullable(toInteger(requestParams.getFirst(PARAM_OFFSET))).orElse(0);

    if (count <= 0) {
      throw ErrorCode.INVALID_PAGING.exception("_count must be greater than 0 but is " + count);
    }
    if (offset < 0) {
      throw ErrorCode.INVALID_PAGING.exception("_offset must be >= 0 but is " + offset);
    }

    // little problem. PageRequest does not support offset - just page
    if (offset % count > 0) {
      log.warn("offset {} is not a multiple of the page size of {}", offset, count);
    }
    return PageRequest.of(offset / count, count);
  }

  private void validateRequestParams(final MultiValueMap<String, String> requestParams) {
    final Set<String> filterParams = requestParamFilterResolver.getSupportedFilterParams();
    final List<String> unsupportedParams =
        requestParams.keySet().stream()
            .filter(
                param ->
                    !filterParams.contains(param)
                        && !SUPPORTED_NO_FILTER_PARAMS.contains(param)
                        && !searchProps.ignoredParams().contains(param))
            .toList();

    if (!unsupportedParams.isEmpty()) {
      throw ErrorCode.UNSUPPORTED_REQUEST_PARAMETER.exception(
          "Unsupported request params: " + unsupportedParams);
    }
  }
}
