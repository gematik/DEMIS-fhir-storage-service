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
import de.gematik.demis.storage.common.entity.BinaryEntity;
import de.gematik.demis.storage.reader.common.search.Filter;
import de.gematik.demis.storage.reader.common.search.SearchSetService;
import de.gematik.demis.storage.reader.common.security.Caller;
import de.gematik.demis.storage.reader.config.FssReaderConfigProperties;
import de.gematik.demis.storage.reader.error.ErrorCode;
import de.gematik.demis.storage.reader.test.TestData;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.hl7.fhir.r4.model.Binary;
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
import org.springframework.util.MultiValueMapAdapter;

@ExtendWith(MockitoExtension.class)
class BinaryReadServiceTest {

  @Mock BinaryMapper mapperMock;
  @Mock BinaryReadonlyRepository binaryReadonlyRepositoryMock;
  @Mock FssReaderConfigProperties.SearchProps searchProps;
  @Mock SearchSetService searchSetServiceMock;

  @InjectMocks BinaryReadService underTest;

  @Nested
  class FindByIdTest {
    @Test
    void findById_success() {
      final String responsibleDepartment = "1.01.0.53.";
      final var caller = createCaller(responsibleDepartment);

      final UUID uuid = UUID.randomUUID();
      final BinaryEntity entity = new BinaryEntity();
      entity.setResponsibleDepartment(responsibleDepartment).setId(uuid);
      final Binary binary = new Binary();

      when(binaryReadonlyRepositoryMock.findById(any())).thenReturn(Optional.of(entity));
      when(mapperMock.entityToResource(any())).thenReturn(binary);

      final Binary result = underTest.findById(caller, uuid);

      assertThat(result).isSameAs(binary);
      verify(binaryReadonlyRepositoryMock).findById(uuid);
      verify(mapperMock).entityToResource(entity);
    }

    @Test
    void findById_notFound() {
      final var caller = createCaller("does-not-matter");
      final UUID uuid = UUID.randomUUID();

      when(binaryReadonlyRepositoryMock.findById(any())).thenReturn(Optional.empty());

      final ServiceException exception =
          catchThrowableOfType(ServiceException.class, () -> underTest.findById(caller, uuid));
      assertThat(exception)
          .isNotNull()
          .returns(ErrorCode.RESOURCE_NOT_FOUND.getCode(), ServiceException::getErrorCode)
          .returns(HttpStatus.NOT_FOUND, ServiceException::getResponseStatus);
    }

