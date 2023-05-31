package org.folio.service;

import static org.folio.model.integration.JobStatus.CANCELLED;
import static org.folio.model.integration.JobStatus.COMMITTED;
import static org.folio.model.integration.JobStatus.DISCARDED;
import static org.folio.model.integration.JobStatus.ERROR;
import static org.folio.service.LinksGenerationService.progressBarBuilder;

import java.io.File;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import org.folio.client.DataImportClient;

@Slf4j
public class DataImportService {
  private static final String AUTHORITY_TITLE = "Import Authorities";
  private static final String BIB_TITLE = "Generating Bibs";
  private static final int STATUS_CHECK_INTERVAL = 2;
  private final DataImportClient dataImportClient;

  public DataImportService(DataImportClient dataImportClient) {
    this.dataImportClient = dataImportClient;
  }

  @SneakyThrows
  public String importAuthority(File authorityMrcFile) {
    var uploadDefinition = dataImportClient.uploadDefinition(authorityMrcFile.toPath());
    log.info("Import authority job id: " + uploadDefinition.getJobExecutionId());

    dataImportClient.uploadFile(uploadDefinition);
    dataImportClient.uploadJobProfile(uploadDefinition, "createAuthority.json");
    var jobId = uploadDefinition.getJobExecutionId();

    waitForJobFinishing(progressBarBuilder(AUTHORITY_TITLE).build(), jobId);

    return uploadDefinition.getJobExecutionId();
  }

  public String importBibs(File bibMrcFile) {
    var uploadDefinition = dataImportClient.uploadDefinition(bibMrcFile.toPath());
    log.info("Import bibs job id: " + uploadDefinition.getJobExecutionId());

    dataImportClient.uploadFile(uploadDefinition);
    dataImportClient.uploadJobProfile(uploadDefinition, "createInstance.json");
    var jobId = uploadDefinition.getJobExecutionId();

    waitForJobFinishing(progressBarBuilder(BIB_TITLE).build(), jobId);

    return jobId;
  }

  @SneakyThrows
  private void waitForJobFinishing(ProgressBar progressBar, String jobId) {
    var job = dataImportClient.retrieveJobExecution(jobId);
    progressBar.maxHint(job.getTotal());
    progressBar.stepTo(job.getCurrent());
    progressBar.setExtraMessage(job.getUiStatus());

    if (isJobFinished(job.getStatus())) {
      progressBar.close();
    } else {
      TimeUnit.SECONDS.sleep(STATUS_CHECK_INTERVAL);
      waitForJobFinishing(progressBar, jobId);
    }
  }

  private boolean isJobFinished(String status) {
    return COMMITTED.name().equals(status)
      || ERROR.name().equals(status)
      || CANCELLED.name().equals(status)
      || DISCARDED.name().equals(status);
  }
}
