package org.folio.service;

import lombok.SneakyThrows;
import org.folio.client.DataImportClient;
import org.folio.model.integration.JobStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.folio.model.integration.JobStatus.CANCELLED;
import static org.folio.model.integration.JobStatus.COMMITTED;
import static org.folio.model.integration.JobStatus.ERROR;
import static org.folio.model.integration.JobStatus.FILE_UPLOADED;

public class DataImportService {
    private static final Logger LOG = LoggerFactory.getLogger(DataImportService.class);
    private final DataImportClient dataImportClient;

    public DataImportService(DataImportClient dataImportClient) {
        this.dataImportClient = dataImportClient;
    }

    @SneakyThrows
    public String importAuthority(File authorityMrcFile) {
        var uploadDefinition = dataImportClient.uploadDefinition(authorityMrcFile.toPath());
        LOG.info("Import authority job id: " + uploadDefinition.getJobExecutionId());

        dataImportClient.uploadFile(uploadDefinition);
        waitStatus(uploadDefinition.getJobExecutionId(), FILE_UPLOADED);

        dataImportClient.uploadJobProfile(uploadDefinition, "createAuthority.json");
        waitStatus(uploadDefinition.getJobExecutionId(), COMMITTED);

        return uploadDefinition.getJobExecutionId();
    }

    @SneakyThrows
    public String importBibs(Path bibMrcFile) {
        var uploadDefinition = dataImportClient.uploadDefinition(bibMrcFile);
        LOG.info("Import bibs job id: " + uploadDefinition.getJobExecutionId());

        dataImportClient.uploadFile(uploadDefinition);
        waitStatus(uploadDefinition.getJobExecutionId(), FILE_UPLOADED);

        dataImportClient.uploadJobProfile(uploadDefinition, "createInstance.json");
        waitStatus(uploadDefinition.getJobExecutionId(), COMMITTED);

        return uploadDefinition.getJobExecutionId();
    }

    @SneakyThrows
    private String waitStatus(String jobId, JobStatus expectedStatus) {
        var status = dataImportClient.getJobStatus(jobId);
        LOG.info("Import job status: " + status);
        if (status.equals(expectedStatus.name())
                || status.equals(ERROR.name())
                || status.equals(CANCELLED.name())) {
            return status;
        } else {
            TimeUnit.SECONDS.sleep(20);
            return waitStatus(jobId, expectedStatus);
        }
    }
}
