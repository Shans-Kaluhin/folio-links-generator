package org.folio.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Configuration {
    private String okapiUrl;
    private String tenant;
    private String username;
    private String password;
    private String token;
    private List<BibsConfig> marcBibs;

    public record BibsConfig (int totalBibs, List<String> linkingFields) {}
}
