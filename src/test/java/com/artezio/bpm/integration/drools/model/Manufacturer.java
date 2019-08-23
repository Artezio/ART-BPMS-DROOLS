package com.artezio.bpm.integration.drools.model;

import java.io.Serializable;

public class Manufacturer implements Serializable {
    private String name;
    private String legalAddress;

    public Manufacturer() {
    }

    public Manufacturer(String name, String legalAddress) {
        this.name = name;
        this.legalAddress = legalAddress;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLegalAddress() {
        return legalAddress;
    }

    public void setLegalAddress(String legalAddress) {
        this.legalAddress = legalAddress;
    }
}
