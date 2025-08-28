package de.gematik.demis.storage.purger.internal.purgequery;

/*-
 * #%L
 * fhir-storage-purger
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
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 * #L%
 */

final class DefaultPeriodPurgeQuery implements PurgeQuery {

  /** SQL statement to delete expired records from the database. The default period is applied. */
  private static final String STATEMENT_PURGE_DEFAULT_PERIOD =
      """
            DELETE FROM ${TABLE}
            WHERE ctid IN (
                SELECT ctid
                FROM ${TABLE}
                WHERE last_updated < (CURRENT_TIMESTAMP - make_interval(days => :defaultPeriodInDays))
                LIMIT :batchSize
            )
            RETURNING id;
          """;

  @Override
  public String get() {
    return STATEMENT_PURGE_DEFAULT_PERIOD;
  }
}
