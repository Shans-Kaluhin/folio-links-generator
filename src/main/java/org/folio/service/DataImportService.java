package org.folio.service;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import org.folio.client.DataImportClient;

import java.io.File;
import java.util.concurrent.TimeUnit;

import static org.folio.model.integration.JobStatus.CANCELLED;
import static org.folio.model.integration.JobStatus.COMMITTED;
import static org.folio.model.integration.JobStatus.DISCARDED;
import static org.folio.model.integration.JobStatus.ERROR;

@Slf4j
public class DataImportService {
    private static final String STATUS_BAR_TITLE = "IMPORT-PROGRESS-BAR  INFO --- [main] org.folio.service.DataImportService      : ";
    private static final String AUTHORITY_TITLE = "Import Authorities";
    private static final String BIB_TITLE = "Generating Bibs";
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

        waitForJobFinishing(buildProgressBar(AUTHORITY_TITLE), jobId);

        return uploadDefinition.getJobExecutionId();
    }

    public String importBibs(File bibMrcFile) {
        var uploadDefinition = dataImportClient.uploadDefinition(bibMrcFile.toPath());
        log.info("Import bibs job id: " + uploadDefinition.getJobExecutionId());

        dataImportClient.uploadFile(uploadDefinition);
        dataImportClient.uploadJobProfile(uploadDefinition, "createInstance.json");
        var jobId = uploadDefinition.getJobExecutionId();

        waitForJobFinishing(buildProgressBar(BIB_TITLE), jobId);

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
            TimeUnit.SECONDS.sleep(20);
            waitForJobFinishing(progressBar, jobId);
        }
    }

    private boolean isJobFinished(String status) {
        return COMMITTED.name().equals(status)
                || ERROR.name().equals(status)
                || CANCELLED.name().equals(status)
                || DISCARDED.name().equals(status);
    }

    private ProgressBar buildProgressBar(String title) {
        return new ProgressBarBuilder()
                .setTaskName(STATUS_BAR_TITLE + title)
                .setStyle(ProgressBarStyle.ASCII)
                .setMaxRenderedLength(STATUS_BAR_TITLE.length() + 70)
                .build();
    }
}
