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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.gematik.demis.storage.common.entity.BundleEntity;
import de.gematik.demis.storage.common.entity.Tag;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;

class SimpleJpaRepositoryWithSqlSupportTest {

  private EntityManager entityManager;

  private SimpleJpaRepositoryWithSqlSupport<BundleEntity, UUID> underTest;

  @BeforeEach
  void setup() {
    entityManager = mock(EntityManager.class);
    when(entityManager.getDelegate()).thenReturn("Not Relevant");
    final JpaEntityInformation<BundleEntity, UUID> entityInformation =
        mock(JpaEntityInformation.class);
    when(entityInformation.getJavaType()).thenReturn(BundleEntity.class);

    underTest = new SimpleJpaRepositoryWithSqlSupport<>(entityInformation, entityManager);
  }

  @Test
  void search() {
    final var dbResult = List.of(new BundleEntity(), new BundleEntity());

    final Query query = Mockito.mock(Query.class);
    when(entityManager.createNativeQuery(Mockito.anyString(), Mockito.eq(BundleEntity.class)))
        .thenReturn(query);
    when(query.getResultList()).thenReturn(dbResult);

    final var conditions = createWhereConditions();
    final var pageable = Pageable.unpaged(Sort.by("my_sort_col"));

    final Page<BundleEntity> actualResult = underTest.search(conditions, pageable);

    final Page<BundleEntity> expectedPage = new PageImpl<>(dbResult, pageable, dbResult.size());
    assertThat(actualResult).isEqualTo(expectedPage);

    assertSql(
        "select * from bundles where my_first_col = ? and my_second_col = ?::jsonb order by my_sort_col ASC");

    assertParameters(query, "my-value", "[{\"system\":\"my-system\",\"code\":\"my-code\"}]");
  }

  private static void assertParameters(final Query query, Object... expectedValues) {
    final int parameterCount = expectedValues.length;
    final var expectedIndexList =
        IntStream.range(1, parameterCount + 1).boxed().toArray(Integer[]::new);
    final ArgumentCaptor<Integer> parameterIndexArgument = ArgumentCaptor.forClass(Integer.class);
    final ArgumentCaptor<Object> parameterValueArgument = ArgumentCaptor.forClass(Object.class);
    Mockito.verify(query, times(parameterCount))
        .setParameter(parameterIndexArgument.capture(), parameterValueArgument.capture());
    assertThat(parameterIndexArgument.getAllValues()).containsExactly(expectedIndexList);
    assertThat(parameterValueArgument.getAllValues()).containsExactly(expectedValues);
  }

  @Test
  void paging() {
    final var dbResult = List.of(new BundleEntity(), new BundleEntity(), new BundleEntity());

    final Query searchQuery = Mockito.mock(Query.class);
    when(entityManager.createNativeQuery(Mockito.anyString(), Mockito.eq(BundleEntity.class)))
        .thenReturn(searchQuery);
    when(searchQuery.getResultList()).thenReturn(dbResult);

    final long count = 30;
    final Query countQuery = Mockito.mock(Query.class);
    when(entityManager.createNativeQuery(Mockito.anyString(), Mockito.eq(Long.class)))
        .thenReturn(countQuery);
    when(countQuery.getSingleResult()).thenReturn(count);

    final PageRequest pageRequest = PageRequest.of(4, 3);
    final Page<BundleEntity> page = underTest.search(createWhereConditions(), pageRequest);

    final Page<BundleEntity> expectedPage = new PageImpl<>(dbResult, pageRequest, count);
    assertThat(page).isEqualTo(expectedPage);

    verify(searchQuery).setFirstResult(12);
    verify(searchQuery).setMaxResults(3);
  }

  private void assertSql(final String expectedSql) {
    final ArgumentCaptor<String> sqlArgument = ArgumentCaptor.forClass(String.class);
    Mockito.verify(entityManager)
        .createNativeQuery(sqlArgument.capture(), Mockito.any(Class.class));
    assertThat(sqlArgument.getValue()).isEqualToNormalizingWhitespace(expectedSql);
  }

  private static List<SqlWhereCondition> createWhereConditions() {
    final Tag tag = new Tag().setSystem("my-system").setCode("my-code");
    final SqlWhereCondition one = new SqlWhereCondition("my_first_col = ?", "my-value", false);
    final SqlWhereCondition two =
        new SqlWhereCondition("my_second_col = ?::jsonb", List.of(tag), true);
    return List.of(one, two);
  }

  @Test
  void count() {
    final long expectedCount = 8;
    final Query query = Mockito.mock(Query.class);
    when(entityManager.createNativeQuery(Mockito.anyString(), Mockito.eq(Long.class)))
        .thenReturn(query);
    when(query.getSingleResult()).thenReturn(expectedCount);

    final List<SqlWhereCondition> conditions = createWhereConditions();
    final long actual = underTest.count(conditions);

    assertThat(actual).isEqualTo(expectedCount);

    assertSql("select count(*) from bundles where my_first_col = ? and my_second_col = ?::jsonb");
  }
}
