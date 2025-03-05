package de.gematik.demis.storage.common.entity;

/*-
 * #%L
 * fhir-storage-common
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

import de.gematik.demis.storage.common.converter.CompressBytesConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Immutable;

@Entity(name = "binaries")
@Table(
    name = "binaries",
    indexes = {
      @Index(name = "idx_binaries_last_updated", columnList = "lastUpdated"),
      @Index(name = "idx_binaries_responsible_department", columnList = "responsibleDepartment")
    })
@Getter
@Setter
@ToString(callSuper = true)
@Immutable
public class BinaryEntity extends AbstractResourceEntity {

  public static final String COLUMN_CONTENT_TYPE = "content_type";
  public static final String COLUMN_DATA = "data";

  @Size(max = 255)
  @NotNull
  @Column(name = COLUMN_CONTENT_TYPE)
  private String contentType;

  @NotNull
  @Convert(converter = CompressBytesConverter.class)
  @Column(name = COLUMN_DATA)
  private byte[] data;

  @Override
  public String toResourceId() {
    return "Binary/" + getId();
  }
}
