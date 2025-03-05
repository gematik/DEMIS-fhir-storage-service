package de.gematik.demis.storage.reader.bundle;

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

import de.gematik.demis.storage.reader.api.BundleEndpoint;
import de.gematik.demis.storage.reader.common.ReadController;
import de.gematik.demis.storage.reader.common.fhir.FhirConverter;
import de.gematik.demis.storage.reader.common.security.AuthorizationService;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BundleReadController extends ReadController implements BundleEndpoint {

  public BundleReadController(
      final BundleReadService service,
      final AuthorizationService authorizationService,
      final FhirConverter fhirConverter) {
    super(service, authorizationService, fhirConverter);
  }
}
