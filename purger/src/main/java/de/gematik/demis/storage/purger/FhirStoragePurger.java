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

import de.gematik.demis.storage.ScanBaseMarker;
import de.gematik.demis.storage.purger.external.ExternalPurge;
import de.gematik.demis.storage.purger.internal.InternalPurge;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackageClasses = ScanBaseMarker.class)
@ConfigurationPropertiesScan
@EnableAsync
@Slf4j
public class FhirStoragePurger implements CommandLineRunner {

  private final List<InternalPurge> internalPurges;
  private final List<ExternalPurge> externalPurges;

  private final boolean enabled;

  public FhirStoragePurger(
      List<InternalPurge> internalPurges,
      List<ExternalPurge> externalPurges,
      @Value("${fss.purger.job.enabled:true}") boolean enabled) {
    this.internalPurges = internalPurges;
    this.externalPurges = externalPurges;
    this.enabled = enabled;
  }

  private static String formatDuration(long durationMillis) {
    var millis = durationMillis;
    if (durationMillis < 0) {
      log.warn(
          "Impossible error of negative duration based on system time! DurationMillis: {}",
          durationMillis);
      millis = -millis;
    }
    return DurationFormatUtils.formatDuration(millis, "[d'd'H'h'm'm's's']");
  }

  private static String formatDurationUntilNow(long startMillis) {
    return formatDuration(System.currentTimeMillis() - startMillis);
  }

  /**
   * Start one-time job to purge FHIR storage.
   *
   * @param args ignored
   */
  public static void main(final String[] args) {
    SpringApplication.run(FhirStoragePurger.class, args).close();
  }

  private static <P extends Purge> String print(Collection<P> purges) {
    return purges.stream().map(Purge::name).collect(Collectors.joining(", ", "[", "]"));
  }

  @Override
  public final void run(String... args) {
    if (enabled) {
      runPurge();
    } else {
      log.warn("Purging job is disabled");
    }
  }

  final void runPurge() {
    final long startMillis = System.currentTimeMillis();
    runPurges(internalPurges);
    runPurges(externalPurges);
    logEnd(startMillis);
  }

  private <P extends Purge> void runPurges(Collection<P> purges) {
    logStartPhase(purges);
    final long startMillis = System.currentTimeMillis();
    try {
      CompletableFuture.allOf(purges.stream().map(this::run).toArray(CompletableFuture[]::new))
          .join();
    } catch (Exception e) {
      log.error("Purging finished with at least one error! First error printed as stacktrace.", e);
    } finally {
      logEndPhase(purges, startMillis);
    }
  }

  private CompletableFuture<Void> run(Purge purge) {
    try {
      return purge
          .run()
          .exceptionally(
              t -> {
                log.error("Error while purging {}", purge.name(), t);
                return null;
              });
    } catch (Exception e) {
      log.error("Error while starting purging {}", purge.name(), e);
      return CompletableFuture.failedFuture(e);
    }
  }

  private <P extends Purge> void logStartPhase(Collection<P> purges) {
    log.info("Starting purging phase. Purges: {}", print(purges));
  }

  private <P extends Purge> void logEndPhase(Collection<P> purges, long startMillis) {
    log.info(
        "Finished purging phase. Purges: {} Duration: {}",
        print(purges),
        formatDurationUntilNow(startMillis));
  }

  private void logEnd(final long startMillis) {
    log.info("Purging finished. Duration: {}", formatDurationUntilNow(startMillis));
  }
}
