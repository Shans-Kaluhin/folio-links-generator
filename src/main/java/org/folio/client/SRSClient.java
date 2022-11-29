package org.folio.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.folio.model.ExternalIdsHolder;
import org.folio.model.RecordType;
import org.folio.util.HttpWorker;

import java.util.ArrayList;
import java.util.List;

public class SRSClient {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String GET_RECORDS_PATH = "/source-storage/records?snapshotId=%s&recordType=%s&limit=%s";
    private static final String LIMIT = "5";
    private final HttpWorker httpWorker;

    public SRSClient(HttpWorker httpWorker) {
        this.httpWorker = httpWorker;
    }

    @SneakyThrows
    public List<ExternalIdsHolder> retrieveExternalIdsHolders(String jobId, RecordType recordType) {
        String uri = String.format(GET_RECORDS_PATH, jobId, recordType, LIMIT);

        var request = httpWorker.constructGETRequest(uri);
        var response = httpWorker.sendRequest(request);

        httpWorker.verifyStatus(response, 200, "Failed to get records ids by jobId");

        var externalIds = new ArrayList<ExternalIdsHolder>();
        var jsonBody = OBJECT_MAPPER.readTree(response.body());
        var totalRecords = jsonBody.findValue("totalRecords").asText();
        var records = jsonBody.findValue("records");

        for (JsonNode record : records) {
            var idsHolder = ExternalIdsHolder.map(record, recordType);
            externalIds.add(idsHolder);
        }

        return externalIds;
    }
}
