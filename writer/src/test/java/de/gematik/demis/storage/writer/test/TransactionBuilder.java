package de.gematik.demis.storage.writer.test;

/*-
 * #%L
 * fhir-storage-writer
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

import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Resource;

public class TransactionBuilder {

  private final List<Resource> entries = new ArrayList<>();

  public TransactionBuilder addResource(final Resource resource) {
    entries.add(resource);
    return this;
  }

  public Bundle build() {
    final Bundle transactionBundle = new Bundle();
    transactionBundle.setType(Bundle.BundleType.TRANSACTION);

    for (final Resource resource : entries) {
      transactionBundle
          .addEntry()
          .setFullUrl(IdType.newRandomUuid().getValue())
          .setResource(resource)
          .getRequest()
          .setUrl("IsIgnored")
          .setMethod(Bundle.HTTPVerb.POST);
    }

    return transactionBundle;
  }
}
