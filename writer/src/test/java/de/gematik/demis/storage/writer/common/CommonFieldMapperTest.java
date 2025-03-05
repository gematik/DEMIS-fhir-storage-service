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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.gematik.demis.storage.common.entity.AbstractResourceEntity;
import de.gematik.demis.storage.common.entity.BinaryEntity;
import de.gematik.demis.storage.common.entity.BundleEntity;
import de.gematik.demis.storage.common.entity.Tag;
import io.micrometer.tracing.CurrentTraceContext;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import java.util.List;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommonFieldMapperTest {

  @Mock private Tracer tracer;
  @InjectMocks private CommonFieldMapper underTest;

  @Test
  void setCommonFieldsFromResource() {
    final String responsibleDepartment = "1.01.0.53.";
    final List<Tag> tags =
        List.of(
            new Tag()
                .setSystem("https://demis.rki.de/fhir/CodeSystem/ResponsibleDepartmentNotifier")
                .setCode("1.11.0.11.01."),
            new Tag()
                .setSystem("https://demis.rki.de/fhir/CodeSystem/ResponsibleDepartment")
                .setCode(responsibleDepartment),
            new Tag()
                .setSystem("https://demis.rki.de/fhir/CodeSystem/RelatedNotificationBundle")
                .setCode("1a3a16aa-64e0-5eb1-8601-018fc3794b6e")
                .setDisplay(
                    "Relates to message with identifier: 1a3a16aa-64e0-5eb1-8601-018fc3794b6e"));

    final Resource bundle = new Bundle();
    final Meta meta = bundle.getMeta();
    tags.forEach(tag -> meta.addTag(tag.getSystem(), tag.getCode(), tag.getDisplay()));
    final String lastUpdated = "2024-01-02T14:19:29.114+01:00";
    meta.getLastUpdatedElement().setValueAsString(lastUpdated);

    final AbstractResourceEntity targetEntity = new BundleEntity();

    underTest.setCommonFieldsFromResource(targetEntity, bundle);

    assertThat(targetEntity)
        .returns(null, AbstractResourceEntity::getLastUpdated)
        .returns(tags, AbstractResourceEntity::getTags)
        .returns(responsibleDepartment, AbstractResourceEntity::getResponsibleDepartment);
  }

  @Test
  void setResourceIndependentFields() {
    final String traceId = "abc1234xyz";
    setupTracer(traceId);

    final var bundle = new BundleEntity();
    final var binary = new BinaryEntity();
    final List<AbstractResourceEntity> targetEntities = List.of(bundle, binary);
    underTest.setResourceIndependentFields(targetEntities);

    // assert sourceId
    assertThat(bundle.getSourceId()).isEqualTo(traceId);
    assertThat(binary.getSourceId()).isEqualTo(traceId);

    // assert lastUpdated
    assertThat(bundle.getLastUpdated()).isNull();
    assertThat(binary.getLastUpdated()).isNull();
  }

  private void setupTracer(final String traceId) {
    final CurrentTraceContext currentTraceContext = mock(CurrentTraceContext.class);
    final TraceContext traceContext = mock(TraceContext.class);
    when(traceContext.traceId()).thenReturn(traceId);
    when(currentTraceContext.context()).thenReturn(traceContext);
    when(tracer.currentTraceContext()).thenReturn(currentTraceContext);
  }
}
