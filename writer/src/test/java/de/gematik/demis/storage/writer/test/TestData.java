package de.gematik.demis.storage.writer.test;

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

import ca.uhn.fhir.context.FhirContext;
import de.gematik.demis.storage.common.entity.BinaryEntity;
import de.gematik.demis.storage.common.entity.BundleEntity;
import de.gematik.demis.storage.common.entity.Tag;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Resource;

public class TestData {

  public static final String BINARY_FHIR_JSON = "/testdata/binary.json";
  public static final String BUNDLE_DOCUMENT_FHIR_JSON = "/testdata/bundle-document.json";
  public static final String BUNDLE_DOCUMENT_CONTENT_DB =
      "/testdata/bundle-document-content-db.json";

  public static String readResourceAsString(final String resourceName) {
    try (final InputStream is = TestData.class.getResourceAsStream(resourceName)) {
      if (is == null) {
        throw new IllegalStateException("missing resource file " + resourceName);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    } catch (final IOException e) {
      throw new UncheckedIOException("error reading classpath resource " + resourceName, e);
    }
  }

  public static String resourceToJsonString(final Resource resource) {
    return FhirContext.forR4Cached().newJsonParser().encodeResourceToString(resource);
  }

  public static <T extends IBaseResource> T jsonToResource(
      final Class<T> clazz, final String json) {
    return FhirContext.forR4Cached().newJsonParser().parseResource(clazz, json);
  }

  public static BinaryEntity createBinaryEntity() {
    return (BinaryEntity)
        new BinaryEntity()
            .setContentType("application/cms")
            .setData(Base64.getDecoder().decode("xxxyyyzzz123".getBytes(StandardCharsets.UTF_8)))
            .setLastUpdated(
                OffsetDateTime.parse("2024-01-02T14:19:29.114+01:00")
                    .withOffsetSameInstant(ZoneOffset.UTC))
            .setResponsibleDepartment("1.01.0.53.")
            .setTags(createTags())
            .setSourceId("061f30ab559170b6c4db82ca25ef6daa");
  }

  public static BundleEntity createBundleEntity() {
    return (BundleEntity)
        new BundleEntity()
            .setContent(readResourceAsString(BUNDLE_DOCUMENT_CONTENT_DB))
            .setNotificationBundleId("fee6005e-5686-4b7b-b6ee-98b0e98a9d42")
            .setNotificationId("e8d8cc43-32c2-4f93-8eaf-b2f3e6deb2a9")
            .setProfile(
                "https://demis.rki.de/fhir/StructureDefinition/NotificationBundleLaboratory")
            .setLastUpdated(
                OffsetDateTime.parse("2024-01-02T14:19:29.114+01:00")
                    .withOffsetSameInstant(ZoneOffset.UTC))
            .setResponsibleDepartment("1.01.0.53.")
            .setTags(createTags())
            .setSourceId("061f30ab559170b6c4db82ca25ef6daa");
  }

  private static List<Tag> createTags() {
    return List.of(
        new Tag()
            .setSystem("https://demis.rki.de/fhir/CodeSystem/ResponsibleDepartment")
            .setCode("1.01.0.53."),
        new Tag()
            .setSystem("https://demis.rki.de/fhir/CodeSystem/ResponsibleDepartmentNotifier")
            .setCode("1.11.0.11.01."),
        new Tag()
            .setSystem("https://demis.rki.de/fhir/CodeSystem/RelatedNotificationBundle")
            .setCode("1a3a16aa-64e0-5eb1-8601-018fc3794b6e")
            .setDisplay(
                "Relates to message with identifier: 1a3a16aa-64e0-5eb1-8601-018fc3794b6e"));
  }
}
