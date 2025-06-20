# Ticket Management Service

## Chrome DevTools Integration

This project includes integration with Chrome DevTools for enhanced development experience.

### Setup

The project uses the
[vite-plugin-devtools-json](https://github.com/ChromeDevTools/vite-plugin-devtools-json) plugin to
enable seamless integration with Chrome DevTools features:

1. DevTools Project Settings (devtools.json)
2. Automatic Workspace folders

### Installation

To install the required dependencies:

```bash
cd src/main/frontend
npm install vite-plugin-devtools-json --save-dev
```

### Configuration

The plugin is already configured in `src/main/frontend/vite.config.ts` with a custom UUID for the
workspace identification.

### Usage

Once installed and configured, the plugin will:

- Generate `/.well-known/appspecific/com.chrome.devtools.json` endpoint
- Serve project settings as JSON with workspace root and UUID
- Enable automatic workspace folder detection in Chrome DevTools

### DevTools Settings

The plugin serves project settings with the following structure:

```json
{
  "workspace": {
    "root": "/path/to/project/root",
    "uuid": "ticket-management-service-dev-workspace"
  }
}
```

This enables Chrome DevTools to automatically detect and configure the workspace for debugging and
development.
