# 📦 GitHub Release Files

When the **unified workflow** runs (after Docker deployment), it creates a GitHub Release with these files:

## 🐧 Linux Files
```
kNotes-1.1.0.AppImage                    # ~104MB - Portable executable
kNotes-1.1.0.AppImage.blockmap          # Delta update file
latest-linux.yml                        # Auto-updater metadata
```

## 🪟 Windows Files
```
kNotes Setup 1.1.0.exe                  # ~120MB - NSIS installer
kNotes Setup 1.1.0.exe.blockmap        # Delta update file
latest.yml                              # Auto-updater metadata
```

## 🍎 macOS Files
```
kNotes-1.1.0.dmg                        # ~115MB - Disk image installer
kNotes-1.1.0.dmg.blockmap              # Delta update file
kNotes-1.1.0-mac.zip                   # Raw .app bundle
latest-mac.yml                          # Auto-updater metadata
```

## 📝 Release Notes (Auto-generated)
```markdown
## kNotes Desktop v1.1.0

### What's New:
- Latest frontend updates from the web application
- Automatic synchronization with deployed API
- Bug fixes and improvements

### Downloads:
- **Windows**: Download the .exe installer
- **macOS**: Download the .dmg installer
- **Linux**: Download the .AppImage file

Built from commit: abc123...
```

## 🔗 Release URL
The release will be available at:
`https://github.com/lhamacorp/knotes/releases/latest`

## 👥 User Experience
1. Users go to GitHub releases page
2. Download appropriate file for their OS
3. Install/run the application
4. App automatically checks for future updates
5. Users get notified when new versions are available