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

import static org.springframework.core.ResolvableType.forInstance;

import de.gematik.demis.storage.common.entity.AbstractResourceEntity;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.core.ResolvableType;
import org.springframework.core.ResolvableTypeProvider;

public record ResourceSavedEvent<R extends IBaseResource, E extends AbstractResourceEntity>(
    R resource, E entity) implements ResolvableTypeProvider {

  @Override
  public ResolvableType getResolvableType() {
    return ResolvableType.forClassWithGenerics(
        getClass(), forInstance(resource), forInstance(entity));
  }

  @Override
  public String toString() {
    return "ResourceSavedEvent{"
        + "resourceType="
        + resource.getClass().getSimpleName()
        + ", entityId="
        + entity.getId()
        + '}';
  }
}
