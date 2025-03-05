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

public record SqlWhereCondition(String sql, Object parameterValue, boolean jsonParameter) {
  public static SqlWhereCondition of(String dbColumn, String operator, Object value) {
    return of(dbColumn, operator, value, false);
  }

  public static SqlWhereCondition of(
      final String dbColumn,
      final String operator,
      final Object value,
      final boolean jsonParameter) {
    return new SqlWhereCondition(
        dbColumn + " " + operator + " ?" + (jsonParameter ? "::jsonb" : ""), value, jsonParameter);
  }
}
