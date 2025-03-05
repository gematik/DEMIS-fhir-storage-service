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

import static de.gematik.demis.storage.common.fhir.DemisFhirNames.RESPONSIBLE_HEALTH_DEPARTMENT_CODING_SYSTEM;
import static de.gematik.demis.storage.writer.util.FhirQueries.findFirstTagOfSystem;
import static de.gematik.demis.storage.writer.util.ListUtil.map;

import de.gematik.demis.storage.common.entity.AbstractResourceEntity;
import de.gematik.demis.storage.common.entity.Tag;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import jakarta.annotation.Nullable;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.hl7.fhir.instance.model.api.IBaseMetaType;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
final class CommonFieldMapper {

  private final Tracer tracer;

  public void setCommonFieldsFromResource(
      final AbstractResourceEntity entity, final IBaseResource resource) {
    final IBaseMetaType meta = resource.getMeta();
    entity
        .setResponsibleDepartment(getResponsibleDepartment(meta))
        .setTags(map(meta.getTag(), this::convertTag));
  }

  @Nullable
  private String getResponsibleDepartment(final IBaseMetaType meta) {
    return findFirstTagOfSystem(meta.getTag(), RESPONSIBLE_HEALTH_DEPARTMENT_CODING_SYSTEM)
        .map(IBaseCoding::getCode)
        .orElse(null);
  }

  private Tag convertTag(final IBaseCoding coding) {
    return new Tag()
        .setSystem(coding.getSystem())
        .setCode(coding.getCode())
        .setDisplay(coding.getDisplay());
  }

  public void setResourceIndependentFields(final List<AbstractResourceEntity> entities) {
    setSourceId(entities);
  }

  private void setSourceId(final List<AbstractResourceEntity> entities) {
    final TraceContext context = tracer.currentTraceContext().context();
    if (context != null) {
      final String traceId = context.traceId();
      entities.forEach(entity -> entity.setSourceId(traceId));
    }
  }
}
