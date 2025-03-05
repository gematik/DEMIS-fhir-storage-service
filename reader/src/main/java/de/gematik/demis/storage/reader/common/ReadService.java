package de.gematik.demis.storage.reader.common;

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

import static de.gematik.demis.storage.reader.api.ParameterNames.PARAM_COUNT;
import static de.gematik.demis.storage.reader.api.ParameterNames.PARAM_OFFSET;
import static de.gematik.demis.storage.reader.error.ErrorCode.RESOURCE_NOT_FOUND;
import static java.util.Optional.ofNullable;

import de.gematik.demis.storage.common.entity.AbstractResourceEntity;
import de.gematik.demis.storage.common.reader.EntityResourceMapper;
import de.gematik.demis.storage.reader.common.search.Filter;
import de.gematik.demis.storage.reader.common.search.RequestParamFilterResolver;
import de.gematik.demis.storage.reader.common.search.SearchSetService;
import de.gematik.demis.storage.reader.common.search.SortResolver;
import de.gematik.demis.storage.reader.common.security.Caller;
import de.gematik.demis.storage.reader.config.FssReaderConfigProperties.SearchProps;
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

@RequiredArgsConstructor
@Slf4j
public abstract class ReadService<
    E extends AbstractResourceEntity, R extends Resource, F extends Filter> {

  private static final Logger QUERY_RESULT_LOGGER = LoggerFactory.getLogger("queryresult");

  private final ResourceReadonlyRepository<E> repository;
  private final EntityResourceMapper<E, R> mapper;
  private final RequestParamFilterResolver<F> requestParamFilterResolver;
  private final SearchProps searchProps;
  private final SearchSetService searchSetService;
  private final SortResolver sortResolver = new SortResolver();

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
      final var ids = resources.get().map(this::getBusinessId).toArray();
      QUERY_RESULT_LOGGER.info(
          "{} {} -> {} {}", getResourceType(), requestParams, resources.getTotalElements(), ids);
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

    // little problem. PageRequest does not support offset - just page
    return PageRequest.of(offset / count, count);
  }
}
