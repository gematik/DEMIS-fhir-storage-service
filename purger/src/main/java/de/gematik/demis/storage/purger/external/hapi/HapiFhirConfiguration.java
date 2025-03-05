package de.gematik.demis.storage.purger.external.hapi;

/*-
 * #%L
 * fhir-storage-purger
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

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IRestfulClientFactory;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import java.time.Duration;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class HapiFhirConfiguration {

  private final Duration clientSocketTimeout;
  @Getter private final boolean clientLogRequests;

  public HapiFhirConfiguration(
      @Value("${fss.hapi.client.socket-timeout}") Duration clientSocketTimeout,
      @Value("${fss.hapi.client.log-requests}") boolean clientLogRequests) {
    this.clientSocketTimeout = clientSocketTimeout;
    this.clientLogRequests = clientLogRequests;
  }

  @Bean
  public FhirContext fhirContext() {
    log.debug("Creating FHIR R4 context");
    final var fhirContext = FhirContext.forR4Cached();
    configureClients(fhirContext);
    return fhirContext;
  }

  private void configureClients(FhirContext fhirContext) {
    final IRestfulClientFactory clients = fhirContext.getRestfulClientFactory();
    final ServerValidationModeEnum validationMode = ServerValidationModeEnum.NEVER;
    clients.setServerValidationMode(validationMode);
    final int millis = Math.toIntExact(clientSocketTimeout.toMillis());
    log.debug(
        "Setting FHIR client configuration. SocketTimeoutMillis: {} ServerValidationMode: {} LogRequests: {}",
        millis,
        validationMode,
        clientLogRequests);
    clients.setSocketTimeout(millis);
  }
}
