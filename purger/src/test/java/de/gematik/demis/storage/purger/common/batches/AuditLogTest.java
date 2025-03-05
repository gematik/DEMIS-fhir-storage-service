package de.gematik.demis.storage.purger.common.batches;

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

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.gematik.demis.storage.purger.test.TestLog;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
class AuditLogTest {

  private static final int BATCH_SIZE = 3;
  private static final int BATCH_LIMIT = 5;
  private static final String POD_NAME = "pod-159357";
  private static final String RESOURCE = "binaries";
  private static final String DURATION_EMPTY = "";

  @Mock private BatchesConfiguration batchesConfiguration;
  @Mock private Logger log;
  private AuditLog auditLog;
  private TestLog auditLogs;

  @BeforeEach
  void setAuditLog() {
    auditLog = new AuditLog(batchesConfiguration, log, POD_NAME, RESOURCE);
    auditLogs = TestLog.audit();
  }

  @Test
  void givenDefaultValuesWhenStartThenLogInfo() {
    mockBatchesConfiguration();
    auditLog.start();
    verify(log)
        .info(
            "Starting purging {} based on batches. Pod: {} BatchSize: {} BatchLimit: {}",
            RESOURCE,
            POD_NAME,
            BATCH_SIZE,
            BATCH_LIMIT);
  }

  @Test
  void givenZeroWhenEndOnCompletionThenLogInfo() {
    mockInfoLevel();
    auditLog.endOnCompletion(0);
    verify(log).info("Ended purging {}. No resources found to purge.", RESOURCE);
  }

  @Test
  void givenTotalWhenEndOnCompletionThenLogInfo() {
    mockInfoLevel();
    auditLog.endOnCompletion(8);
    verify(log, times(1))
        .info(
            "Ended purging {} by reaching end of expired records. Total: {} Duration: {}",
            RESOURCE,
            8,
            DURATION_EMPTY);
  }

  @Test
  void givenTotalWhenEndOnBatchLimitThenLogInfo() {
    mockBatchesConfiguration();
    mockInfoLevel();
    auditLog.endOnBatchLimit(8);
    verify(log)
        .info(
            "Ended purging {} by batch limit. Total: {} Duration: {} BatchSize: {} BatchLimit: {}",
            RESOURCE,
            8,
            DURATION_EMPTY,
            BATCH_SIZE,
            BATCH_LIMIT);
  }

  @Test
  void givenBatchWhenStartBatchThenLogInfo() {
    auditLog.startBatch("1");
    verify(log).info("Starting batch purging {}. Batch: {}", RESOURCE, "1");
  }

  @Test
  void givenBatchAndIdsWhenEndBatchThenLogInfo() {
    mockInfoLevel();
    auditLog.endBatch("1", List.of("1", "2", "3"), List.of("4"));
    verify(log, times(1)).isInfoEnabled();
    verify(log)
        .info(
            "Completed batch purging of {}. Batch: {} Deleted: {} Failed: {}",
            "binaries",
            "1",
            3,
            1);
    auditLogs.hasLogMessageContaining(
        "Purger deleted FHIR: binaries! Batch: 1 Block: 1 DeletedIds: 1, 2, 3");
  }

  private void mockInfoLevel() {
    when(log.isInfoEnabled()).thenReturn(true);
  }

  private void mockBatchesConfiguration() {
    when(batchesConfiguration.size()).thenReturn(BATCH_SIZE);
    when(batchesConfiguration.limit()).thenReturn(BATCH_LIMIT);
  }
}
