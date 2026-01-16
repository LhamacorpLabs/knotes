# kNotes Desktop App

A cross-platform desktop version of kNotes that connects to your hosted API at https://notes.lhamacorp.com.

## Prerequisites

- [Node.js](https://nodejs.org/) (version 16 or higher)
- npm (comes with Node.js)

## Installation

1. Navigate to the desktop-app directory:
   ```bash
   cd desktop-app
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

## Running in Development

To run the app in development mode:
```bash
npm start
```

## Building for Distribution

### Build for All Platforms (requires appropriate OS)
```bash
npm run build-all
```

### Build for Specific Platforms

**Windows:**
```bash
npm run build-win
```

**macOS:**
```bash
npm run build-mac
```

**Linux:**
```bash
npm run build-linux
```

### Build Output

Built applications will be available in the `dist/` directory:
- **Windows**: `.exe` installer and portable app
- **macOS**: `.dmg` installer and `.app` file
- **Linux**: `.AppImage` portable executable

## Features

- 🖥️ Native desktop app experience
- 🌐 Connects to your hosted kNotes API
- 🌙 Dark/Light theme support
- ⌨️ Keyboard shortcuts:
  - `Ctrl/Cmd + N` - New Note
  - `Ctrl/Cmd + H` - Home
  - `Ctrl/Cmd + Q` - Quit
- 📱 Responsive interface
- 💾 Auto-save functionality
- 🔄 Real-time conflict detection
- 🔄 **Automatic Updates** - App updates automatically when frontend changes

## Architecture

The desktop app is built with:
- **Electron** - Cross-platform desktop framework
- **Vanilla JavaScript** - No additional framework dependencies
- **Native HTML/CSS** - Clean, fast interface

The app acts as a desktop wrapper around your existing frontend, making HTTP requests to your deployed API at `https://notes.lhamacorp.com/api/notes`.

## Automatic Updates

The desktop app includes automatic update functionality:

### How It Works
1. **Frontend Changes Detected** - When you push changes to the main branch that affect `src/main/resources/static/`, GitHub Actions automatically builds new desktop app versions
2. **GitHub Releases Created** - New versions are published to GitHub Releases with cross-platform binaries
3. **Automatic Notifications** - Desktop app checks for updates every time it starts and notifies users when updates are available
4. **Background Downloads** - Updates download automatically in the background
5. **One-Click Installation** - Users can restart to apply updates with a single click

### Manual Update Check
Users can also manually check for updates via: **File Menu → Check for Updates**

### Update Process
- ✅ Non-disruptive background downloads
- ✅ User chooses when to restart and apply updates
- ✅ Automatic rollback protection
- ✅ Secure signature verification

## Development Notes

- The app loads `home.html` by default
- All API calls are routed to the hosted backend
- Theme preferences are stored locally using localStorage
- The app includes security best practices (disabled node integration, context isolation)

## Troubleshooting

1. **App won't start**: Make sure Node.js and npm are installed correctly
2. **Build fails**: Check that you have the necessary build tools for your platform
3. **API connection issues**: Verify that https://notes.lhamacorp.com is accessible from your network

## Security

The desktop app follows Electron security best practices:
- Node integration is disabled
- Context isolation is enabled
- External links open in the default browser
- Web security is enforced