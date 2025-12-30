package com.baccalaureat.model;

public enum Category {
    PAYS("Pays", "🌍", "Un pays du monde"),
    VILLE("Ville", "🏙️", "Une ville"),
    ANIMAL("Animal", "🐾", "Un animal"),
    METIER("Métier", "👔", "Une profession"),
    PRENOM("Prénom", "👤", "Un prénom"),
    FRUIT("Fruit/Légume", "🍎", "Un fruit ou légume"),
    OBJET("Objet", "📦", "Un objet du quotidien"),
    CELEBRITE("Célébrité", "⭐", "Une personne célèbre");

    private final String displayName;
    private final String icon;
    private final String hint;

    Category(String displayName, String icon, String hint) {
        this.displayName = displayName;
        this.icon = icon;
        this.hint = hint;
    }

    public String displayName() {
        return displayName;
    }

    public String getIcon() {
        return icon;
    }

    public String getHint() {
        return hint;
    }
}
