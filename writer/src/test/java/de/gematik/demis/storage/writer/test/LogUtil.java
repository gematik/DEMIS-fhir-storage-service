package de.gematik.demis.storage.writer.test;

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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.Optional;
import org.slf4j.LoggerFactory;

public class LogUtil {
  public static <T> ListAppender<ILoggingEvent> listenToLog(final Class<T> clazz) {
    final var log = (Logger) LoggerFactory.getLogger(clazz);
    final var listAppender = new ListAppender<ILoggingEvent>();
    listAppender.start();
    log.addAppender(listAppender);
    return listAppender;
  }

  public static boolean hasLogMessage(
      final ListAppender<ILoggingEvent> logAppender, final String expectedLogMessage) {
    return logAppender.list.stream()
        .anyMatch(loggingEvent -> expectedLogMessage.equals(loggingEvent.getFormattedMessage()));
  }

  public static Optional<ILoggingEvent> getFirstLogOfLevel(
      final ListAppender<ILoggingEvent> logEvents, final Level level) {
    return logEvents.list.stream().filter(event -> event.getLevel() == level).findFirst();
  }
}
