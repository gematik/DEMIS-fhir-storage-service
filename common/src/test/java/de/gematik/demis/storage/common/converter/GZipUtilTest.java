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

/**
 * This Test ensures that the compressing algorithmus is not changed. Otherwise, the data in the
 * database has to be migrated.
 */
class GZipUtilTest {

  private static final String TEXT = "hello world. 123";
  private static final byte[] TEXT_AS_BYTES = TEXT.getBytes(StandardCharsets.UTF_8);
  private static final byte[] COMPRESSED =
      new byte[] {
        31, -117, 8, 0, 0, 0, 0, 0, 0, -1, -53, 72, -51, -55, -55, 87, 40, -49, 47, -54, 73, -47,
        83, 48, 52, 50, 6, 0, -59, 10, -24, 54, 16, 0, 0, 0
      };

  @Test
  void decompressText() {
    assertThat(GZipUtil.decompressText(COMPRESSED)).isEqualTo(TEXT);
  }

  @Test
  void compressText() {
    assertThat(GZipUtil.compressText(TEXT)).isEqualTo(COMPRESSED);
  }

  @Test
  void decompress() {
    assertThat(GZipUtil.decompress(COMPRESSED)).isEqualTo(TEXT_AS_BYTES);
  }

  @Test
  void compress() {
    assertThat(GZipUtil.compress(TEXT_AS_BYTES)).isEqualTo(COMPRESSED);
  }
}
