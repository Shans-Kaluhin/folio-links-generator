package org.folio.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Configuration {
  boolean uniqueMarcBibs;
  private String okapiUrl;
  private String tenant;
  private String username;
  private String password;
  private List<BibsConfig> marcBibs;

  public record BibsConfig(int totalBibs, List<String> linkingFields) {
  }
}
