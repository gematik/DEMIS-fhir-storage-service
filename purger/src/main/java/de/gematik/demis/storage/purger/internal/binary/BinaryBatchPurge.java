package de.gematik.demis.storage.purger.internal.binary;

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

import de.gematik.demis.storage.purger.common.batches.Batch;
import de.gematik.demis.storage.purger.common.batches.BatchPurge;
import de.gematik.demis.storage.purger.common.batches.BatchesConfiguration;
import de.gematik.demis.storage.purger.internal.InternalPurge;
import org.springframework.stereotype.Service;

@Service
class BinaryBatchPurge extends BatchPurge implements InternalPurge {

  private final BinaryBatchPurgeRepository repository;

  BinaryBatchPurge(
      BatchesConfiguration batchesConfiguration, BinaryBatchPurgeRepository repository) {
    super(batchesConfiguration);
    this.repository = repository;
  }

  @Override
  protected void purgeBatch(Batch batch) {
    batch.complete(repository.purgeExpiredRecords());
  }

  @Override
  public String name() {
    return "binary";
  }
}
