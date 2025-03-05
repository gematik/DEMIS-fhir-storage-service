package de.gematik.demis.storage.purger;

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

public final class FhirStoragePurgerJob {

  private FhirStoragePurgerJob() {
    throw new UnsupportedOperationException("This class is not supposed to be instantiated");
  }

  /**
   * Start purge job. This is a tweak for Spring Boot integration tests. It is necessary to start
   * the job explicitly as the implicit start in the main method happens before the execution of the
   * SQL test data scripts.
   *
   * @param fhirStoragePurger the purger
   */
  public static void run(FhirStoragePurger fhirStoragePurger) {
    fhirStoragePurger.runPurge();
  }
}
