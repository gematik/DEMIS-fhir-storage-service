package de.gematik.demis.storage.common.entity;

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

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ResourceEntityTest {

  @Test
  void bundleToResourceId() {
    final UUID id = UUID.fromString("a946147a-495c-4608-9c41-0b208c78454c");
    final var entity = new BundleEntity().setId(id);
    final String result = entity.toResourceId();
    assertThat(result).isEqualTo("Bundle/a946147a-495c-4608-9c41-0b208c78454c");
  }

  @Test
  void binaryToResourceId() {
    final UUID id = UUID.fromString("b946147a-495c-4608-9c41-0b208c78454d");
    final var entity = new BinaryEntity().setId(id);
    final String result = entity.toResourceId();
    assertThat(result).isEqualTo("Binary/b946147a-495c-4608-9c41-0b208c78454d");
  }
}
