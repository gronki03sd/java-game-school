# Baccalauréat+

A modernized Scattergories-like desktop game built with Java 17, JavaFX, and SQLite.

## Requirements
- Java 17 or newer (JDK)
- Maven 3.8+
- Windows (tested), but cross-platform should work

## Run
```powershell
# From the project root
 mvn javafx:run
```

If you see JavaFX module errors, ensure JAVA_HOME points to JDK 17+ and that Maven uses the same JDK.

## Structure
- `pom.xml` — dependencies and JavaFX run plugin
- `src/main/java/com/baccalaureat` — code (MVC + services + DAO)
- `src/main/resources/com/baccalaureat` — FXML views

## Game Rules (Solo)
- A random letter is chosen.
- You have 60 seconds.
- Enter words for categories (Pays, Ville, Animal, Métier) starting with the letter.
- On Stop & Validate, each valid word gives 1 point.
- Validity is cached locally in SQLite. Mock API accepts any word length > 2.

## Notes
- SQLite DB file `baccalaureat.db` is created in the project directory.
- The service layer first checks the cache, then validates via a mock API and saves valid words.
