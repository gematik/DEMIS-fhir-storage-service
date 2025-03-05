package de.gematik.demis.storage.writer.config;

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

import de.gematik.demis.storage.common.entity.HapiBundleEntity;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.support.ContextPropagatingTaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@ConditionalOnHapiSync
@EnableAsync
@EnableScheduling
@EntityScan(basePackageClasses = HapiBundleEntity.class)
@Slf4j
class HapiSyncConfiguration {

  @Value("${fss.hapi.threads.core-size}")
  private int corePoolSize;

  @Value("${fss.hapi.threads.max-size}")
  private int maxPoolSize;

  @Value("${fss.hapi.threads.queue-capacity}")
  private int queueCapacity;

  @PostConstruct
  void log() {
    log.info("Bundle Synchronization to Hapi Fhir Server enabled!");
  }

  @Bean(name = "hapiFhirExecutor")
  public ThreadPoolTaskExecutor taskExecutor() {
    final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(corePoolSize);
    executor.setMaxPoolSize(maxPoolSize);
    executor.setQueueCapacity(queueCapacity);
    executor.setThreadNamePrefix("hapi-fhir-client-");
    executor.setTaskDecorator(new ContextPropagatingTaskDecorator());
    executor.initialize();
    log.info(
        "HAPI FHIR executor initialized. Core: {} Max: {} Queue: {}",
        corePoolSize,
        maxPoolSize,
        queueCapacity);
    return executor;
  }
}
