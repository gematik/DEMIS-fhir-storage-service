package de.gematik.demis.storage.reader.common;

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

import static org.springframework.http.ResponseEntity.ok;

import de.gematik.demis.storage.reader.api.ResourceEndpoint;
import de.gematik.demis.storage.reader.common.fhir.FhirConverter;
import de.gematik.demis.storage.reader.common.security.AuthorizationService;
import de.gematik.demis.storage.reader.common.security.Caller;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.context.request.WebRequest;

@RequiredArgsConstructor
public abstract class ReadController implements ResourceEndpoint {

  private final ReadService<?, ?, ?> service;
  private final AuthorizationService authorizationService;
  private final FhirConverter fhirConverter;

  @Override
  public ResponseEntity<Object> search(
      final HttpHeaders headers,
      final MultiValueMap<String, String> requestParams,
      final WebRequest request) {
    final Caller caller = authorizationService.getCaller(headers);
    final Bundle result = service.search(caller, requestParams);
    return fhirConverter.setResponseContent(ok(), result, request);
  }

  @Override
  public ResponseEntity<Object> findById(
      @RequestHeader final HttpHeaders headers, final UUID id, final WebRequest request) {
    final Caller caller = authorizationService.getCaller(headers);
    final Resource result = service.findById(caller, id);
    return fhirConverter.setResponseContent(ok(), result, request);
  }
}
