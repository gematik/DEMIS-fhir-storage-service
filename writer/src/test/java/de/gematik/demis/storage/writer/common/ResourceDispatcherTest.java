package de.gematik.demis.storage.writer.common;

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

import static de.gematik.demis.storage.writer.error.ErrorCode.RESOURCE_TYPE_NOT_SUPPORTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.when;

import de.gematik.demis.service.base.error.ServiceException;
import de.gematik.demis.storage.common.entity.BinaryEntity;
import de.gematik.demis.storage.common.entity.BundleEntity;
import de.gematik.demis.storage.writer.binary.BinaryProcessor;
import de.gematik.demis.storage.writer.bundle.BundleProcessor;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResourceDispatcherTest {

  @Mock BinaryProcessor binaryProcessor;
  @Mock BundleProcessor bundleProcessor;
  @InjectMocks ResourceDispatcher underTest;

  @Test
  void bundle() {
    final var bundle = new Bundle();
    final var entity = new BundleEntity();
    when(bundleProcessor.createEntity(bundle)).thenReturn(entity);

    final var actual = underTest.dispatchResourceAction(bundle, ResourceProcessor::createEntity);

    assertThat(actual).isEqualTo(entity);
    Mockito.verifyNoInteractions(binaryProcessor);
  }

  @Test
  void binary() {
    final var binary = new Binary();
    final var entity = new BinaryEntity();
    when(binaryProcessor.createEntity(binary)).thenReturn(entity);

    final var actual = underTest.dispatchResourceAction(binary, ResourceProcessor::createEntity);

    assertThat(actual).isEqualTo(entity);
    Mockito.verifyNoInteractions(bundleProcessor);
  }

  @Test
  void resourceTypeNotSupported() {
    final var patient = new Patient();
    final ServiceException exception =
        catchThrowableOfType(
            () -> underTest.dispatchResourceAction(patient, ResourceProcessor::createEntity),
            ServiceException.class);

    assertThat(exception)
        .isNotNull()
        .returns(RESOURCE_TYPE_NOT_SUPPORTED.getCode(), ServiceException::getErrorCode);
  }
}
