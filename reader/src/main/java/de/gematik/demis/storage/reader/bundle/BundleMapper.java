package de.gematik.demis.storage.reader.bundle;

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
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 * #L%
 */

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import de.gematik.demis.storage.common.entity.BundleEntity;
import de.gematik.demis.storage.reader.common.EntityResourceMapper;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class BundleMapper extends EntityResourceMapper<BundleEntity, Bundle> {

  private final FhirContext fhirContext;

  @Override
  public Bundle entityToResource(final BundleEntity entity) {
    final IParser parser = fhirContext.newJsonParser();
    final Bundle bundle = parser.parseResource(Bundle.class, entity.getContent());
    bundle.getMeta().addProfile(entity.getProfile());
    setCommonAttributes(bundle, entity);
    return bundle;
  }
}
