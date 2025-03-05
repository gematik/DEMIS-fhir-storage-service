package de.gematik.demis.storage.reader.bundle;

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

import static de.gematik.demis.storage.common.fhir.DemisFhirNames.NOTIFICATION_BUNDLE_ID_NAMING_SYSTEM;
import static de.gematik.demis.storage.reader.api.ParameterNames.PARAM_IDENTIFIER;
import static de.gematik.demis.storage.reader.api.ParameterNames.PARAM_PROFILE;

import ca.uhn.fhir.rest.param.TokenParam;
import de.gematik.demis.service.base.error.ServiceException;
import de.gematik.demis.storage.common.entity.BundleEntity;
import de.gematik.demis.storage.common.reader.BundleMapper;
import de.gematik.demis.storage.reader.common.ReadService;
import de.gematik.demis.storage.reader.common.search.RequestParamFilterResolver;
import de.gematik.demis.storage.reader.common.search.SearchSetService;
import de.gematik.demis.storage.reader.common.security.Caller;
import de.gematik.demis.storage.reader.config.FssReaderConfigProperties.SearchProps;
import de.gematik.demis.storage.reader.error.ErrorCode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BundleReadService extends ReadService<BundleEntity, Bundle, BundleFilter> {
  private static final Map<String, BiConsumer<List<String>, BundleFilter>> BUNDLE_FILTER_PARAMETER =
      Map.of(
          PARAM_PROFILE, BundleReadService::requestParameterProfile,
          PARAM_IDENTIFIER, BundleReadService::requestParameterIdentifier);

  public BundleReadService(
      final BundleReadonlyRepository repository,
      final BundleMapper mapper,
      final SearchProps searchProps,
      final SearchSetService searchSetService) {
    super(
        repository,
        mapper,
        new RequestParamFilterResolver<>(BundleFilter::new, BUNDLE_FILTER_PARAMETER),
        searchProps,
        searchSetService);
  }

  private static void requestParameterProfile(
      final List<String> values, final BundleFilter filter) {
    filter.setProfiles(Set.copyOf(values));
  }

  private static void requestParameterIdentifier(
      final List<String> values, final BundleFilter filter) {
    if (values.size() > 1) {
      throw ErrorCode.INVALID_FILTER.exception("at most one identifier");
    }

    final TokenParam tokenParam = new TokenParam();
    tokenParam.setValueAsQueryToken(null, null, null, values.getFirst());

    if (StringUtils.isEmpty(tokenParam.getSystem())
        || NOTIFICATION_BUNDLE_ID_NAMING_SYSTEM.equals(tokenParam.getSystem())) {
      filter.setNotificationBundleId(tokenParam.getValue());
    } else {
      throw ErrorCode.INVALID_FILTER.exception(
          "identifier system not supported: " + tokenParam.getSystem());
    }
  }

  @Override
  protected void checkResourcePermission(final Caller caller, final BundleEntity entity) {
    if (entity.getProfile() != null && !caller.getProfiles().contains(entity.getProfile())) {
      throw ErrorCode.FORBIDDEN.exception(
          String.format("%s is not allowed to access resource", caller.getName()));
    }
  }

  @Override
  protected void enforceFilter(final Caller caller, final BundleFilter filter) {
    final Set<String> filterProfiles = filter.getProfiles();
    if (filterProfiles == null || filterProfiles.isEmpty()) {
      filter.setProfiles(caller.getProfiles());
    }
  }

  /**
   * Ensures that the requested FHIR profiles are authorized based on the allowed profiles.
   *
   * @param caller containing the set of profiles that are allowed
   * @param filter containing the set of profiles that are being requested
   * @throws ServiceException with HttpStatus 403 if any of the requested profiles are not in the
   *     allowed profiles or if no profile is filtered or allowed
   */
  @Override
  protected void checkFilterPermission(final Caller caller, final BundleFilter filter) {
    final Set<String> allowedProfiles = caller.getProfiles();
    final Set<String> requestedProfiles = filter.getProfiles();
    if (allowedProfiles.isEmpty() || requestedProfiles == null || requestedProfiles.isEmpty()) {
      throw ErrorCode.FORBIDDEN.exception("user is not authorized for any profile");
    }

    final List<String> unauthorizedProfiles =
        requestedProfiles.stream().filter(profile -> !allowedProfiles.contains(profile)).toList();

    if (!unauthorizedProfiles.isEmpty()) {
      throw ErrorCode.FORBIDDEN.exception(
          String.format(
              "Unauthorized profiles detected: %s", String.join(", ", unauthorizedProfiles)));
    }
  }

  @Override
  protected String getResourceType() {
    return "Bundle";
  }

  @Override
  protected String getBusinessId(final Bundle bundle) {
    return bundle.getIdentifier().getValue();
  }
}
