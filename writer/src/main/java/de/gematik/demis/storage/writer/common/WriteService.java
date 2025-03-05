package de.gematik.demis.storage.writer.common;

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

import static de.gematik.demis.storage.writer.error.ErrorCode.FHIR_PARSE_ERROR;
import static de.gematik.demis.storage.writer.error.ErrorCode.VALIDATION_ERROR;
import static de.gematik.demis.storage.writer.util.ListUtil.map;

import ca.uhn.fhir.context.FhirContext;
import de.gematik.demis.storage.common.entity.AbstractResourceEntity;
import de.gematik.demis.storage.writer.error.ValidationError;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

@Service
@RequiredArgsConstructor
@Slf4j
public class WriteService {

  private final FhirContext fhirContext;
  private final ResourceRepository repository;
  private final CommonFieldMapper commonFieldMapper;
  private final ResourceDispatcher resourceDispatcher;
  private final TransactionTemplate transactionTemplate;
  private final ApplicationEventPublisher eventPublisher;

  private static List<String> extractIds(final List<AbstractResourceEntity> saved) {
    return map(saved, AbstractResourceEntity::toResourceId);
  }

  private static List<? extends IBaseResource> getResources(final IBaseResource resource) {
    if (resource instanceof Bundle bundle && bundle.getType() == BundleType.TRANSACTION) {
      return extractResourcesFromTransactionBundle(bundle);
    } else {
      return List.of(resource);
    }
  }

  private static List<Resource> extractResourcesFromTransactionBundle(final Bundle bundle) {
    return map(bundle.getEntry(), BundleEntryComponent::getResource);
  }

  public List<String> store(final String fhirNotificationAsJson) {
    final var resource = parseFhir(fhirNotificationAsJson);
    final var resourcesToStore = getResources(resource);
    validate(resourcesToStore);
    final var entities = createEntities(resourcesToStore);
    final var savedEntities =
        transactionTemplate.execute(
            status -> saveEntitiesAndFireEvents(resourcesToStore, entities));
    return extractIds(savedEntities);
  }

  private IBaseResource parseFhir(final String fhirBinaryAsJsonString) {
    try {
      return fhirContext.newJsonParser().parseResource(fhirBinaryAsJsonString);
    } catch (final Exception ex) {
      throw FHIR_PARSE_ERROR.exception("Error parsing fhir", ex);
    }
  }

  private void validate(final List<? extends IBaseResource> resourcesToStore) {
    final Map<String, Set<ValidationError>> validationErrors = new LinkedHashMap<>();

    int resourceIndex = 0;
    for (final IBaseResource resource : resourcesToStore) {
      final var errors =
          resourceDispatcher.dispatchResourceAction(resource, ResourceProcessor::validate);
      if (!errors.isEmpty()) {
        validationErrors.put("[" + resourceIndex + "]", errors);
      }
      resourceIndex++;
    }

    if (!validationErrors.isEmpty()) {
      throw VALIDATION_ERROR.exception(validationErrors.toString());
    }
  }

  private List<AbstractResourceEntity> createEntities(
      final List<? extends IBaseResource> resourcesToStore) {
    final var entities = map(resourcesToStore, this::createEntity);
    commonFieldMapper.setResourceIndependentFields(entities);
    return entities;
  }

  private AbstractResourceEntity createEntity(final IBaseResource resource) {
    final var entity =
        resourceDispatcher.dispatchResourceAction(resource, ResourceProcessor::createEntity);
    commonFieldMapper.setCommonFieldsFromResource(entity, resource);
    return entity;
  }

  private List<AbstractResourceEntity> saveEntitiesAndFireEvents(
      final List<? extends IBaseResource> resources, final List<AbstractResourceEntity> entities) {
    final var saved = repository.saveAll(entities);
    publishSaveEventsInTx(resources, saved);
    return saved;
  }

  private void publishSaveEventsInTx(
      final List<? extends IBaseResource> resources,
      final List<AbstractResourceEntity> savedEntities) {
    Assert.state(
        resources.size() == savedEntities.size(),
        () -> "Number of saved entities does not match number of resources stored");
    for (int i = 0; i < resources.size(); i++) {
      final var event = new ResourceSavedEvent<>(resources.get(i), savedEntities.get(i));
      log.debug("fire event: {}", event);
      eventPublisher.publishEvent(event);
    }
  }
}
