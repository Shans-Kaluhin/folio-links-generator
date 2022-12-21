package org.folio.client;

import org.folio.model.Configuration;
import org.folio.util.HttpWorker;

import static org.folio.mapper.ResponseMapper.mapResponseToJson;

public class AuthClient {
    private static final String BODY_FORMAT = "{\"username\": \"%s\",\"password\": \"%s\"}";
    private static final String AUTH_PATH = "/authn/login";
    private final Configuration configuration;
    private final HttpWorker httpWorker;

    public AuthClient(Configuration configuration, HttpWorker httpWorker) {
        this.configuration = configuration;
        this.httpWorker = httpWorker;
    }

    public String authorize() {
        String body = String.format(BODY_FORMAT, configuration.getUsername(), configuration.getPassword());

        var request = httpWorker.constructPOSTRequest(AUTH_PATH, body);
        var response = httpWorker.sendRequest(request);

        httpWorker.verifyStatus(response, 201, "Failed to authorize user");

        return mapResponseToJson(response).findValue("okapiToken").asText();
    }
}
