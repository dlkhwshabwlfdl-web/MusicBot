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

## 📁 Folder Structure
