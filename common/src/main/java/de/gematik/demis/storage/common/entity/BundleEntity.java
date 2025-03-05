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

import de.gematik.demis.storage.common.converter.CompressStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Immutable;

@Entity(name = "bundles")
@Table(
    name = "bundles",
    indexes = {
      @Index(name = "idx_bundles_last_updated", columnList = "lastUpdated"),
      @Index(name = "idx_bundles_responsible_department", columnList = "responsibleDepartment"),
      @Index(name = "idx_bundles_notification_bundle_id", columnList = "notificationBundleId"),
      @Index(name = "idx_bundles_profile", columnList = "profile")
    })
@Getter
@Setter
@ToString(callSuper = true)
@Immutable
public class BundleEntity extends AbstractResourceEntity {

  public static final String COLUMN_CONTENT = "content";
  public static final String COLUMN_NOTIFICATION_BUNDLE_ID = "notification_bundle_id";
  public static final String COLUMN_NOTIFICATION_ID = "notification_id";
  public static final String COLUMN_PROFILE = "profile";

  @NotNull
  @Convert(converter = CompressStringConverter.class)
  @Column(name = COLUMN_CONTENT)
  private String content;

  @Column(name = COLUMN_NOTIFICATION_BUNDLE_ID)
  private String notificationBundleId;

  @Column(name = COLUMN_NOTIFICATION_ID)
  private String notificationId;

  @Column(name = COLUMN_PROFILE)
  private String profile;

  @Override
  public String toResourceId() {
    return "Bundle/" + getId();
  }
}
