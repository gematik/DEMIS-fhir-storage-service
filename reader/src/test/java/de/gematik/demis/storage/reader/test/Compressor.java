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

import de.gematik.demis.storage.common.converter.CompressStringConverter;
import org.apache.commons.codec.binary.Hex;

/**
 * In the sql scripts we fill the database with bundles. The content column is a byte[] which
 * contains a compressed zip. The decompressed byte[] must be a valid bundle json string. With this
 * simple class you can generate for a given string a compressed byte[]. Furthermore, you must
 * encode to hex string to use in the sql with E'\\x
 */
public class Compressor {

  public static void main(String[] args) {
    final String text =
        """
{"resourceType":"Bundle","type":"document"}
""";

    final byte[] compressedBytes = new CompressStringConverter().convertToDatabaseColumn(text);
    final String hexEncoded = Hex.encodeHexString(compressedBytes);
    System.out.println(hexEncoded);
  }
}
