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
 * For additional notes and disclaimer from gematik and in case of changes by gematik,
 * find details in the "Readme" file.
 * #L%
 */

import java.util.function.Supplier;

/**
 * Interface for native SQL queries used in the purger. It provides a method to get the query string
 * with parameters of default period and batch size to be set.
 */
public interface PurgeQuery extends Supplier<String> {
  /**
   * Returns the query string with parameters of default period and batch size to be set.
   *
   * @return query
   */
  @Override
  String get();

  /**
   * Returns the name of the parameter for the default period in days.
   *
   * @return the name of the parameter for the default period in days
   */
  default String getDefaultPeriodParameterName() {
    return "defaultPeriodInDays";
  }

  /**
   * Returns the name of the parameter for the batch size.
   *
   * @return the name of the parameter for the batch size
   */
  default String getBatchSizeParameterName() {
    return "batchSize";
  }
}
