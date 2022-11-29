package org.folio.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;

@Getter
@Setter
@AllArgsConstructor
public class UploadDefinition {
    private String uploadDefinitionId;
    private String jobExecutionId;
    private String fileId;
    private Path filePath;
}
