package de.gematik.demis.storage.reader.test;

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

import de.gematik.demis.storage.common.entity.BinaryEntity;
import de.gematik.demis.storage.common.entity.BundleEntity;
import de.gematik.demis.storage.common.entity.Tag;
import de.gematik.demis.storage.reader.api.ParameterNames;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;

public class TestData {

  public static final String BINARY_FHIR_JSON = "/testdata/binary.json";
  public static final String BUNDLE_DOCUMENT_FHIR_JSON = "/testdata/bundle-document.json";
  public static final String BUNDLE_DOCUMENT_CONTENT_DB =
      "/testdata/bundle-document-content-db.json";

  public static String readAsString(final String classpathResourceName) {
    try (final InputStream is = TestData.class.getResourceAsStream(classpathResourceName)) {
      if (is == null) {
        throw new IllegalStateException("missing resource file " + classpathResourceName);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    } catch (final IOException e) {
      throw new UncheckedIOException(
          "error reading classpath resource " + classpathResourceName, e);
    }
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
            .setSourceId("061f30ab559170b6c4db82ca25ef6daa")
            .setId(UUID.fromString("84d6cb09-924a-42c2-a786-0ec0d9271d4a"));
  }

  public static BundleEntity createBundleEntity() {
    return (BundleEntity)
        new BundleEntity()
            .setContent(readAsString(BUNDLE_DOCUMENT_CONTENT_DB))
            .setProfile(
                "https://demis.rki.de/fhir/StructureDefinition/NotificationBundleLaboratory")
            .setLastUpdated(
                OffsetDateTime.parse("2024-01-02T14:19:29.114+01:00")
                    .withOffsetSameInstant(ZoneOffset.UTC))
            .setResponsibleDepartment("1.01.0.53.")
            .setTags(createTags())
            .setSourceId("061f30ab559170b6c4db82ca25ef6daa")
            .setId(UUID.fromString("9e69e954-310a-4385-8365-ffc37bec8b4c"));
  }

  public static List<Tag> createTags() {
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

  /**
   * Creates and returns an instance of {@link HttpHeaders} with predefined headers. The headers
   * include a sender 1.01.0.53. and its authorization token.
   *
   * @return an instance of {@link HttpHeaders} with predefined headers.
   */
  public static HttpHeaders createHttpHeaders() {
    final HttpHeaders headers = new HttpHeaders();
    headers.add(ParameterNames.HEADER_SENDER, "1.01.0.53.");
    // token has roles pathogen-notification-fetcher, vaccine-injury-fetcher and
    // disease-notification-fetcher
    headers.add(
        HttpHeaders.AUTHORIZATION,
        "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6ImU2MmI2ZjI3MDc1ZjA2ZTA2NmI4ZTc0YTEyM2NjM2RmIn0.eyJleHAiOjE3MzIwOTM1MzcsImlhdCI6MTczMjA5MjkzNywianRpIjoiNTFmZTlhZTItNjUwNS00OGM2LWJiMmMtNGUzZWQ4OWFiNWFlIiwiaXNzIjoiaHR0cHM6Ly9hdXRoLmluZ3Jlc3MubG9jYWwvcmVhbG1zL09FR0QiLCJhdWQiOlsibm90aWZpY2F0aW9uLWNsZWFyaW5nLWFwaSIsImFjY291bnQiXSwic3ViIjoiZjlmMWE1ZTctOGFiNS00MzAwLThiMjMtYzhiM2ZmZjBjMzdiIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoiZGVtaXMtaW1wb3J0ZXIiLCJzaWQiOiI4YWJhMmZjOC1lMTUzLTRiM2MtODU2NS04MjA5Mjk0ODg0OTkiLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsicGF0aG9nZW4tbm90aWZpY2F0aW9uLWZldGNoZXIiLCJ2YWNjaW5lLWluanVyeS1mZXRjaGVyIiwiZGlzZWFzZS1ub3RpZmljYXRpb24tZmV0Y2hlciJdfSwicmVzb3VyY2VfYWNjZXNzIjp7Im5vdGlmaWNhdGlvbi1jbGVhcmluZy1hcGkiOnsicm9sZXMiOlsibGFiLW5vdGlmaWNhdGlvbi1yZWNlaXZlciJdfSwiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsIm1hbmFnZS1hY2NvdW50LWxpbmtzIiwidmlldy1wcm9maWxlIl19fSwic2NvcGUiOiJwcm9maWxlIiwib3JnYW5pemF0aW9uIjoiS3JlaXMgSGVyem9ndHVtIEdyb8OfLUzDvG5lYnVyZyIsInByZWZlcnJlZF91c2VybmFtZSI6IjEuMDEuMC41My4ifQ.X8rBojhqKh8Oxz-3WscEYvtfT1R0hRKHv1cN0xD6RyCortDMCaGW5FaX9kOYIRS9td7jkCqOpBiR2tL7DyyovYqZXHMBGUnJjPpfg1krezcSqDmpNc4eox4McjixCnTdnxiUpdYNLKuWOWsRV6-tz8cNXTbTT4eVeLAQ49fqfkLNDoKsWwEDSywj7WznL0p6C-azUlWHARO_SNtEQ0wQS9TdIjIYP7MMycE3yyI0ZJRwBotwwvO-xsiOLECKYZ8zT9qSkF5I12DvhqdlMhrprIimA21D1qZPo4BTZwhsDTzNzNJvPSlYS_pfiN6QCNKtK2-ps6BeA_HO6bcobqr36JsXZmOF2qMBEVsYxcEb76CI0Af8HQvCWwve_Sr-QKv-f3AfUV7FDGQSG_uluZCdmOwPRxuxmiwZtOA7bWIkOhELXu5bPboZ2otH3I0mug28xe-YVAp5SYBArMxijtgbHUCOcLLJ0l4_cRbTo8Yw99-Zr_RDhtxG4JyFBxtBRA2h");
    return headers;
  }
}
