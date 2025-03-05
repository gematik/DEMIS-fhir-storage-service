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
import static de.gematik.demis.storage.writer.test.LogUtil.getFirstLogOfLevel;
import static de.gematik.demis.storage.writer.test.LogUtil.listenToLog;
import static de.gematik.demis.storage.writer.test.TestData.BUNDLE_DOCUMENT_CONTENT_DB;
import static de.gematik.demis.storage.writer.test.TestData.BUNDLE_DOCUMENT_FHIR_JSON;
import static de.gematik.demis.storage.writer.test.TestData.jsonToResource;
import static de.gematik.demis.storage.writer.test.TestData.readResourceAsString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.hl7.fhir.r4.model.Bundle.BundleType.DOCUMENT;
import static org.hl7.fhir.r4.model.Bundle.BundleType.TRANSACTION;

import ca.uhn.fhir.context.FhirContext;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import de.gematik.demis.service.base.error.ServiceException;
import de.gematik.demis.storage.common.entity.BundleEntity;
import de.gematik.demis.storage.writer.error.ValidationError;
import java.util.Optional;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BundleProcessorTest {

  private final FhirContext fhirContext = FhirContext.forR4Cached();

  private BundleProcessor underTest;

  @BeforeEach
  void setup() {
    underTest = new BundleProcessor(fhirContext);
  }

  @Test
  void invalidBundleType() {
    final Bundle bundle = new Bundle().setType(TRANSACTION);
    final ServiceException exception =
        catchThrowableOfType(() -> underTest.validate(bundle), ServiceException.class);

    assertThat(exception)
        .isNotNull()
        .returns(BUNDLE_TYPE_NOT_SUPPORTED.getCode(), ServiceException::getErrorCode);
  }

  @Test
  void valid() {
    final Bundle bundle = new Bundle().setType(DOCUMENT);
    final Set<ValidationError> result = underTest.validate(bundle);
    assertThat(result).isNotNull().isEmpty();
  }

  @Test
  void createEntity() {
    final var bundle =
        jsonToResource(Bundle.class, readResourceAsString(BUNDLE_DOCUMENT_FHIR_JSON));

    final BundleEntity entity = underTest.createEntity(bundle);

    assertThat(entity)
        .isNotNull()
        .returns(
            "https://demis.rki.de/fhir/StructureDefinition/NotificationBundleLaboratory",
            BundleEntity::getProfile);
    assertThat(entity.getContent())
        .isEqualToIgnoringWhitespace(readResourceAsString(BUNDLE_DOCUMENT_CONTENT_DB));
  }

  @Test
  void multipleProfiles() {
    final String firstProfile = "https://demis.rki.de/fhir/StructureDefinition/First";
    final Bundle bundle = new Bundle();
    bundle
        .getMeta()
        .addProfile(firstProfile)
        .addProfile("https://demis.rki.de/fhir/StructureDefinition/Second");

    final var logEvents = listenToLog(BundleProcessor.class);
    final BundleEntity entity = underTest.createEntity(bundle);

    assertThat(entity.getProfile()).isEqualTo(firstProfile);

    final Optional<ILoggingEvent> log = getFirstLogOfLevel(logEvents, Level.WARN);
    Assertions.assertThat(log)
        .isPresent()
        .get()
        .extracting(ILoggingEvent::getFormattedMessage)
        .asString()
        .isEqualTo(
            "multiple profiles - using only first one and ignoring others. All profiles = [CanonicalType[https://demis.rki.de/fhir/StructureDefinition/First], CanonicalType[https://demis.rki.de/fhir/StructureDefinition/Second]]");
  }
}
