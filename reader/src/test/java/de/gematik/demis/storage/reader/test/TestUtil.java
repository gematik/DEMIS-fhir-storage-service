package de.gematik.demis.storage.reader.test;

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

import static org.assertj.core.api.Assertions.assertThat;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.instance.model.api.IBaseResource;

public class TestUtil {
  public static void assertFhirResource(
      final IBaseResource theResource, final String expectedFhirResourceAsJsonString) {
    assertThat(fhirResourceToJson(theResource))
        .isEqualToIgnoringWhitespace(expectedFhirResourceAsJsonString);
  }

  public static String fhirResourceToJson(final IBaseResource theResource) {
    if (theResource == null) {
      return null;
    }

    return getJsonParser().setPrettyPrint(true).encodeResourceToString(theResource);
  }

  public static IParser getJsonParser() {
    return FhirContext.forR4Cached().newJsonParser();
  }
}
