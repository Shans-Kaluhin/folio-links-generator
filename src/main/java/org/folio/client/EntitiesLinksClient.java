package org.folio.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.folio.model.InstanceLinks;
import org.folio.util.HttpWorker;

import java.util.HashMap;

public class EntitiesLinksClient {

    private static final String GET_LINKING_RULES_PATH = "/linking-rules/instance-authority";
    private static final String EDIT_LINKS_PATH = "/links/instances/%s";
    private final HttpWorker httpWorker;

    public EntitiesLinksClient(HttpWorker httpWorker) {
        this.httpWorker = httpWorker;
    }

    @SneakyThrows
    public void link(String instance, InstanceLinks instanceLinks) {
        var uri = String.format(EDIT_LINKS_PATH, instance);
        var body = new ObjectMapper().writeValueAsString(instanceLinks);

        var request = httpWorker.constructPUTRequest(uri, body);
        var response = httpWorker.sendRequest(request);

        httpWorker.verifyStatus(response, 200, "Failed to link records");
    }

    public String getLinkedRules() {
        var request = httpWorker.constructGETRequest(GET_LINKING_RULES_PATH);
        var response = httpWorker.sendRequest(request);

        httpWorker.verifyStatus(response, 200, "Failed to get linking rules");

        return response.body();
    }
}
