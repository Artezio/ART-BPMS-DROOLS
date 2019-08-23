package com.artezio.bpm.integration.drools.model;

import java.io.Serializable;

public class Item implements Serializable {
    private Double price;
    private String name;
    private Manufacturer manufacturer;

    public Item() {
    }

    public Item(Double price, String name, Manufacturer manufacturer) {
        this.price = price;
        this.name = name;
        this.manufacturer = manufacturer;
    }

    public Manufacturer getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(Manufacturer manufacturer) {
        this.manufacturer = manufacturer;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
