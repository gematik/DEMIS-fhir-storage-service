package de.gematik.demis.storage.purger.external.hapi;

/*-
 * #%L
 * fhir-storage-purger
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

import de.gematik.demis.storage.common.entity.HapiBundleEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * The auto-DDL from JPA generates this for tests. In production, the Spring app does not need this
 * data. Therefore, there is no production entity for it.
 */
@Entity(name = "purger_hapi_bundle")
@Getter
@Setter
@ToString(callSuper = true)
public class PurgerHapiBundleEntity extends HapiBundleEntity {

  public static final String COLUMN_PURGER_ID = "purger_id";
  public static final String COLUMN_PURGER_STARTED = "purger_started";
  public static final String COLUMN_PURGER_ATTEMPT = "purger_attempt";

  @Column(name = COLUMN_PURGER_ID)
  private String purgerId;

  @Column(name = COLUMN_PURGER_STARTED, columnDefinition = "TIMESTAMP WITH TIME ZONE")
  private OffsetDateTime purgerStarted;

  @Column(name = COLUMN_PURGER_ATTEMPT)
  private int purgerAttempt;
}
