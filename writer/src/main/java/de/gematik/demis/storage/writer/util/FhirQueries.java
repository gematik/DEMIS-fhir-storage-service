package de.gematik.demis.storage.writer.util;

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

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.Generated;
import org.hl7.fhir.instance.model.api.IBaseCoding;

public class FhirQueries {

  @Generated // for sonar
  private FhirQueries() {
    throw new UnsupportedOperationException("no instances");
  }

  public static <T extends IBaseCoding> Optional<T> findFirstTagOfSystem(
      final List<T> tag, final String system) {
    return tag.stream().filter(c -> Objects.equals(c.getSystem(), system)).findFirst();
  }
}
