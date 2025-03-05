package de.gematik.demis.storage.reader.api;

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
 * #L%
 */

import lombok.Generated;

public class ParameterNames {
  @Generated
  private ParameterNames() {
    throw new UnsupportedOperationException();
  }

  // Common filter
  public static final String PARAM_LAST_UPDATED = "_lastUpdated";
  public static final String PARAM_TAG = "_tag";
  public static final String PARAM_SOURCE = "_source";

  // Bundle specific filter
  public static final String PARAM_PROFILE = "_profile";
  public static final String PARAM_IDENTIFIER = "identifier"; // without underscore

  // Sort
  public static final String PARAM_SORT = "_sort";
  public static final String PARAM_SORT_ASC = "_sort:asc";
  public static final String PARAM_SORT_DESC = "_sort:desc";

  // Paging
  public static final String PARAM_COUNT = "_count";
  public static final String PARAM_OFFSET = "_offset";

  // Response Format
  public static final String PARAM_FORMAT = "_format";

  // header
  public static final String HEADER_SENDER = "x-sender";
}
