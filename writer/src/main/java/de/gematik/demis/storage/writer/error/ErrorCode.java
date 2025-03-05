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

import de.gematik.demis.service.base.error.ServiceException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum ErrorCode {
  FHIR_PARSE_ERROR(HttpStatus.BAD_REQUEST),
  RESOURCE_TYPE_NOT_SUPPORTED(HttpStatus.UNPROCESSABLE_ENTITY),
  BUNDLE_TYPE_NOT_SUPPORTED(HttpStatus.UNPROCESSABLE_ENTITY),
  VALIDATION_ERROR(HttpStatus.UNPROCESSABLE_ENTITY);

  private final HttpStatus httpStatus;

  public String getCode() {
    return name();
  }

  public ServiceException exception() {
    return new ServiceException(getHttpStatus(), getCode(), null);
  }

  public ServiceException exception(final String message) {
    return new ServiceException(getHttpStatus(), getCode(), message);
  }

  public ServiceException exception(final String message, final Throwable cause) {
    return new ServiceException(getHttpStatus(), getCode(), message, cause);
  }
}
