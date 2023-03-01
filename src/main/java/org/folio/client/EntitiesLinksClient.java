package org.folio.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.folio.model.RecordType;
import org.folio.model.integration.InstanceLinks;
import org.folio.model.integration.LinkingRule;
import org.folio.util.HttpWorker;

import java.util.HashMap;
import java.util.List;

import static org.folio.mapper.ResponseMapper.mapRules;

@Slf4j
public class EntitiesLinksClient {
    private static final String GET_LINKING_RULES_PATH = "/linking-rules/instance-authority";
    private static final String INSTANCE_LINKS_PATH = "/links/instances/%s";
    private final HashMap<RecordType, HashMap<String, List<LinkingRule>>> rules = new HashMap<>();
    private final HttpWorker httpWorker;

    public EntitiesLinksClient(HttpWorker httpWorker) {
        this.httpWorker = httpWorker;
    }

    @SneakyThrows
    public void link(String instanceId, InstanceLinks instanceLinks) {
        var uri = String.format(INSTANCE_LINKS_PATH, instanceId);
        var body = new ObjectMapper().writeValueAsString(instanceLinks);

        var request = httpWorker.constructPUTRequest(uri, body);
        var response = httpWorker.sendRequest(request);

        httpWorker.verifyStatus(response, 204, "Failed to link records");
    }

    @SneakyThrows
    public HashMap<String, List<LinkingRule>> getLinkedRules(RecordType recordType) {
        if (rules.isEmpty()) {
            log.info("Retrieving linking rules...");
            var request = httpWorker.constructGETRequest(GET_LINKING_RULES_PATH);
            var response = httpWorker.sendRequest(request);

            httpWorker.verifyStatus(response, 200, "Failed to get linking rules");

            var json = response.body();
            rules.put(RecordType.MARC_BIB, mapRules(json, RecordType.MARC_BIB));
            rules.put(RecordType.MARC_AUTHORITY, mapRules(json, RecordType.MARC_AUTHORITY));
        }
        return rules.get(recordType);
    }
}
