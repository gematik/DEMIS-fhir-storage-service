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

import de.gematik.demis.storage.common.entity.AbstractResourceEntity;
import de.gematik.demis.storage.writer.binary.BinaryProcessor;
import de.gematik.demis.storage.writer.bundle.BundleProcessor;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResourceDispatcher {

  private final BinaryProcessor binaryProcessor;
  private final BundleProcessor bundleProcessor;

  public <T> T dispatchResourceAction(
      final IBaseResource resource, final ResourceAction<T> resourceAction) {
    return switch (resource) {
      case Binary binary -> resourceAction.apply(binaryProcessor, binary);
      case Bundle bundle -> resourceAction.apply(bundleProcessor, bundle);
      default ->
          throw RESOURCE_TYPE_NOT_SUPPORTED.exception(
              "resource type not supported: " + resource.getClass().getSimpleName());
    };
  }

  @FunctionalInterface
  public interface ResourceAction<T> {
    <R extends IBaseResource, E extends AbstractResourceEntity> T apply(
        ResourceProcessor<R, E> processor, R resource);
  }
}
