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

import static de.gematik.demis.storage.reader.test.TestData.createTags;
import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.demis.storage.common.entity.Tag;
import de.gematik.demis.storage.reader.common.sql.SqlWhereCondition;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class FilterTest {

  private final Filter underTest = new Filter();

  @Test
  void toSqlWhereConditions_all() {
    final List<Tag> tags = createTags();
    underTest.setLastUpdatedLowerBound(OffsetDateTime.MIN);
    underTest.setLastUpdatedUpperBound(OffsetDateTime.MAX);
    underTest.setSourceId("my-source-id");
    underTest.setResponsibleDepartment("1.053.2");
    underTest.setTags(tags);

    final List<SqlWhereCondition> result = underTest.toSqlWhereConditions();

    assertThat(result)
        .isNotNull()
        .containsExactlyInAnyOrder(
            new SqlWhereCondition("last_updated >= ?", underTest.getLastUpdatedLowerBound(), false),
            new SqlWhereCondition("last_updated <= ?", underTest.getLastUpdatedUpperBound(), false),
            new SqlWhereCondition(
                "responsible_department = ?", underTest.getResponsibleDepartment(), false),
            new SqlWhereCondition("source_id = ?", underTest.getSourceId(), false),
            new SqlWhereCondition("tags @> ?::jsonb", List.of(tags.get(0)), true),
            new SqlWhereCondition("tags @> ?::jsonb", List.of(tags.get(1)), true),
            new SqlWhereCondition("tags @> ?::jsonb", List.of(tags.get(2)), true));
  }

  @Test
  void toSqlWhereConditions_none() {
    final List<SqlWhereCondition> result = underTest.toSqlWhereConditions();
    assertThat(result).isNotNull().isEmpty();
  }
}
