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

import static de.gematik.demis.storage.writer.error.ValidationError.Type.MISSING_ELEMENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import de.gematik.demis.storage.common.entity.BinaryEntity;
import de.gematik.demis.storage.writer.error.ValidationError;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.hl7.fhir.r4.model.Binary;
import org.junit.jupiter.api.Test;

class BinaryProcessorTest {

  private static final String CONTENT_TYPE = "application/test";
  private static final byte[] CONTENT = "Dies ist ein Test".getBytes(StandardCharsets.UTF_8);
  private static final Binary BINARY = new Binary().setData(CONTENT).setContentType(CONTENT_TYPE);

  private final BinaryProcessor underTest = new BinaryProcessor();

  @Test
  void validationError() {
    final Set<ValidationError> result = underTest.validate(new Binary());
    assertThat(result)
        .containsExactlyInAnyOrder(
            new ValidationError(MISSING_ELEMENT, "ContentType"),
            new ValidationError(MISSING_ELEMENT, "Data"));
  }

  @Test
  void valid() {
    final Set<ValidationError> result = underTest.validate(BINARY);
    assertThat(result).isNotNull().isEmpty();
  }

  @Test
  void createEntity() {
    final BinaryEntity entity = underTest.createEntity(BINARY);

    assertThat(entity)
        .isNotNull()
        .returns(CONTENT_TYPE, BinaryEntity::getContentType)
        .returns(CONTENT, BinaryEntity::getData);
  }
}
