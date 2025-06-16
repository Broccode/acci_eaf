# Favicon Setup Guide for ACCI EAF Launchpad

## Steps to Set Up Your Custom Capybara Favicon

### 1. Generate Favicon Files

1. Visit <https://realfavicongenerator.net/>
2. Upload `docs/static/img/capybara_coder_logo.png`
3. Configure settings as desired (defaults work well)
4. Download the generated favicon package

### 2. Extract and Place Files

Extract the downloaded zip file and place these files in `docs/static/`:

```
docs/static/
├── favicon.ico
├── favicon-16x16.png
├── favicon-32x32.png
├── apple-touch-icon.png
├── android-chrome-192x192.png
├── android-chrome-512x512.png
└── site.webmanifest
```

### 3. Verify Configuration

The Docusaurus configuration has been updated to include:

- Main favicon.ico
- PNG favicons for different sizes
- Apple touch icon for iOS devices
- Web app manifest for PWA support

### 4. Test Your Favicon

Start the development server:

```bash
nx serve docs
```

Then check:

- Browser tab should show your new favicon
- Bookmark the page to see favicon in bookmarks
- Test on mobile devices by adding to home screen

### 5. Clean Up

After verification, you can delete this guide file:

```bash
rm docs/setup-favicon.md
```

## File Placement Command

After downloading the favicon files, you can use this command to move them correctly:

```bash
# If your favicon files are in ~/Downloads/favicon_package/
mv ~/Downloads/favicon_package/* docs/static/
```

## Troubleshooting

- Make sure files are in `docs/static/` not `docs/static/img/`
- Clear browser cache if old favicon persists
- Check browser developer tools for 404 errors on favicon files
