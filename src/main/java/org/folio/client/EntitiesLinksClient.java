package org.folio.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import lombok.SneakyThrows;
import org.folio.model.integration.InstanceLinks;
import org.folio.util.HttpWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntitiesLinksClient {
    private static final Logger LOG = LoggerFactory.getLogger(EntitiesLinksClient.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String GET_LINKING_RULES_PATH = "/linking-rules/instance-authority";
    private static final String INSTANCE_LINKS_PATH = "/links/instances/%s";
    private final HttpWorker httpWorker;

    public EntitiesLinksClient(HttpWorker httpWorker) {
        this.httpWorker = httpWorker;
    }

    @SneakyThrows
    public void appendLinks(String instanceId, InstanceLinks newLinks) {
        var uri = String.format(INSTANCE_LINKS_PATH, instanceId);

        var request = httpWorker.constructGETRequest(uri);
        var response = httpWorker.sendRequest(request);

        httpWorker.verifyStatus(response, 200, "Failed to get linking rules");

        var oldLinks = new Gson().fromJson(response.body(), InstanceLinks.class);
        newLinks.getLinks().addAll(oldLinks.getLinks());

        link(instanceId, newLinks);
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
    public JsonNode getLinkedRules() {
        LOG.info("Retrieving linking rules...");
        var request = httpWorker.constructGETRequest(GET_LINKING_RULES_PATH);
        var response = httpWorker.sendRequest(request);

        httpWorker.verifyStatus(response, 200, "Failed to get linking rules");

        return OBJECT_MAPPER.readTree(response.body());
    }
}
