# Images Directory

Place your image files here (PNG, JPG, GIF, SVG).

## Naming Conventions

- Use lowercase with hyphens: `logo-main.png`
- Be descriptive: `button-play-icon.png` instead of `btn1.png`
- Include size in name if multiple versions: `logo-32x32.png`, `logo-64x64.png`

## Recommended Sizes

- **Icons**: 16x16, 24x24, 32x32, 48x48, 64x64
- **Logos**: 128x128, 256x256
- **Backgrounds**: 1920x1080 or larger
- **Buttons**: 100x40 or as needed

## Loading Images

Always use the ImageLoader utility:

```java
import com.baccalaureat.util.ImageLoader;

Image logo = ImageLoader.loadImage("/com/baccalaureat/images/logo.png");
ImageView icon = ImageLoader.loadImageView("/com/baccalaureat/images/button-play.png");
```

## Example Files (to be added)

- logo.png - Main application logo
- background.jpg - Main menu background
- banner.png - Game banner
- splash.png - Splash screen
