# 🎵 MusicBot - Minecraft Voice Chat Music Player

A Spigot plugin that plays music through **Simple Voice Chat** groups.
Works like a Discord music bot, but inside Minecraft!

## ✨ Features

- 🎶 Play MP4/MP3/OGG/WAV music files in Voice Chat groups
- 👥 Group-only playback (only group members hear the music)
- 📋 Queue system with skip, pause, resume
- 🔊 Per-player volume control
- 📊 BossBar now-playing display with progress
- 🔒 Secure - no system paths exposed to players
- 🔍 Smart song search with fuzzy matching
- ⌨️ Tab completion for song names
- 🔄 Auto-detect new songs with /music reload
- 🎨 Custom Voice Chat volume category with icon

## 📋 Requirements

| Requirement | Version |
|---|---|
| Minecraft Server | Spigot/Paper 1.20.2+ |
| Java | 21+ |
| Simple Voice Chat | 2.5.x |
| FFmpeg | Any recent version |

## 📥 Installation

### Step 1: Install Dependencies
1. Install [Simple Voice Chat](https://modrinth.com/plugin/simple-voice-chat) on your server
2. Install [FFmpeg](https://github.com/GyanD/codexffmpeg/releases/download/2026-06-15-git-44d082edc8/ffmpeg-2026-06-15-git-44d082edc8-essentials_build.zip) on your server:
3. The FFmpeg file will be in zip format. Copy it to the FFmpeg/bin/FFmpeg.exe path and paste it into the Plugins/    
       MusicBot server path.!!!
   - **Windows:** Download from [ffmpeg.org](https://ffmpeg.org/download.html), extract, add to PATH
   - **Linux:** `sudo apt install ffmpeg`
   - **Mac:** `brew install ffmpeg`

### Step 2: Install Plugin
1. Download `MusicBot.jar` from [Releases](../../releases)
2. Put it in your `plugins/` folder
3. Restart the server

### Step 3: Add Music
1. Put your music files (MP4/MP3/OGG/WAV) in `plugins/MusicBot/music/`
2. Use `/music reload` to refresh the song list

## 🎮 Commands

| Command | Description |
|---|---|
| `/music play <name>` | Play a song (with tab complete!) |
| `/music list` | Show all available songs |
| `/music stop` | Stop playback and clear queue |
| `/music pause` | Pause current song |
| `/music resume` | Resume paused song |
| `/music skip` | Skip to next song in queue |
| `/music volume <0-100>` | Set playback volume |
| `/music queue` | Show current queue |
| `/music reload` | Reload config and song list (admin) |

## 🔒 Permissions

| Permission | Description | Default |
|---|---|---|
| `musicbot.use` | Use music bot commands | Everyone |
| `musicbot.admin` | Reload config and songs | OP |

## ⚠️ Important Notes

- Players **must be in a Voice Chat group** to use the music bot
- Only group members can hear the music
- If the DJ (person who started playing) leaves the group, music stops
- New members joining the group will automatically hear the music

## 🎵 Supported Formats

- `.mp4` (recommended - small file size)
- `.mp3`
- `.ogg`
- `.wav`
- `.m4a`
- `.flac`
- `.webm`

---

## 📦 Changelog

### v1.1.0 — Group Session Update

#### 🔧 Bug Fixes
- **Fixed:** Queue was per-player instead of per-group. Now all group members share one queue.
- **Fixed:** Multiple players in the same group could start separate music sessions causing audio overlap.
- **Fixed:** Any player could stop/pause/skip music. Now only the DJ (session starter) can control playback.
- **Fixed:** System file paths were exposed to players in error messages and `/music list`.
- **Fixed:** Audio cache didn't invalidate when music files were replaced with the same filename.
- **Fixed:** Players not connected to Voice Chat could trigger errors.

#### ✨ New Features
- **Group-Based Sessions:** Each Voice Chat group now has exactly one shared music session.
- **DJ System:** The player who starts the music becomes the DJ. Only the DJ can stop, pause, resume, skip, and change volume.
- **All Members Can Queue:** Any group member can add songs to the queue with `/music play`.
- **Multi-Language Support:** Players can choose their own language with `/music lang`.
- **Persian (Farsi) Language:** Full Persian translation included.
- **Auto DJ Transfer:** When the DJ leaves the group, music stops automatically.
- **BossBar Now Playing:** Shows current song, progress bar, elapsed time, and who requested it — only visible to group members.
- **Smart Song Search:** Fuzzy matching, partial name search, and word matching.
- **Tab Completion:** Song names auto-complete when typing `/music play`.

#### 🔒 Security
- Path traversal protection (blocks `../../` attacks)
- Input sanitization on all commands
- Rate limiting (1 second cooldown between commands)
- No system paths exposed to players
- File access restricted to `plugins/MusicBot/` folder only
- Error messages sanitized before showing to players

#### 🌍 Supported Languages
| Code | Language | Command |
|------|----------|---------|
| `en` | English 🇬🇧 | `/music lang en` |
| `fa` | فارسی 🇮🇷 | `/music lang fa` |

#### 🏗️ Technical Changes
- Session architecture changed from per-player to per-group
- Queue is now shared across all group members
- Audio cache hash includes file modification time and size
- Group membership monitored every 3 seconds
- Added `playerdata.yml` for persistent language preferences
- Added `lang/` folder with translation files

---
