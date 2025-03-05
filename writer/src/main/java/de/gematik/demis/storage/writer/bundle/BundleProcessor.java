package de.gematik.demis.storage.writer.bundle;

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

import static de.gematik.demis.storage.writer.error.ErrorCode.BUNDLE_TYPE_NOT_SUPPORTED;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import de.gematik.demis.storage.common.entity.BundleEntity;
import de.gematik.demis.storage.writer.common.ResourceProcessor;
import de.gematik.demis.storage.writer.error.ValidationError;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BundleProcessor implements ResourceProcessor<Bundle, BundleEntity> {

  private static final List<String> EXCLUDED_ELEMENTS = List.of("Bundle.meta");

  private final FhirContext fhirContext;

  @Override
  public Set<ValidationError> validate(final Bundle bundle) {
    final Bundle.BundleType type;
    if ((type = bundle.getType()) != Bundle.BundleType.DOCUMENT) {
      throw BUNDLE_TYPE_NOT_SUPPORTED.exception("bundle type not supported: " + type);
    }

    // there are no validation constraints for bundle
    return Set.of();
  }

  @Override
  public BundleEntity createEntity(final Bundle bundle) {
    return new BundleEntity()
        .setProfile(getProfile(bundle.getMeta()))
        .setNotificationBundleId(getNotificationBundleId(bundle))
        .setNotificationId(getNotificationId(bundle))
        .setContent(getContent(bundle));
  }

  private String getNotificationBundleId(final Bundle bundle) {
    return bundle.getIdentifier().getValue();
  }

  private String getNotificationId(final Bundle bundle) {
    final Resource resource = bundle.getEntryFirstRep().getResource();
    return resource instanceof Composition composition
        ? composition.getIdentifier().getValue()
        : null;
  }

  @Nullable
  private String getProfile(final Meta meta) {
    if (meta.hasProfile()) {
      if (meta.getProfile().size() > 1) {
        log.warn(
            "multiple profiles - using only first one and ignoring others. All profiles = {}",
            meta.getProfile());
      }
      return meta.getProfile().getFirst().getValue();
    } else {
      return null;
    }
  }

  private String getContent(final Bundle bundle) {
    final IParser parser = fhirContext.newJsonParser();
    parser.setDontEncodeElements(EXCLUDED_ELEMENTS);
    return parser.encodeResourceToString(bundle);
  }
}
