package de.gematik.demis.storage.common.converter;

/*-
 * #%L
 * fhir-storage-common
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

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class CompressStringConverterTest {

  private final CompressStringConverter underTest = new CompressStringConverter();

  @Test
  void convert() {
    final String text =
        "Irgendein Text, der komprimiert werden soll und dadurch weniger bytes in Anspruch nehmen sollte. eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee";

    final byte[] compressedBytes = underTest.convertToDatabaseColumn(text);
    assertThat(compressedBytes)
        .isNotNull()
        .hasSizeLessThan(text.getBytes(StandardCharsets.UTF_8).length);

    final String actual = underTest.convertToEntityAttribute(compressedBytes);
    assertThat(actual).isEqualTo(text);
  }
}
