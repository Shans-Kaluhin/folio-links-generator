package org.folio.model.integration;

import java.nio.file.Path;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class UploadDefinition {
  private String uploadDefinitionId;
  private String jobExecutionId;
  private String fileId;
  private Path filePath;
}
