# UI Modernization Guide - DialogHelper & Image Loading

## Overview

This document describes the newly implemented UI modernization features for the JavaFX Baccalauréat+ application.

---

## 1. DialogHelper - Styled Alerts and Dialogs

### Purpose

The `DialogHelper` class provides a centralized way to create consistent, styled dialogs throughout the application. All alerts now automatically load the main CSS file and apply custom styling.

### Location

`src/main/java/com/baccalaureat/util/DialogHelper.java`

### Features

- ✅ Automatic CSS loading from `styles.css`
- ✅ Custom styling for each dialog type (Info, Error, Warning, Confirmation)
- ✅ Consistent appearance with the rest of the application
- ✅ Type-specific color schemes
- ✅ Smooth animations and hover effects

### Usage Examples

#### Information Dialog

```java
import com.baccalaureat.util.DialogHelper;

// Simple information message
DialogHelper.showInformation(
    "Success",
    "Operation Completed",
    "Your data has been saved successfully."
);
```

#### Error Dialog

```java
DialogHelper.showError(
    "Erreur",
    "Une erreur s'est produite",
    "Impossible de charger les données."
);
```

#### Warning Dialog

```java
DialogHelper.showWarning(
    "Attention",
    "Données non sauvegardées",
    "Vous avez des modifications non sauvegardées."
);
```

#### Confirmation Dialog (Simple)

```java
boolean confirmed = DialogHelper.showConfirmation(
    "Confirmation",
    "Êtes-vous sûr?",
    "Cette action est irréversible."
);

if (confirmed) {
    // User clicked OK
    performAction();
}
```

#### Confirmation Dialog (Custom Buttons)

```java
import javafx.scene.control.ButtonType;

ButtonType nextBtn = new ButtonType("Continuer", ButtonBar.ButtonData.OK_DONE);
ButtonType cancelBtn = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);

Optional<ButtonType> result = DialogHelper.showConfirmation(
    "Tour suivant",
    "Prêt pour le prochain tour?",
    "Cliquez sur Continuer pour passer au joueur suivant.",
    nextBtn, cancelBtn
);

if (result.isPresent() && result.get() == nextBtn) {
    // User clicked Continue
    nextTurn();
}
```

#### Advanced - Create Custom Alert

```java
Alert alert = DialogHelper.createStyledAlert(
    Alert.AlertType.INFORMATION,
    "Résultats",
    "Partie terminée!",
    "Score final: 150 points"
);

// Further customize the alert
alert.setGraphic(customGraphic);
alert.getButtonTypes().setAll(customButton1, customButton2);

// Show the dialog
alert.showAndWait();
```

### Migrating Existing Alerts

#### Before (Old Code):

```java
Alert alert = new Alert(Alert.AlertType.ERROR);
alert.setTitle("Erreur");
alert.setHeaderText("Une erreur s'est produite");
alert.setContentText(message);
alert.showAndWait();
```

#### After (Using DialogHelper):

```java
import com.baccalaureat.util.DialogHelper;

DialogHelper.showError("Erreur", "Une erreur s'est produite", message);
```

---

## 2. CSS Styling for Dialogs

### Location

`src/main/resources/com/baccalaureat/styles.css` (lines 745+)

### Custom Classes Applied

- `.custom-dialog` - Base styling for all dialogs
- `.info-dialog` - Green/mint theme for information
- `.error-dialog` - Red theme for errors
- `.warning-dialog` - Orange theme for warnings
- `.confirmation-dialog` - Blue theme for confirmations

### Styling Features

- **Modern rounded corners** with 16px radius
- **Type-specific header colors** matching dialog type
- **Smooth shadows** for depth
- **Hover effects** on buttons with scale animations
- **Consistent button styling** with primary and cancel variants

### Customization

You can further customize dialog appearance by modifying these CSS classes:

```css
/* Example: Change confirmation dialog accent color */
.confirmation-dialog .header-panel {
  -fx-background-color: #your-color;
  -fx-border-color: #your-border-color;
}

.confirmation-dialog .button-bar .button {
  -fx-background-color: #your-button-color;
}
```

---

## 3. ImageLoader - JAR-Safe Image Loading

### Purpose

The `ImageLoader` utility ensures images are loaded correctly both during development and when running from a JAR file.

### Location

`src/main/java/com/baccalaureat/util/ImageLoader.java`

### Best Practices

#### ✅ CORRECT - Use These Patterns

```java
import com.baccalaureat.util.ImageLoader;

// Load image from resources
Image logo = ImageLoader.loadImage("/com/baccalaureat/images/logo.png");

// Load image with specific dimensions
Image icon = ImageLoader.loadImage(
    "/com/baccalaureat/icons/settings.png",
    32, 32,    // width, height
    true,      // preserve ratio
    true       // smooth scaling
);

// Create ImageView directly
ImageView iconView = ImageLoader.loadImageView(
    "/com/baccalaureat/icons/play.png"
);

// Create ImageView with fit dimensions
ImageView thumbnail = ImageLoader.loadImageView(
    "/com/baccalaureat/images/thumbnail.png",
    100, 100,  // fitWidth, fitHeight
    true       // preserve ratio
);

// Check if resource exists before loading
if (ImageLoader.resourceExists("/com/baccalaureat/images/optional.png")) {
    Image img = ImageLoader.loadImage("/com/baccalaureat/images/optional.png");
}
```

