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
import static de.gematik.demis.storage.common.entity.AbstractResourceEntity.COLUMN_RESPONSIBLE_DEPARTMENT;
import static de.gematik.demis.storage.common.entity.AbstractResourceEntity.COLUMN_SOURCE_ID;
import static de.gematik.demis.storage.common.entity.AbstractResourceEntity.COLUMN_TAGS;

import de.gematik.demis.storage.common.entity.Tag;
import de.gematik.demis.storage.reader.common.sql.SqlWhereCondition;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class Filter {
  private OffsetDateTime lastUpdatedLowerBound;
  private OffsetDateTime lastUpdatedUpperBound;
  private String responsibleDepartment;
  private String sourceId;
  private List<Tag> tags; // AND

  public Filter addTag(final Tag tag) {
    if (tags == null) {
      tags = new ArrayList<>();
    }
    tags.add(tag);
    return this;
  }

  public List<SqlWhereCondition> toSqlWhereConditions() {
    final List<SqlWhereCondition> conditions = new ArrayList<>();

    if (lastUpdatedLowerBound != null) {
      conditions.add(SqlWhereCondition.of(COLUMN_LAST_UPDATED, ">=", lastUpdatedLowerBound));
    }
    if (lastUpdatedUpperBound != null) {
      conditions.add(SqlWhereCondition.of(COLUMN_LAST_UPDATED, "<=", lastUpdatedUpperBound));
    }
    if (responsibleDepartment != null) {
      conditions.add(
          SqlWhereCondition.of(COLUMN_RESPONSIBLE_DEPARTMENT, "=", responsibleDepartment));
    }
    if (sourceId != null) {
      conditions.add(SqlWhereCondition.of(COLUMN_SOURCE_ID, "=", sourceId));
    }
    if (tags != null) {
      for (final Tag tag : tags) {
        conditions.add(SqlWhereCondition.of(COLUMN_TAGS, "@>", List.of(tag), true));
      }
    }

    return conditions;
  }
}
