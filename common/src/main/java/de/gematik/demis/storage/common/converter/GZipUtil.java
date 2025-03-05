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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import lombok.Generated;

public class GZipUtil {

  @Generated // just for sonar
  private GZipUtil() {
    throw new UnsupportedOperationException("no instances");
  }

  private static final Charset CHARSET = StandardCharsets.UTF_8;

  public static String decompressText(final byte[] compressedText) {
    return new String(decompress(compressedText), CHARSET);
  }

  public static byte[] compressText(final String text) {
    return compress(text.getBytes(CHARSET));
  }

  public static byte[] decompress(final byte[] compressedText) {
    try (final GZIPInputStream is = new GZIPInputStream(new ByteArrayInputStream(compressedText))) {
      return is.readAllBytes();
    } catch (IOException e) {
      throw new UncheckedIOException("error in decrompress", e);
    }
  }

  public static byte[] compress(final byte[] bytes) {
    try (final ByteArrayOutputStream os = new ByteArrayOutputStream();
        final GZIPOutputStream gos = new GZIPOutputStream(os)) {
      gos.write(bytes);
      gos.finish();
      return os.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException("error in compress", e);
    }
  }
}
