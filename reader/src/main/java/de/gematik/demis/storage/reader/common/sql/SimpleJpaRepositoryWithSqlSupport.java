package de.gematik.demis.storage.reader.common.sql;

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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Table;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.jpa.support.PageableUtils;
import org.springframework.data.support.PageableExecutionUtils;

public class SimpleJpaRepositoryWithSqlSupport<T, I> extends SimpleJpaRepository<T, I>
    implements SqlRepository<T> {

  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

  private final EntityManager entityManager;

  public SimpleJpaRepositoryWithSqlSupport(
      final JpaEntityInformation<T, ?> entityInformation, final EntityManager entityManager) {
    super(entityInformation, entityManager);
    this.entityManager = entityManager;
  }

  private static String toOrderSql(final Sort sort) {
    if (sort != null && sort.isSorted()) {
      return "order by "
          + sort.get()
              .map(order -> order.getProperty() + " " + order.getDirection().name())
              .collect(Collectors.joining(", "));
    } else {
      return "";
    }
  }

  private static String toWhereSql(final List<SqlWhereCondition> conditions) {
    if (conditions != null && !conditions.isEmpty()) {
      return "where "
          + conditions.stream().map(SqlWhereCondition::sql).collect(Collectors.joining(" and "));
    } else {
      return "";
    }
  }

  private static String getTableName(final Class<?> domainClass) {
    return domainClass.getAnnotation(Table.class).name();
  }

  private static String createSql(
      final Class<?> domainClass,
      final String selectClause,
      final List<SqlWhereCondition> conditions,
      Sort sort) {
    return String.format(
        "select %s from %s %s %s",
        selectClause, getTableName(domainClass), toWhereSql(conditions), toOrderSql(sort));
  }

  private static void setQueryParameters(
      final Query query, final List<SqlWhereCondition> conditions) {
    if (conditions == null || conditions.isEmpty()) {
      return;
    }

    int pos = 1;
    for (final SqlWhereCondition condition : conditions) {
      query.setParameter(pos++, getParameterValue(condition));
    }
  }

  private static Object getParameterValue(final SqlWhereCondition condition) {
    final Object value = condition.parameterValue();
    return condition.jsonParameter() ? toJson(value) : value;
  }

  private static String toJson(final Object value) {
    try {
      return OBJECT_MAPPER.writeValueAsString(value);
    } catch (final JsonProcessingException e) {
      throw new IllegalArgumentException("error converting parameter value to json: " + value, e);
    }
  }

  @Override
  public Page<T> search(final List<SqlWhereCondition> conditions, final Pageable pageable) {
    final Class<T> domainClass = getDomainClass();
    final String sql = createSql(domainClass, "*", conditions, pageable.getSort());
    final Query query = entityManager.createNativeQuery(sql, domainClass);
    setQueryParameters(query, conditions);

    if (pageable.isPaged()) {
      query.setFirstResult(PageableUtils.getOffsetAsInteger(pageable));
      query.setMaxResults(pageable.getPageSize());
    }

    final List<T> resultList = query.getResultList();

    return PageableExecutionUtils.getPage(resultList, pageable, () -> count(conditions));
  }

  @Override
  public long count(final List<SqlWhereCondition> conditions) {
    final String sql = createSql(getDomainClass(), "count(*)", conditions, null);
    final Query query = entityManager.createNativeQuery(sql, Long.class);
    setQueryParameters(query, conditions);

    return (Long) query.getSingleResult();
  }
}
