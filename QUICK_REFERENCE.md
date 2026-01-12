# Quick Reference Card - DialogHelper & ImageLoader

## 🎨 DialogHelper - Styled Dialogs

### Import

```java
import com.baccalaureat.util.DialogHelper;
```

### Methods

#### Information Dialog

```java
DialogHelper.showInformation("Title", "Header", "Content text");
```

#### Error Dialog

```java
DialogHelper.showError("Erreur", "Header", "Error message");
```

#### Warning Dialog

```java
DialogHelper.showWarning("Attention", "Header", "Warning message");
```

#### Simple Confirmation (returns boolean)

```java
boolean confirmed = DialogHelper.showConfirmation(
    "Confirmer",
    "Êtes-vous sûr?",
    "Cette action ne peut pas être annulée."
);

if (confirmed) {
    // User clicked OK
}
```

#### Custom Button Confirmation

```java
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;

ButtonType yesBtn = new ButtonType("Oui", ButtonBar.ButtonData.YES);
ButtonType noBtn = new ButtonType("Non", ButtonBar.ButtonData.NO);

Optional<ButtonType> result = DialogHelper.showConfirmation(
    "Question",
    "Voulez-vous continuer?",
    "Choisissez une option",
    yesBtn, noBtn
);

if (result.isPresent() && result.get() == yesBtn) {
    // User clicked Yes
}
```

#### Advanced - Customizable Alert

```java
Alert alert = DialogHelper.createStyledAlert(
    Alert.AlertType.INFORMATION,
    "Title",
    "Header",
    "Content"
);

// Customize further
alert.setGraphic(customGraphic);
alert.getButtonTypes().setAll(customButtons);
alert.showAndWait();
```

---

## 🖼️ ImageLoader - JAR-Safe Image Loading

### Import

```java
import com.baccalaureat.util.ImageLoader;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
```

### Methods

#### Load Image

```java
Image img = ImageLoader.loadImage("/com/baccalaureat/images/logo.png");
```

#### Load Image with Size

```java
Image img = ImageLoader.loadImage(
    "/com/baccalaureat/icons/play.png",
    32,    // width
    32,    // height
    true,  // preserve ratio
    true   // smooth scaling
);
```

#### Create ImageView

```java
ImageView imgView = ImageLoader.loadImageView("/com/baccalaureat/images/banner.png");
```

#### Create ImageView with Fit Size

```java
ImageView icon = ImageLoader.loadImageView(
    "/com/baccalaureat/icons/settings.png",
    48,    // fitWidth
    48,    // fitHeight
    true   // preserve ratio
);
```

#### Check if Resource Exists

```java
if (ImageLoader.resourceExists("/com/baccalaureat/images/optional.png")) {
    Image img = ImageLoader.loadImage("/com/baccalaureat/images/optional.png");
} else {
    // Use fallback
}
```

---

## 📂 Resource Path Format

### ✅ CORRECT

```java
"/com/baccalaureat/images/logo.png"           // Absolute from classpath root
"/com/baccalaureat/icons/settings.png"         // Forward slashes, even on Windows
"/com/baccalaureat/MainMenu.fxml"              // Works for all resources
```

### ❌ WRONG

```java
"C:/Users/me/project/images/logo.png"          // Absolute file path
"images/logo.png"                              // Relative path
"logo.png"                                     // No path
"file:///C:/path/to/file.png"                  // File URL
```

---

## 📁 Directory Structure

```
src/main/resources/
└── com/baccalaureat/
    ├── styles.css              ← CSS files
    ├── MainMenu.fxml           ← FXML files
    ├── images/                 ← Place your images here
    │   ├── logo.png
    │   ├── background.jpg
    │   └── banner.png
    └── icons/                  ← Place your icons here
        ├── play.png
        ├── settings.png
        └── exit.png
```

---

## 🎨 Dialog Colors

| Type             | Header Color     | Border Color | Button Color   |
| ---------------- | ---------------- | ------------ | -------------- |
| **Information**  | #e8f5f2 (Mint)   | #4ecca3      | #4ecca3 (Mint) |
| **Error**        | #fee (Red)       | #e74c3c      | #e74c3c (Red)  |
| **Warning**      | #fff9e6 (Orange) | #ffa726      | #4ecca3 (Mint) |
| **Confirmation** | #e8f4fd (Blue)   | #3498db      | #3498db (Blue) |

---

## 📋 Common Patterns

### Replace Existing Alert

```java
// OLD
Alert alert = new Alert(Alert.AlertType.ERROR);
alert.setTitle("Error");
alert.setHeaderText("Something went wrong");
alert.setContentText(errorMessage);
alert.showAndWait();

// NEW
DialogHelper.showError("Error", "Something went wrong", errorMessage);
```

### Load and Display Image in UI

```java
// In controller
@FXML
private ImageView logoView;

@FXML
public void initialize() {
    Image logo = ImageLoader.loadImage("/com/baccalaureat/images/logo.png");
    if (logo != null) {
        logoView.setImage(logo);
    }
}
```

### Button with Icon

```java
Button btn = new Button("Settings");
ImageView icon = ImageLoader.loadImageView(
    "/com/baccalaureat/icons/settings.png",
    24, 24, true
);
btn.setGraphic(icon);
```

---

## ⚡ Quick Tips

1. **Always** use absolute paths starting with `/` for resources
2. **Never** use file system paths (C:/, D:/, etc.)
3. **Test** in JAR after adding images
4. **Use** DialogHelper for all alerts (consistency)
5. **Check** console for "Image not found" warnings
6. **Place** all assets in `src/main/resources/`
7. **Import** utilities at the top of your controllers
8. **Follow** naming conventions (lowercase-with-hyphens.png)

---

## 🔗 More Information

- **Full Guide**: `UI_MODERNIZATION_GUIDE.md`
- **Examples**: `DIALOG_MIGRATION_EXAMPLES.md`
- **Summary**: `IMPLEMENTATION_SUMMARY.md`
- **CSS**: `DIALOG_CSS_SNIPPETS.css`

---

**Print this card and keep it handy!** 📌
