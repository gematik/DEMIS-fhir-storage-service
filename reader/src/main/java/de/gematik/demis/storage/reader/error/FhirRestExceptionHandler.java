package de.gematik.demis.storage.reader.error;

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

import static de.gematik.demis.storage.reader.api.ParameterNames.HEADER_SENDER;

import de.gematik.demis.service.base.error.ErrorCodeSupplier;
import de.gematik.demis.service.base.error.ServiceCallException;
import de.gematik.demis.service.base.error.ServiceException;
import de.gematik.demis.service.base.error.rest.api.ErrorDTO;
import de.gematik.demis.storage.reader.common.fhir.FhirConverter;
import jakarta.annotation.Nullable;
import jakarta.validation.ValidationException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

// TODO cp (angepasst) von NPS -> service-base
@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class FhirRestExceptionHandler extends ResponseEntityExceptionHandler {
  private final FhirConverter fhirConverter;

  private static boolean hasCause(final Exception ex, final Class<? extends Throwable> clazz) {
    Throwable cause = ex;
    while ((cause = cause.getCause()) != null && cause != ex) {
      if (clazz.isInstance(cause)) {
        return true;
      }
    }
    return false;
  }

  @ExceptionHandler(Exception.class)
  public final ResponseEntity<Object> handleServerException(
      final Exception ex, final WebRequest request) {
    final HttpStatus responseStatus;
    if (hasCause(ex, ConnectException.class)) {
      responseStatus = HttpStatus.SERVICE_UNAVAILABLE;
    } else if (hasCause(ex, SocketTimeoutException.class)) {
      responseStatus = HttpStatus.GATEWAY_TIMEOUT;
    } else {
      responseStatus = HttpStatus.INTERNAL_SERVER_ERROR;
    }
    return handleError(responseStatus, ex, request, null);
  }

  @ExceptionHandler(ServiceException.class)
  public final ResponseEntity<Object> handleServiceException(
      final ServiceException ex, final WebRequest request) {
    final HttpStatus responseStatus =
        Optional.ofNullable(ex.getResponseStatus()).orElse(HttpStatus.INTERNAL_SERVER_ERROR);
    return handleError(responseStatus, ex, request, ex.getMessage());
  }

  @ExceptionHandler(ValidationException.class)
  public final ResponseEntity<Object> handleClientException(
      final Exception ex, final WebRequest request) {
    return handleError(HttpStatus.BAD_REQUEST, ex, request, ex.getMessage());
  }

  @ExceptionHandler(ServiceCallException.class)
  public final ResponseEntity<Object> handleFeignException(
      final ServiceCallException ex, final WebRequest request) {
    final HttpStatus responseStatus =
        HttpStatus.Series.resolve(ex.getHttpStatus()) == HttpStatus.Series.SERVER_ERROR
            ? HttpStatus.BAD_GATEWAY
            : HttpStatus.INTERNAL_SERVER_ERROR;
    return handleError(responseStatus, ex, request, null);
  }

  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      final Exception ex,
      Object body,
      final HttpHeaders headers,
      final HttpStatusCode statusCode,
      final WebRequest request) {
    if (body == null && ex instanceof ErrorResponse errorResponse) {
      body = errorResponse.updateAndGetBody(getMessageSource(), LocaleContextHolder.getLocale());
    }
    final String detail =
        body instanceof ProblemDetail problemDetail ? problemDetail.getDetail() : null;
    return handleError(statusCode, ex, request, detail);
  }

  private ResponseEntity<Object> handleError(
      final HttpStatusCode statusCode,
      final Exception ex,
      final WebRequest request,
      final String detail) {
    final String errorCode =
        ex instanceof ErrorCodeSupplier errorCodeSupplier ? errorCodeSupplier.getErrorCode() : null;
    final ErrorDTO errorDTO = createErrorDTO(statusCode, request, errorCode, detail);

    final String sender = request.getHeader(HEADER_SENDER);
    logException(statusCode, ex, errorDTO, sender);

    return createResponseEntity(statusCode, request, errorDTO);
  }

  private ResponseEntity<Object> createResponseEntity(
      final HttpStatusCode statusCode, final WebRequest request, final ErrorDTO errorDTO) {
    final var result = createOperationOutcome(errorDTO);
    return fhirConverter.setResponseContent(ResponseEntity.status(statusCode), result, request);
  }

  private void logException(
      final HttpStatusCode statusCode,
      final Exception ex,
      final ErrorDTO errorDTO,
      final String sender) {
    if (statusCode.is5xxServerError()) {
      log.error("server error processing request: {} from sender {}", errorDTO, sender, ex);
    } else {
      log.warn(
          "invalid client request: {} from sender {} -> {}", errorDTO, sender, String.valueOf(ex));
    }
  }

  private ErrorDTO createErrorDTO(
      final HttpStatusCode statusCode,
      final WebRequest request,
      @Nullable final String errorCode,
      @Nullable final String detail) {
    final String uri =
        request instanceof ServletWebRequest servletWebRequest
            ? servletWebRequest.getRequest().getRequestURI()
            : null;
    return new ErrorDTO(
        UUID.randomUUID().toString(),
        statusCode.value(),
        LocalDateTime.now(),
        errorCode,
        detail,
        uri);
  }

  private OperationOutcome createOperationOutcome(final ErrorDTO errorDTO) {
    final OperationOutcome result = new OperationOutcome();
    result
        .addIssue()
        .setSeverity(OperationOutcome.IssueSeverity.ERROR)
        .setCode(
            errorDTO.status() >= 500
                ? OperationOutcome.IssueType.EXCEPTION
                : OperationOutcome.IssueType.PROCESSING)
        .setDiagnostics(errorDTO.detail())
        .setDetails(new CodeableConcept().addCoding(new Coding().setCode(errorDTO.errorCode())))
        .addLocation(errorDTO.id());
    return result;
  }
}
