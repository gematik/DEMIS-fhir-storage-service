package de.gematik.demis.storage.reader.bundle;

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

import static de.gematik.demis.storage.common.entity.BundleEntity.COLUMN_NOTIFICATION_BUNDLE_ID;
import static de.gematik.demis.storage.common.entity.BundleEntity.COLUMN_PROFILE;

import de.gematik.demis.storage.reader.common.search.Filter;
import de.gematik.demis.storage.reader.common.sql.SqlWhereCondition;
import java.util.List;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class BundleFilter extends Filter {

  private Set<String> profiles;
  private String notificationBundleId;

  @Override
  public List<SqlWhereCondition> toSqlWhereConditions() {
    final List<SqlWhereCondition> conditions = super.toSqlWhereConditions();

    if (profiles != null) {
      conditions.add(SqlWhereCondition.of(COLUMN_PROFILE, "in", profiles));
    }

    if (notificationBundleId != null) {
      conditions.add(
          SqlWhereCondition.of(COLUMN_NOTIFICATION_BUNDLE_ID, "=", notificationBundleId));
    }

    return conditions;
  }
}
