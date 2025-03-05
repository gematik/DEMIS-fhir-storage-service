package de.gematik.demis.storage.writer.bundle.hapifhir;

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

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest(
    properties = "fss.hapi.enabled=false",
    classes = {BundleSavedEventListener.class, SynchronizationCronJob.class, HapiSender.class})
class HapiSyncDisabledIntegrationTest {

  @Autowired ApplicationContext applicationContext;

  @Test
  void eventListenerDisabled() {
    assertThrows(
        NoSuchBeanDefinitionException.class,
        () -> applicationContext.getBean(BundleSavedEventListener.class));
  }

  @Test
  void cronJobDisabled() {
    assertThrows(
        NoSuchBeanDefinitionException.class,
        () -> applicationContext.getBean(SynchronizationCronJob.class));
  }

  @Test
  void noHapiSender() {
    assertThrows(
        NoSuchBeanDefinitionException.class, () -> applicationContext.getBean(HapiSender.class));
  }
}
