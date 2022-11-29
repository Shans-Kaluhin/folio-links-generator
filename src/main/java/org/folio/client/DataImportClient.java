package org.folio.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.folio.model.UploadDefinition;
import org.folio.util.HttpWorker;

import java.nio.file.Path;

import static org.folio.util.FileWorker.getJsonObject;

public class DataImportClient {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String UPLOAD_DEFINITION_BODY = "{\"fileDefinitions\":[{\"size\": 1,\"name\": \"%s\"}]}";
    private static final String UPLOAD_DEFINITION_PATH = "/data-import/uploadDefinitions";
    private static final String UPLOAD_DEFINITION_BY_ID_PATH = "/data-import/uploadDefinitions/%s";
    private static final String UPLOAD_FILE_PATH = UPLOAD_DEFINITION_PATH + "/%s/files/%s";
    private static final String UPLOAD_JOB_PROFILE_PATH = UPLOAD_DEFINITION_PATH + "/%s/processFiles?defaultMapping=false";
    private static final String JOB_EXECUTION_PATH = "/change-manager/jobExecutions/%s";

    private final HttpWorker httpWorker;

    public DataImportClient(HttpWorker httpWorker) {
        this.httpWorker = httpWorker;
    }

    @SneakyThrows
    public UploadDefinition uploadDefinition(Path filePath) {
        String body = String.format(UPLOAD_DEFINITION_BODY, filePath.getFileName());

        var request = httpWorker.constructPOSTRequest(UPLOAD_DEFINITION_PATH, body);
        var response = httpWorker.sendRequest(request);

        httpWorker.verifyStatus(response, 201, "Failed to upload definition");

        var jsonBody = OBJECT_MAPPER.readTree(response.body());
        var fileDefinitions = jsonBody.findValue("fileDefinitions");
        var uploadDefinitionId = fileDefinitions.findValue("uploadDefinitionId").asText();
        var jobExecutionId = fileDefinitions.findValue("jobExecutionId").asText();
        var fileId = fileDefinitions.findValue("id").asText();

        return new UploadDefinition(uploadDefinitionId, jobExecutionId, fileId, filePath);
    }

    @SneakyThrows
    public JsonNode getUploadDefinition(String uploadDefinitionId) {
        var uri = String.format(UPLOAD_DEFINITION_BY_ID_PATH, uploadDefinitionId);

        var request = httpWorker.constructGETRequest(uri);
        var response = httpWorker.sendRequest(request);

        httpWorker.verifyStatus(response, 200, "Failed to get upload definition");

        return OBJECT_MAPPER.readTree(response.body());
    }

    @SneakyThrows
    public void uploadFile(UploadDefinition uploadDefinition) {
        var uploadPath = String.format(UPLOAD_FILE_PATH, uploadDefinition.getUploadDefinitionId(), uploadDefinition.getFileId());

        var request = httpWorker.constructPOSTRequest(uploadPath, uploadDefinition.getFilePath());
        var response = httpWorker.sendRequest(request);

        httpWorker.verifyStatus(response, 200, "Failed to upload file");
    }

    public void uploadJobProfile(UploadDefinition uploadDefinition, String jobName) {
        var uploadPath = String.format(UPLOAD_JOB_PROFILE_PATH, uploadDefinition.getUploadDefinitionId());
        var jobProfile = getJsonObject("jobProfiles/" + jobName);

        jobProfile.set("uploadDefinition", getUploadDefinition(uploadDefinition.getUploadDefinitionId()));

        var request = httpWorker.constructPOSTRequest(uploadPath, jobProfile.toString());
        var response = httpWorker.sendRequest(request);

        httpWorker.verifyStatus(response, 204, "Failed to upload job profile");
    }

    @SneakyThrows
    public String getJobStatus(String jobId) {
        var getJobStatusPath = String.format(JOB_EXECUTION_PATH, jobId);

        var request = httpWorker.constructGETRequest(getJobStatusPath);
        var response = httpWorker.sendRequest(request);

        httpWorker.verifyStatus(response, 200, "Failed to fetching jo status");

        var jsonBody = OBJECT_MAPPER.readTree(response.body());

        return jsonBody.findValue("status").asText();
    }
}
