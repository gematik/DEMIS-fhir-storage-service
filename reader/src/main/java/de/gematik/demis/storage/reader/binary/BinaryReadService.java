package de.gematik.demis.storage.reader.binary;

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

import static de.gematik.demis.storage.common.fhir.DemisFhirNames.RELATED_NOTIFICATION_BUNDLE_CODING_SYSTEM;

import de.gematik.demis.service.base.error.ServiceException;
import de.gematik.demis.storage.common.entity.BinaryEntity;
import de.gematik.demis.storage.common.reader.BinaryMapper;
import de.gematik.demis.storage.reader.common.ReadService;
import de.gematik.demis.storage.reader.common.search.Filter;
import de.gematik.demis.storage.reader.common.search.RequestParamFilterResolver;
import de.gematik.demis.storage.reader.common.search.SearchSetService;
import de.gematik.demis.storage.reader.common.security.Caller;
import de.gematik.demis.storage.reader.config.FssReaderConfigProperties.SearchProps;
import de.gematik.demis.storage.reader.error.ErrorCode;
import java.util.Map;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Coding;
import org.springframework.stereotype.Service;

@Service
public class BinaryReadService extends ReadService<BinaryEntity, Binary, Filter> {

  public BinaryReadService(
      final BinaryReadonlyRepository repository,
      final BinaryMapper mapper,
      final SearchProps searchProps,
      final SearchSetService searchSetService) {
    super(
        repository,
        mapper,
        new RequestParamFilterResolver<>(Filter::new, Map.of()),
        searchProps,
        searchSetService);
  }

  @Override
  protected void checkResourcePermission(final Caller caller, final BinaryEntity entity) {
    final String sender = caller.getName();
    if (sender == null || !sender.equalsIgnoreCase(entity.getResponsibleDepartment())) {
      throw ErrorCode.FORBIDDEN.exception(
          String.format("%s is not allowed to access resource", sender));
    }
  }

  @Override
  protected void enforceFilter(final Caller caller, final Filter filter) {
    if (filter.getResponsibleDepartment() == null) {
      filter.setResponsibleDepartment(caller.getName());
    }
  }

  /**
   * Validates whether the caller is allowed to filter by the specified responsible department.
   *
   * <p>This method ensures that the callers name matches the provided responsible department. If
   * the sender is missing or blank, or if the sender does not have the required authorization to
   * filter by the responsible department, an exception is thrown.
   *
   * @param caller the {@link Caller} containing the sender's information (preferred username).
   * @param filter the filter to validate against the sender's identity.
   * @throws ServiceException with HttpStatus 403 if the sender is missing or not authorized to
   *     filter by the responsible department.
   */
  @Override
  protected void checkFilterPermission(final Caller caller, final Filter filter) {
    final String sender = caller.getName();
    if (sender == null || sender.isBlank()) {
      throw ErrorCode.FORBIDDEN.exception("Sender information is missing");
    }

    if (!sender.equalsIgnoreCase(filter.getResponsibleDepartment())) {
      throw ErrorCode.FORBIDDEN.exception(
          String.format(
              "%s is not allowed to filter by %s", sender, filter.getResponsibleDepartment()));
    }
  }

  @Override
  protected String getResourceType() {
    return "Binary";
  }

  @Override
  protected String getBusinessId(final Binary binary) {
    return binary.getMeta().getTag().stream()
        .filter(coding -> RELATED_NOTIFICATION_BUNDLE_CODING_SYSTEM.equals(coding.getSystem()))
        .findFirst()
        .map(Coding::getCode)
        .orElse("N/A");
  }
}
