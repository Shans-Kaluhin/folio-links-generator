package org.folio.model;

public enum RecordType {
    MARC_BIB("instance"),
    MARC_AUTHORITY("authority");

    private String externalIdName;

    RecordType(String externalIdName) {
        this.externalIdName = externalIdName;
    }

    public String getExternalIdName() {
        return externalIdName;
    }
}
