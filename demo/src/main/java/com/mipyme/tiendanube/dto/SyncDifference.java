package com.mipyme.tiendanube.dto;

public class SyncDifference {
    private String field;
    private String level;
    private String identifier;
    private String localValue;
    private String remoteValue;
    private String resolution;
    private String label;

    public SyncDifference() {}

    public SyncDifference(String field, String level, String identifier, String localValue, String remoteValue) {
        this.field = field;
        this.level = level;
        this.identifier = identifier;
        this.localValue = localValue;
        this.remoteValue = remoteValue;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getLocalValue() {
        return localValue;
    }

    public void setLocalValue(String localValue) {
        this.localValue = localValue;
    }

    public String getRemoteValue() {
        return remoteValue;
    }

    public void setRemoteValue(String remoteValue) {
        this.remoteValue = remoteValue;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }
}
