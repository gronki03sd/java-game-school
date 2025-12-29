package com.baccalaureat.model;

public enum Category {
    PAYS,
    VILLE,
    ANIMAL,
    METIER;

    public String displayName() {
        switch (this) {
            case PAYS:
                return "Pays";
            case VILLE:
                return "Ville";
            case ANIMAL:
                return "Animal";
            case METIER:
                return "Métier";
            default:
                return name();
        }
    }
}
