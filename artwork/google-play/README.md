# Google Play artwork

`app-icon-512.png` is the ready-to-upload **Google Play app icon** for **ذِكر**.

- **Dimensions:** 512 × 512 px
- **Format:** PNG / RGB (fully opaque; no transparent pixels)
- **Artwork:** original deep-emerald background with the app's gold-and-ivory remembrance mark

In Play Console, upload this file in **Store settings → Main store listing → App icon**.

To recreate it after changing the brand geometry, run from the repository root:

```bash
python3 tools/build_launcher_icons.py
```

That command rebuilds both the Android launcher fallbacks and this Play Store file.
