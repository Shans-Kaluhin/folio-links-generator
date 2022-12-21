package org.folio.client;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.folio.model.RecordType;
import org.folio.model.integration.ExternalIdsHolder;
import org.folio.util.HttpWorker;

import java.util.ArrayList;
import java.util.List;

import static org.folio.mapper.ResponseMapper.mapRecordsToExternalIds;
import static org.folio.mapper.ResponseMapper.mapResponseToJson;

@Slf4j
public class SRSClient {
    private static final String GET_RECORDS_PATH = "/source-storage/records?snapshotId=%s&recordType=%s&limit=%s&offset=%s";
    private static final String LIMIT = "1000";
    private final HttpWorker httpWorker;

    public SRSClient(HttpWorker httpWorker) {
        this.httpWorker = httpWorker;
    }

    @SneakyThrows
    public List<ExternalIdsHolder> retrieveExternalIdsHolders(String jobId, RecordType recordType) {
        var totalHolders = new ArrayList<ExternalIdsHolder>();
        int totalRecords = retrieveTotalRecords(jobId, recordType);

        while (totalRecords > totalHolders.size()) {
            totalHolders.addAll(retrieveExternalIdsHolders(jobId, recordType, totalHolders.size()));
            log.info("Retrieving {} records: {} of {}", recordType, totalHolders.size(), totalRecords);
        }

        return totalHolders;
    }

    @SneakyThrows
    private List<ExternalIdsHolder> retrieveExternalIdsHolders(String jobId, RecordType recordType, int offset) {
        String uri = String.format(GET_RECORDS_PATH, jobId, recordType, LIMIT, offset);

        var request = httpWorker.constructGETRequest(uri);
        var response = httpWorker.sendRequest(request);

        httpWorker.verifyStatus(response, 200, "Failed to get records ids by jobId");

        return mapRecordsToExternalIds(response.body(), recordType);
    }

    @SneakyThrows
    private int retrieveTotalRecords(String jobId, RecordType recordType) {
        String uri = String.format(GET_RECORDS_PATH, jobId, recordType, 0, 0);

        var request = httpWorker.constructGETRequest(uri);
        var response = httpWorker.sendRequest(request);

        httpWorker.verifyStatus(response, 200, "Failed to get records ids by jobId");

        return mapResponseToJson(response).findValue("totalRecords").asInt();
    }
}
