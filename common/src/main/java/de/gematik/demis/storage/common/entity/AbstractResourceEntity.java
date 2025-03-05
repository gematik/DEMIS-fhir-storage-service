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

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@ToString
@MappedSuperclass
public abstract class AbstractResourceEntity implements Entity {

  public static final String COLUMN_ID = "id";

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = COLUMN_ID)
  private UUID id;

  public static final String COLUMN_LAST_UPDATED = "last_updated";

  @Column(
      name = COLUMN_LAST_UPDATED,
      updatable = false,
      insertable = false,
      columnDefinition = "TIMESTAMP WITH TIME ZONE")
  private OffsetDateTime lastUpdated;

  public static final String COLUMN_RESPONSIBLE_DEPARTMENT = "responsible_department";

  @Column(name = COLUMN_RESPONSIBLE_DEPARTMENT)
  private String responsibleDepartment;

  public static final String COLUMN_TAGS = "tags";

  @Column(name = COLUMN_TAGS)
  @JdbcTypeCode(SqlTypes.JSON)
  private List<Tag> tags;

  public static final String COLUMN_SOURCE_ID = "source_id";

  @Column(name = COLUMN_SOURCE_ID, length = 50)
  private String sourceId;

  @Transient
  public abstract String toResourceId();
}
