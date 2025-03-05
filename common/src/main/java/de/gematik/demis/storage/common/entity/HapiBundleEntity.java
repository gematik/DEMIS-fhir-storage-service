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

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity(name = "hapi_bundles")
@Table(
    name = "hapi_bundles",
    indexes = {@Index(name = "idx_hapi_bundles_status", columnList = "status")})
@Getter
@Setter
@ToString
public class HapiBundleEntity {

  @Id private UUID bundleId;
  @Version private LocalDateTime modifiedAt;

  @Enumerated(EnumType.ORDINAL)
  private HapiSyncedStatus status = HapiSyncedStatus.NEW;

  private Integer responseCode;
  private String hapiId;
  private String error;
}
