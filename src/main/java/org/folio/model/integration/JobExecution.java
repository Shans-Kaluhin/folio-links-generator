package org.folio.model.integration;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class JobExecution {
    private String status;
    private String uiStatus;
    private int current;
    private int total;
}
