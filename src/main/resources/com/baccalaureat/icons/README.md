# Icons Directory

Place your icon files here (PNG, SVG preferred for scalability).

## Naming Conventions

- Use descriptive names: `settings.png`, `exit.png`, `play.png`
- Include size if necessary: `play-32.png`
- Use consistent style across all icons

## Recommended Icons

- **UI Controls**: play, pause, stop, next, previous, settings, exit
- **Game Actions**: validate, submit, skip, hint, timer
- **Multiplayer**: player, add-player, remove-player, crown (winner)
- **Status**: success, error, warning, info, pending
- **Navigation**: back, home, menu, close

## Icon Sizes

- Standard UI icons: 24x24, 32x32, 48x48
- Toolbar icons: 24x24
- Button icons: 32x32
- Large icons: 64x64, 128x128

## Loading Icons

Always use the ImageLoader utility:

```java
import com.baccalaureat.util.ImageLoader;

ImageView settingsIcon = ImageLoader.loadImageView("/com/baccalaureat/icons/settings.png", 32, 32, true);
Image playIcon = ImageLoader.loadImage("/com/baccalaureat/icons/play.png");
```

## Icon Libraries

Consider using:

- **Material Icons**: https://fonts.google.com/icons
- **Font Awesome**: https://fontawesome.com/
- **Feather Icons**: https://feathericons.com/
- **Heroicons**: https://heroicons.com/

## Current Usage

Currently, the application uses emoji icons (📝, 🎮, ⚙️, etc.) in labels.
You can replace these with proper icon images for a more professional look.
