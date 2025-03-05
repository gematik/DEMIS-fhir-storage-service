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

import static de.gematik.demis.storage.writer.error.ErrorCode.FHIR_PARSE_ERROR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import ca.uhn.fhir.context.FhirContext;
import de.gematik.demis.service.base.error.ServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WriteServiceTest {

  FhirContext fhirContext = FhirContext.forR4Cached();
  @Mock ResourceRepository repository;
  @Mock CommonFieldMapper commonFieldMapper;
  @Mock ResourceDispatcher resourceDispatcher;

  @InjectMocks WriteService underTest;

  @Test
  void invalidFhirInput() {
    final ServiceException exception =
        catchThrowableOfType(() -> underTest.store("{}"), ServiceException.class);

    assertThat(exception)
        .isNotNull()
        .returns(FHIR_PARSE_ERROR.getCode(), ServiceException::getErrorCode);
  }
}
