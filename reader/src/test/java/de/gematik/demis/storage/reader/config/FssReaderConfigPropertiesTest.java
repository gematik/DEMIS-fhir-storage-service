package de.gematik.demis.storage.reader.config;

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
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 * #L%
 */

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchException;
import static org.assertj.core.util.Throwables.getRootCause;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;

class FssReaderConfigPropertiesTest {

  @Test
  void invalidIgnoredParametersThrowsException() {
    final var app = new SpringApplication(ReaderPropertiesTestConfiguration.class);
    app.setWebApplicationType(WebApplicationType.NONE);

    final String parameter = "--fss.search.ignored-params=_summary,_tag,_profile,_format,_elements";

    final Exception exception = catchException(() -> app.run(parameter));

    assertThat(exception).isNotNull().isInstanceOf(ConfigurationPropertiesBindException.class);
    assertThat(getRootCause(exception))
        .isNotNull()
        .isInstanceOf(IllegalStateException.class)
        .hasMessage(
            "The following parameters are supported and cannot be ignored: [_tag, _profile, _format]");
  }

  @TestConfiguration
  @EnableConfigurationProperties(FssReaderConfigProperties.class)
  static class ReaderPropertiesTestConfiguration {}
}
