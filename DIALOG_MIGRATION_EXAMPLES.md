# Dialog Migration Example

This file demonstrates how to migrate existing Alert code to use the new DialogHelper utility.

## Example 1: MultiplayerLobbyController

### Before (Current Code)

```java
package com.baccalaureat.controller;

import javafx.scene.control.Alert;
// ... other imports

public class MultiplayerLobbyController {

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText("Une erreur s'est produite");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
```

### After (Using DialogHelper)

```java
package com.baccalaureat.controller;

import com.baccalaureat.util.DialogHelper;
// ... other imports
// Note: Remove "import javafx.scene.control.Alert;" if no longer needed

public class MultiplayerLobbyController {

    private void showError(String message) {
        DialogHelper.showError("Erreur", "Une erreur s'est produite", message);
    }
}
```

## Example 2: MultiplayerGameController - Custom Button Confirmation

### Before (Current Code)

```java
Alert alert = new Alert(Alert.AlertType.INFORMATION);
alert.setTitle("Tour terminé");
alert.setHeaderText(player.getName() + (points > 0 ? " - Bravo! 🎉" : " - Dommage! 😅"));
alert.setContentText("Points gagnés: +" + points + "\nScore total: " + player.getScore());

ButtonType nextBtn = new ButtonType("Joueur suivant", ButtonBar.ButtonData.OK_DONE);
alert.getButtonTypes().setAll(nextBtn);
alert.showAndWait();
```

### After (Using DialogHelper)

```java
import com.baccalaureat.util.DialogHelper;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;

ButtonType nextBtn = new ButtonType("Joueur suivant", ButtonBar.ButtonData.OK_DONE);
DialogHelper.showConfirmation(
    "Tour terminé",
    player.getName() + (points > 0 ? " - Bravo! 🎉" : " - Dommage! 😅"),
    "Points gagnés: +" + points + "\nScore total: " + player.getScore(),
    nextBtn
);
```

## Example 3: GameController - Confirmation Dialog

### Before (Current Code)

```java
Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
confirm.setTitle("Quitter");
confirm.setHeaderText("Voulez-vous vraiment quitter?");
confirm.setContentText("Votre progression sera perdue.");

Optional<ButtonType> result = confirm.showAndWait();
if (result.isPresent() && result.get() == ButtonType.OK) {
    // User confirmed
    exitGame();
}
```

### After (Using DialogHelper)

```java
import com.baccalaureat.util.DialogHelper;

boolean confirmed = DialogHelper.showConfirmation(
    "Quitter",
    "Voulez-vous vraiment quitter?",
    "Votre progression sera perdue."
);

if (confirmed) {
    exitGame();
}
```

## Example 4: MainMenuController - Simple Information

### Before (Current Code)

```java
Alert alert = new Alert(Alert.AlertType.INFORMATION);
alert.setTitle("À propos");
alert.setHeaderText("Baccalauréat+");
alert.setContentText("Version 1.0\nDéveloppé par ...");
alert.showAndWait();
```

### After (Using DialogHelper)

```java
import com.baccalaureat.util.DialogHelper;

DialogHelper.showInformation(
    "À propos",
    "Baccalauréat+",
    "Version 1.0\nDéveloppé par ..."
);
```

## Migration Steps

1. **Add import** for DialogHelper:

   ```java
   import com.baccalaureat.util.DialogHelper;
   ```

2. **Replace Alert creation** with appropriate DialogHelper method:

   - `Alert.AlertType.INFORMATION` → `DialogHelper.showInformation(...)`
   - `Alert.AlertType.ERROR` → `DialogHelper.showError(...)`
   - `Alert.AlertType.WARNING` → `DialogHelper.showWarning(...)`
   - `Alert.AlertType.CONFIRMATION` → `DialogHelper.showConfirmation(...)`

3. **Remove unused imports** if Alert is no longer used:

   ```java
   // Remove this if not needed:
   import javafx.scene.control.Alert;
   ```

4. **Test** the dialog to ensure it displays correctly with styling

## Benefits of Migration

✅ **Automatic Styling**: All dialogs automatically match your application theme  
✅ **Less Code**: Simpler, more readable code  
✅ **Consistency**: All dialogs look and behave the same way  
✅ **Maintainability**: Easier to update dialog styling globally  
✅ **No CSS Loading**: DialogHelper handles CSS loading automatically

## Visual Differences

### Before

- Plain OS-default dialog appearance
- Gray/system colors
- No custom styling
- Doesn't match application theme

### After

- Modern rounded corners (16px radius)
- Type-specific color schemes:
  - **Info**: Mint green header (#e8f5f2)
  - **Error**: Red header (#fee)
  - **Warning**: Orange header (#fff9e6)
  - **Confirmation**: Blue header (#e8f4fd)
- Smooth shadows and hover effects
- Matches application's Baccalauréat+ theme
- Styled buttons with animations
