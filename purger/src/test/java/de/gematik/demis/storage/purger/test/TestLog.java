package de.gematik.demis.storage.purger.test;

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

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class TestLog {

  private final ListAppender<ILoggingEvent> logs;

  public static <T> TestLog of(Class<T> clazz) {
    return createTestLog((Logger) LoggerFactory.getLogger(clazz));
  }

  public static TestLog audit() {
    return createTestLog((Logger) LoggerFactory.getLogger("AUDIT"));
  }

  private static @NotNull TestLog createTestLog(Logger log) {
    final var listAppender = new ListAppender<ILoggingEvent>();
    listAppender.start();
    log.addAppender(listAppender);
    return new TestLog(listAppender);
  }

  public boolean hasLogMessageContaining(String text) {
    return messages().anyMatch(message -> message.contains(text));
  }

  private @NotNull Stream<String> messages() {
    return logs.list.stream().map(ILoggingEvent::getFormattedMessage);
  }
}
