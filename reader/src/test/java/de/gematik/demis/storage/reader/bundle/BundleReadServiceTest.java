package de.gematik.demis.storage.reader.bundle;

/*-
 * #%L
 * fhir-storage-reader
 * %%
 * Copyright (C) 2025 - 2026 gematik GmbH
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
 * For additional notes and disclaimer from gematik and in case of changes by gematik,
 * find details in the "Readme" file.
 * #L%
 */

import static de.gematik.demis.storage.common.entity.AbstractResourceEntity.COLUMN_LAST_UPDATED;
import static de.gematik.demis.storage.reader.test.TestUtil.paramStringToMultiValueMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import de.gematik.demis.service.base.error.ServiceException;
import de.gematik.demis.storage.common.entity.BundleEntity;
import de.gematik.demis.storage.reader.common.search.Filter;
import de.gematik.demis.storage.reader.common.search.SearchSetService;
import de.gematik.demis.storage.reader.common.security.Caller;
import de.gematik.demis.storage.reader.config.FssReaderConfigProperties;
import de.gematik.demis.storage.reader.error.ErrorCode;
import de.gematik.demis.storage.reader.test.TestData;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class BundleReadServiceTest {

  private static final String CALLER_NAME = "1.";
  private static final Set<String> AUTHORIZED_PROFILES = Set.of("MyTestProfile");

  @Mock BundleMapper mapperMock;
  @Mock BundleReadonlyRepository bundleReadonlyRepositoryMock;
  @Mock FssReaderConfigProperties.SearchProps searchProps;
  @Mock SearchSetService searchSetServiceMock;

  @InjectMocks BundleReadService underTest;

  @Nested
  class FindByIdTest {
    @Test
    void findById_success() {
      final String profile = "NotificationProfile";
      final var caller = createCaller(Set.of(profile));

      final UUID uuid = UUID.randomUUID();
      final BundleEntity entity = new BundleEntity();
      entity
          .setProfile(profile)
          .setResponsibleDepartment("is-irrelevant-for-permission")
          .setId(uuid);
      final Bundle bundle = new Bundle();

      when(bundleReadonlyRepositoryMock.findById(any())).thenReturn(Optional.of(entity));
      when(mapperMock.entityToResource(any())).thenReturn(bundle);

      final Bundle result = underTest.findById(caller, uuid);

      assertThat(result).isSameAs(bundle);
      verify(bundleReadonlyRepositoryMock).findById(uuid);
      verify(mapperMock).entityToResource(entity);
    }

    @Test
    void findById_notFound() {
      final var caller = createCaller(AUTHORIZED_PROFILES);
      final UUID uuid = UUID.randomUUID();

      when(bundleReadonlyRepositoryMock.findById(any())).thenReturn(Optional.empty());

      final ServiceException exception =
          catchThrowableOfType(ServiceException.class, () -> underTest.findById(caller, uuid));
      assertThat(exception)
          .isNotNull()
          .returns(ErrorCode.RESOURCE_NOT_FOUND.getCode(), ServiceException::getErrorCode)
          .returns(HttpStatus.NOT_FOUND, ServiceException::getResponseStatus);
    }

    @Test
    void findById_forbidden() {
      final var caller = createCaller(Set.of("Wrong-Profile"));
      final UUID uuid = UUID.randomUUID();
      final BundleEntity entity = new BundleEntity();
      entity.setProfile("Other-Profile").setResponsibleDepartment(caller.getName()).setId(uuid);

      when(bundleReadonlyRepositoryMock.findById(any())).thenReturn(Optional.of(entity));

      final ServiceException exception =
          catchThrowableOfType(ServiceException.class, () -> underTest.findById(caller, uuid));
      assertThat(exception)
          .isNotNull()
          .returns(ErrorCode.FORBIDDEN.getCode(), ServiceException::getErrorCode)
          .returns(HttpStatus.FORBIDDEN, ServiceException::getResponseStatus);
    }
  }

  @Nested
  class SearchTest {

    private static String toResponsibleDepartmentParameter(final String responsibleDepartment) {
      return "_tag=https://demis.rki.de/fhir/CodeSystem/ResponsibleDepartment|"
          + responsibleDepartment;
    }

    @Test
    void searchWithoutRequestParams_okay() {
      final int pageSize = 10;
      final Set<String> profiles =
          Set.of("https://demis.rki.de/fhir/StructureDefinition/NotificationBundleDisease");

      final Pageable expectedPageable =
          PageRequest.of(0, pageSize).withSort(Direction.ASC, COLUMN_LAST_UPDATED);
      final Filter expectedFilter = new BundleFilter().setProfiles(profiles);
      final BundleEntity bundleEntity = TestData.createBundleEntity();
      final Page<BundleEntity> dbResult =
          new PageImpl<>(List.of(bundleEntity), expectedPageable, 1);

      final var bundle = new Bundle();
      bundle.setId(bundleEntity.toResourceId());
      final Page<Bundle> mappedPage = new PageImpl<>(List.of(bundle), expectedPageable, 1);

      final Bundle searchSetBundle = new Bundle();

      setupSearchProps(pageSize, pageSize);
      when(bundleReadonlyRepositoryMock.search(any(Filter.class), any())).thenReturn(dbResult);
      when(mapperMock.entityToResource(any())).thenReturn(bundle);
      when(searchSetServiceMock.createSearchSet(any())).thenReturn(searchSetBundle);

      // execute
      final Bundle result =
          underTest.search(createCaller(profiles), paramStringToMultiValueMap(null));

      // assert
      assertThat(result).isSameAs(searchSetBundle);
      verify(bundleReadonlyRepositoryMock).search(expectedFilter, expectedPageable);
      verify(mapperMock).entityToResource(bundleEntity);
      verify(searchSetServiceMock).createSearchSet(mappedPage);
    }

    @Test
    void filterIdentifier_success() {
      final String notificationBundleId = "123-abc-456-def";
      final String parameter =
          "identifier=https://demis.rki.de/fhir/NamingSystem/NotificationBundleId|"
              + notificationBundleId;
      final Filter expectedFilter =
          new BundleFilter()
              .setProfiles(AUTHORIZED_PROFILES)
              .setNotificationBundleId(notificationBundleId);

      executeFilterTest(parameter, expectedFilter, AUTHORIZED_PROFILES);
    }

    @Test
    void requestOtherHealthOffice_success() {
      final String responsibleDepartmentInRequest = "1.01.0.53.";
      final Set<String> profiles =
          Set.of("https://demis.rki.de/fhir/StructureDefinition/NotificationBundleDisease");

      final var parameter = toResponsibleDepartmentParameter(responsibleDepartmentInRequest);
      final Filter expectedFilter =
          new BundleFilter()
              .setProfiles(profiles)
              .setResponsibleDepartment(responsibleDepartmentInRequest);
      executeFilterTest(parameter, expectedFilter, profiles);
    }

    @Test
    void filterProfile_success() {
      final String profile =
          "https://demis.rki.de/fhir/StructureDefinition/NotificationBundleDisease";
      final String param = "_profile=" + profile;
      final Filter expectedFilter = new BundleFilter().setProfiles(Set.of(profile));
      executeFilterTest(param, expectedFilter, Set.of(profile, "OtherProfile"));
    }

    @Test
    void filterMultipleProfiles_success() {
      final String profile1 =
          "https://demis.rki.de/fhir/StructureDefinition/NotificationBundleDisease";
      final String profile2 =
          "https://demis.rki.de/fhir/StructureDefinition/NotificationBundlePathogen";
      final String param = String.format("_profile=%s&_profile=%s", profile1, profile2);
      final Filter expectedFilter = new BundleFilter().setProfiles(Set.of(profile1, profile2));
      executeFilterTest(param, expectedFilter, Set.of(profile1, profile2, "OtherProfile"));
    }

    @Test
    void filterUnauthorizedProfile_throwsException() {
      final String profile1 =
          "https://demis.rki.de/fhir/StructureDefinition/NotificationBundleDisease";
      final String profile2 =
          "https://demis.rki.de/fhir/StructureDefinition/NotificationBundlePathogen";
      final String param = String.format("_profile=%s&_profile=%s", profile1, profile2);

      final var caller = createCaller(Set.of(profile1));

      final ServiceException exception =
          catchThrowableOfType(
              ServiceException.class,
              () -> underTest.search(caller, paramStringToMultiValueMap(param)));

      assertThat(exception)
          .isNotNull()
          .returns(ErrorCode.FORBIDDEN.getCode(), ServiceException::getErrorCode)
          .returns(HttpStatus.FORBIDDEN, ServiceException::getResponseStatus);

      verifyNoInteractions(bundleReadonlyRepositoryMock);
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          // system 'NotificationBundleId' is not allowed (must be full qualified)
          "identifier=NotificationBundleId|123-abc-456-def",
          // more than one identifier are not allowed
          "identifier=https://demis.rki.de/fhir/NamingSystem/NotificationBundleId|123&identifier=a|2"
        })
    void invalidFilter_throwsException(final String parameter) {
      final var map = paramStringToMultiValueMap(parameter);

      final ServiceException exception =
          catchThrowableOfType(
              ServiceException.class,
              () -> underTest.search(createCaller(Set.of("irrelevant")), map));

      assertThat(exception)
          .isNotNull()
          .returns(ErrorCode.INVALID_FILTER.getCode(), ServiceException::getErrorCode);
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          // complex search queries are not supported
          "composition.type=https://loinc.org|34782-3",
          "patient.gender=male"
        })
    void unsupportedRequestParameter_throwsException(final String parameter) {
      final var map = paramStringToMultiValueMap(parameter);

      final ServiceException exception =
          catchThrowableOfType(
              ServiceException.class,
              () -> underTest.search(createCaller(Set.of("irrelevant")), map));

      assertThat(exception)
          .isNotNull()
          .returns(
              ErrorCode.UNSUPPORTED_REQUEST_PARAMETER.getCode(), ServiceException::getErrorCode)
          .returns("Unsupported request params: " + map.keySet(), ServiceException::getMessage);
    }

    @ParameterizedTest
    @CsvSource({"17, 34", "1, 0", "1, 1000", "100, 101"})
    void paging_success(final int count, final int offset) {
      final String requestParams = String.format("_count=%s&_offset=%s", count, offset);

      final Filter expectedFilter = new BundleFilter().setProfiles(AUTHORIZED_PROFILES);
      final Pageable expectedPageable =
          PageRequest.of(offset / count, count).withSort(Direction.ASC, COLUMN_LAST_UPDATED);

      final Bundle searchSetBundle = new Bundle();
      setupSearchProps(100, 100);
      when(bundleReadonlyRepositoryMock.search(any(Filter.class), any())).thenReturn(Page.empty());
      when(searchSetServiceMock.createSearchSet(any())).thenReturn(searchSetBundle);

      // execute
      final Bundle result =
          underTest.search(
              createCaller(AUTHORIZED_PROFILES), paramStringToMultiValueMap(requestParams));

      assertThat(result).isSameAs(searchSetBundle);
      verify(bundleReadonlyRepositoryMock).search(expectedFilter, expectedPageable);
    }

    @ParameterizedTest
    @CsvSource({"0, 10", "-1, 10", "1, -1", "-1, -1"})
    void invalidCount_throwsException(final int count, final int offset) {
      final var caller = createCaller(AUTHORIZED_PROFILES);
      final var requestParams =
          paramStringToMultiValueMap(
              String.format(
                  "%s&_count=%s&_offset=%s",
                  toResponsibleDepartmentParameter(caller.getName()), count, offset));

      final ServiceException exception =
          catchThrowableOfType(
              ServiceException.class, () -> underTest.search(caller, requestParams));

      assertThat(exception)
          .isNotNull()
          .returns(ErrorCode.INVALID_PAGING.getCode(), ServiceException::getErrorCode);

      verifyNoInteractions(bundleReadonlyRepositoryMock);
    }

    private void executeFilterTest(
        final String parameter, final Filter expectedFilter, final Set<String> profiles) {
      final Bundle searchSetBundle = new Bundle();

      setupSearchProps(1, 1);
      when(bundleReadonlyRepositoryMock.search(any(Filter.class), any())).thenReturn(Page.empty());
      when(searchSetServiceMock.createSearchSet(any())).thenReturn(searchSetBundle);

      final var caller = createCaller(profiles);

      // execute
      final Bundle result = underTest.search(caller, paramStringToMultiValueMap(parameter));

      assertThat(result).isSameAs(searchSetBundle);
      verify(bundleReadonlyRepositoryMock).search(eq(expectedFilter), any());
    }

    private void setupSearchProps(int defaultPageSize, int maxPageSize) {
      when(searchProps.defaultPageSize()).thenReturn(defaultPageSize);
      when(searchProps.maxPageSize()).thenReturn(maxPageSize);
    }
  }

  private static Caller createCaller(final Set<String> authorizedProfiles) {
    return new Caller(CALLER_NAME, () -> authorizedProfiles);
  }
}
