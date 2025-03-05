package de.gematik.demis.storage.writer.error;

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

import static de.gematik.demis.storage.writer.error.ValidationError.Type.MISSING_ELEMENT;

import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public record ValidationError(Type type, String message) {
  @RequiredArgsConstructor
  @Getter
  public enum Type {
    MISSING_ELEMENT("element '%s' is null but mandatory");
    private final String messageTemplate;
  }

  @Override
  public String toString() {
    return String.format(type().getMessageTemplate(), message());
  }

  @Nullable
  public static ValidationError checkNonNull(final Object value, final String element) {
    return value == null ? new ValidationError(MISSING_ELEMENT, element) : null;
  }
}
