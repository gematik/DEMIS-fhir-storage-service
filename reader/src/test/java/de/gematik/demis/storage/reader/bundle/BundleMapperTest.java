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

import static de.gematik.demis.storage.reader.test.TestData.readAsString;
import static de.gematik.demis.storage.reader.test.TestUtil.assertFhirResource;
import static org.assertj.core.api.Assertions.assertThat;

import ca.uhn.fhir.context.FhirContext;
import de.gematik.demis.storage.common.entity.BundleEntity;
import de.gematik.demis.storage.common.reader.BundleMapper;
import de.gematik.demis.storage.reader.test.TestData;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.Test;

class BundleMapperTest {

  private final BundleMapper underTest = new BundleMapper(FhirContext.forR4Cached());

  @Test
  void entityToResource() {
    final BundleEntity bundleEntity = TestData.createBundleEntity();
    final Bundle result = underTest.entityToResource(bundleEntity);

    assertThat(result).isNotNull();
    assertFhirResource(result, readAsString(TestData.BUNDLE_DOCUMENT_FHIR_JSON));
  }
}
