package de.gematik.demis.storage.purger.common.batches;

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
 * #L%
 */

import java.util.List;
import java.util.UUID;

/** Batch of a batch-based purge */
public interface Batch {

  /**
   * Get the name of the current batch.
   *
   * @return the name of the current batch
   */
  String name();

  /**
   * Complete the current batch.
   *
   * @param batch records of the current batch
   */
  void complete(List<UUID> batch);

  /**
   * Complete the current batch.
   *
   * @param batch records of the current batch
   * @param failures records that failed to be purged
   */
  void complete(List<UUID> batch, List<UUID> failures);

  /**
   * Terminates the whole purge operation right now.
   *
   * @param t cause of failure
   */
  void completeExceptionally(Throwable t);
}
