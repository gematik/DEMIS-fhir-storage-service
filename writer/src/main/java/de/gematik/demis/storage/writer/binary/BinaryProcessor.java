package de.gematik.demis.storage.writer.binary;

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

import static de.gematik.demis.storage.writer.error.ValidationError.checkNonNull;

import de.gematik.demis.storage.common.entity.BinaryEntity;
import de.gematik.demis.storage.writer.common.ResourceProcessor;
import de.gematik.demis.storage.writer.error.ValidationError;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.Binary;
import org.springframework.stereotype.Component;

@Component
public class BinaryProcessor implements ResourceProcessor<Binary, BinaryEntity> {

  @Override
  public Set<ValidationError> validate(final Binary binary) {
    return Stream.of(
            checkNonNull(binary.getContentType(), "ContentType"),
            checkNonNull(binary.getData(), "Data"))
        .filter(Objects::nonNull)
        .collect(Collectors.toUnmodifiableSet());
  }

  @Override
  public BinaryEntity createEntity(final Binary binary) {
    return new BinaryEntity().setContentType(binary.getContentType()).setData(binary.getData());
  }
}