    @Test
    void findById_forbidden() {
      final var caller = createCaller("not-responsible-user");
      final UUID uuid = UUID.randomUUID();
      final BinaryEntity entity = new BinaryEntity();
      entity.setResponsibleDepartment("different-from-the-caller").setId(uuid);

      when(binaryReadonlyRepositoryMock.findById(any())).thenReturn(Optional.of(entity));

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
    void search_okay() {
      final String responsibleDepartment = "1.01.0.53.";
      final var caller = createCaller(responsibleDepartment);
      final var params =
          paramStringToMultiValueMap(toResponsibleDepartmentParameter(responsibleDepartment));
      final int pageSize = 10;

      final Pageable expectedPageable =
          PageRequest.of(0, pageSize).withSort(Direction.ASC, COLUMN_LAST_UPDATED);
      final Filter expectedFilter = new Filter().setResponsibleDepartment(responsibleDepartment);
      final BinaryEntity binaryEntity = TestData.createBinaryEntity();
      final Page<BinaryEntity> dbResult =
          new PageImpl<>(List.of(binaryEntity), expectedPageable, 1);

      final Binary binary = new Binary();
      binary.setId(binaryEntity.toResourceId());
      final Page<Binary> mappedPage = new PageImpl<>(List.of(binary), expectedPageable, 1);

      final Bundle searchSetBundle = new Bundle();

      setupSearchProps(pageSize, pageSize);
      when(binaryReadonlyRepositoryMock.search(any(Filter.class), any())).thenReturn(dbResult);
      when(mapperMock.entityToResource(any())).thenReturn(binary);
      when(searchSetServiceMock.createSearchSet(any())).thenReturn(searchSetBundle);

      // execute
      final Bundle result = underTest.search(caller, params);

      // assert
      assertThat(result).isSameAs(searchSetBundle);
      verify(binaryReadonlyRepositoryMock).search(expectedFilter, expectedPageable);
      verify(mapperMock).entityToResource(binaryEntity);
      verify(searchSetServiceMock).createSearchSet(mappedPage);
    }

    @ParameterizedTest
    @CsvSource({"17, 34", "1, 0", "1, 1000", "100, 101"})
    void paging_success(final int count, final int offset) {
      final var caller = createCaller("1.02.0.54.");
      final String requestParams =
          String.format(
              "%s&_count=%s&_offset=%s",
              toResponsibleDepartmentParameter(caller.getName()), count, offset);

      final Filter expectedFilter = new Filter().setResponsibleDepartment(caller.getName());
      final Pageable expectedPageable =
          PageRequest.of(offset / count, count).withSort(Direction.ASC, COLUMN_LAST_UPDATED);

      final Bundle searchSetBundle = new Bundle();
      setupSearchProps(100, 100);
      when(binaryReadonlyRepositoryMock.search(any(Filter.class), any())).thenReturn(Page.empty());
      when(searchSetServiceMock.createSearchSet(any())).thenReturn(searchSetBundle);

      // execute
      final Bundle result = underTest.search(caller, paramStringToMultiValueMap(requestParams));

      assertThat(result).isSameAs(searchSetBundle);
      verify(binaryReadonlyRepositoryMock).search(expectedFilter, expectedPageable);
    }

    @ParameterizedTest
    @CsvSource({"0, 10", "-1, 10", "1, -1", "-1, -1"})
    void invalidCount_throwsException(final int count, final int offset) {
      final var caller = createCaller("1.02.0.54.");
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

      verifyNoInteractions(binaryReadonlyRepositoryMock);
    }

    @Test
    void requestOtherHealthOffice_throwsException() {
      final String responsibleDepartmentInRequest = "1.01.0.54.";
      final var caller = createCaller("2.1.1.4");

      final var params =
          paramStringToMultiValueMap(
              toResponsibleDepartmentParameter(responsibleDepartmentInRequest));

      final ServiceException exception =
          catchThrowableOfType(ServiceException.class, () -> underTest.search(caller, params));

      assertThat(exception)
          .isNotNull()
          .returns(ErrorCode.FORBIDDEN.getCode(), ServiceException::getErrorCode)
          .returns(HttpStatus.FORBIDDEN, ServiceException::getResponseStatus);

      verifyNoInteractions(binaryReadonlyRepositoryMock);
    }

    @Test
    void noResponsibleDepartmentFilter_success() {
      final String sender = "1.0.53.";
      final var caller = createCaller(sender);
      final Bundle searchSetBundle = new Bundle();

      // caller is automatically set as responsible department filter
      final Filter expectedFilter = new Filter().setResponsibleDepartment(sender);

      setupSearchProps(1, 1);
      when(binaryReadonlyRepositoryMock.search(any(Filter.class), any())).thenReturn(Page.empty());
      when(searchSetServiceMock.createSearchSet(any())).thenReturn(searchSetBundle);

      // execute
      final Bundle result = underTest.search(caller, new MultiValueMapAdapter<>(Map.of()));

      assertThat(result).isSameAs(searchSetBundle);
      verify(binaryReadonlyRepositoryMock).search(eq(expectedFilter), any());
    }

    @Test
    void noResponsibleDepartmentFilterAndNoSenderHeader_throwsException() {
      final var caller = createCaller(null);
      final var requestParams = new MultiValueMapAdapter<String, String>(Map.of());

      final ServiceException exception =
          catchThrowableOfType(
              ServiceException.class, () -> underTest.search(caller, requestParams));

      assertThat(exception)
          .isNotNull()
          .returns(ErrorCode.FORBIDDEN.getCode(), ServiceException::getErrorCode)
          .returns(HttpStatus.FORBIDDEN, ServiceException::getResponseStatus);
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          // complex search queries are not supported
          "composition.type=https://loinc.org|34782-3",
          "patient.gender=male",
          // binary does not support these exclusive bundle filter
          "_profile=https://demis.rki.de/fhir/StructureDefinition/NotificationBundleDisease",
          "identifier=https://demis.rki.de/fhir/NamingSystem/NotificationBundleId|abc-123"
        })
    void unsupportedRequestParameter_throwsException(final String parameter) {
      final var map = paramStringToMultiValueMap(parameter);

      final ServiceException exception =
          catchThrowableOfType(
              ServiceException.class, () -> underTest.search(createCaller("irrelevant"), map));

      assertThat(exception)
          .isNotNull()
          .returns(
              ErrorCode.UNSUPPORTED_REQUEST_PARAMETER.getCode(), ServiceException::getErrorCode)
          .returns("Unsupported request params: " + map.keySet(), ServiceException::getMessage);
    }

    private void setupSearchProps(int defaultPageSize, int maxPageSize) {
      when(searchProps.defaultPageSize()).thenReturn(defaultPageSize);
      when(searchProps.maxPageSize()).thenReturn(maxPageSize);
    }
  }

  private static Caller createCaller(final String name) {
    return new Caller(name, () -> Set.of("does-not-matter"));
  }
}
