package com.redcatdev86.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CareerSave implements Serializable {

    private static final long serialVersionUID = 8277756666798481988L;
    private String name;
    private List<Injury> injuries = new ArrayList<>();

    public CareerSave() {
        // Required for JSON
    }

    public CareerSave(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Injury> getInjuries() {
        return injuries;
    }

    public void setInjuries(List<Injury> injuries) {
        this.injuries = injuries;
    }

    @Override
    public String toString() {
        return name; // important for ComboBox display
    }
}
