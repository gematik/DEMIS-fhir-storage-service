package de.gematik.demis.storage.reader.common.security;

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

import de.gematik.demis.service.base.security.jwt.Token;
import de.gematik.demis.service.base.security.jwt.TokenFactory;
import de.gematik.demis.storage.reader.config.FssReaderConfigProperties.SecurityConfiguration;
import de.gematik.demis.storage.reader.error.ErrorCode;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthorizationService {

  private final SecurityConfiguration securityConfiguration;

  public Caller getCaller(final HttpHeaders headers) {
    final Token token = tokenFromHeaders(headers);
    if (token == null) {
      return new Caller(null, Set::of);
    }
    return new Caller(token.preferredUsername(), () -> getAuthorizedFhirProfiles(token));
  }

  /**
   * Retrieves the set of authorized FHIR profiles for a given token.
   *
   * <p>This method extracts roles from the provided token, maps them to FHIR profiles based on the
   * role-profile mappings defined in the security configuration, and filters out any null entries.
   *
   * @param token the {@link Token} containing roles that will be mapped to FHIR profiles.
   * @return a {@link Set} of authorized FHIR profiles derived from the token's roles.
   */
  private Set<String> getAuthorizedFhirProfiles(final Token token) {
    final List<String> roles = token.roles();

    return roles.stream()
        .map(securityConfiguration.roleProfileMapping()::get)
        .filter(Objects::nonNull)
        .collect(Collectors.toUnmodifiableSet());
  }

  private Token tokenFromHeaders(final HttpHeaders headers) {
    try {
      return new TokenFactory(headers).get();
    } catch (IllegalArgumentException e) {
      throw ErrorCode.FORBIDDEN.exception("Invalid Authorization header");
    }
  }
}
