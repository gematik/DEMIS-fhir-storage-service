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

import static de.gematik.demis.storage.reader.api.ParameterNames.SUPPORTED_PARAMETERS;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "fss")
@Validated
@Builder
@Slf4j
public record FssReaderConfigProperties(
    @Bean @NotNull SearchProps search,
    Map<String, String> reader,
    @Bean @NotNull SecurityConfiguration security) {

  @PostConstruct
  void log() {
    log.info("FSS READER CONFIGURATION {}", this);
  }

  public record SearchProps(
      @Positive @Max(10000) int maxPageSize,
      @Positive int defaultPageSize,
      Set<String> ignoredParams) {

    public SearchProps {
      if (ignoredParams == null) {
        ignoredParams = new HashSet<>();
      } else {
        final List<String> ignoredButSupportedParams =
            ignoredParams.stream().filter(SUPPORTED_PARAMETERS::contains).toList();
        if (!ignoredButSupportedParams.isEmpty()) {
          throw new IllegalStateException(
              "The following parameters are supported and cannot be ignored: "
                  + ignoredButSupportedParams);
        }
      }
    }
  }

  public record SecurityConfiguration(@NotNull Map<String, String> roleProfileMapping) {}
}