#### ❌ WRONG - Never Use These Patterns

```java
// ❌ Absolute file system paths - WON'T WORK IN JAR
new Image("C:/Users/myuser/images/icon.png");
new Image("D:/project/resources/logo.png");

// ❌ Relative paths - UNRELIABLE IN JAR
new Image("images/icon.png");
new Image("../resources/logo.png");

// ❌ File URLs - WON'T WORK IN JAR
new Image("file:///C:/path/to/file.png");
new Image("file://D:/images/icon.png");

// ❌ Direct file loading - WON'T WORK IN JAR
new Image(new FileInputStream("logo.png"));
```

### Resource Directory Structure

Place all images in the resources directory:

```
src/main/resources/
└── com/
    └── baccalaureat/
        ├── styles.css
        ├── images/
        │   ├── logo.png
        │   ├── background.jpg
        │   └── banner.png
        ├── icons/
        │   ├── settings.png
        │   ├── play.png
        │   ├── pause.png
        │   └── exit.png
        └── sounds/
            ├── click.wav
            └── success.mp3
```

### Path Format

**Always use absolute paths from the classpath root:**

- Start with `/` to indicate root of classpath
- Use forward slashes `/` (even on Windows)
- Include package structure: `/com/baccalaureat/...`
- Include file extension

**Examples:**

```
✅ "/com/baccalaureat/images/logo.png"
✅ "/com/baccalaureat/icons/settings.png"
❌ "images/logo.png"
❌ "logo.png"
❌ "C:/project/src/main/resources/images/logo.png"
```

---

## 4. Implementation Checklist

### For Adding New Images

1. **Place image file** in `src/main/resources/com/baccalaureat/images/` or appropriate subdirectory
2. **Load using ImageLoader**:
   ```java
   Image img = ImageLoader.loadImage("/com/baccalaureat/images/myimage.png");
   ```
3. **Test in development** mode
4. **Build JAR** and test that images load correctly
5. **Check console** for any "Image not found" warnings

### For Migrating Existing Dialogs

1. **Import DialogHelper**:
   ```java
   import com.baccalaureat.util.DialogHelper;
   ```
2. **Replace Alert creation** with DialogHelper methods
3. **Remove manual CSS loading** (DialogHelper does this automatically)
4. **Test dialog appearance** matches application theme

---

## 5. Current Project Status

### ✅ Completed

- DialogHelper utility class created with full styling support
- CSS styles for dialogs added to styles.css
- ImageLoader utility class created for JAR-safe image loading
- Comprehensive documentation provided

### 📝 No Issues Found

- All existing resource loading already uses proper `getClass().getResource()` patterns
- No broken absolute file paths detected
- FXML and CSS files are loaded correctly
- Project currently uses emoji icons (no image files yet)

### 🎯 Ready for Future Development

- ImageLoader utility ready for when image assets are added
- All resource loading patterns are JAR-safe
- Dialog system ready to use throughout application

---

## 6. Testing Recommendations

### Test Dialogs

```java
// In any controller, test the new dialogs:
@FXML
private void testDialogs() {
    // Test info dialog
    DialogHelper.showInformation("Test", "Information Dialog", "This is a test message.");

    // Test error dialog
    DialogHelper.showError("Test", "Error Dialog", "This is an error message.");

    // Test warning dialog
    DialogHelper.showWarning("Test", "Warning Dialog", "This is a warning message.");

    // Test confirmation
    boolean confirmed = DialogHelper.showConfirmation(
        "Test",
        "Confirmation Dialog",
        "Do you want to proceed?"
    );
    System.out.println("User confirmed: " + confirmed);
}
```

### Build and Test JAR

```bash
# Build the project
mvn clean package

# Run the JAR
java -jar target/baccalaureat-1.0-SNAPSHOT.jar

# Verify:
# ✓ Dialogs appear styled correctly
# ✓ Images load (when added)
# ✓ No resource loading errors in console
```

---

## 7. Additional Resources

### JavaFX Documentation

- [Alert Dialog](https://openjfx.io/javadoc/17/javafx.controls/javafx/scene/control/Alert.html)
- [CSS Reference Guide](https://openjfx.io/javadoc/17/javafx.graphics/javafx/scene/doc-files/cssref.html)
- [Image Loading](https://openjfx.io/javadoc/17/javafx.graphics/javafx/scene/image/Image.html)

### Project Files

- Dialog Utility: [DialogHelper.java](src/main/java/com/baccalaureat/util/DialogHelper.java)
- Image Utility: [ImageLoader.java](src/main/java/com/baccalaureat/util/ImageLoader.java)
- Main Stylesheet: [styles.css](src/main/resources/com/baccalaureat/styles.css)

---

**Version:** 1.0  
**Last Updated:** January 12, 2026  
**Author:** GitHub Copilot
