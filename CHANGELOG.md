# Changelog

All notable changes to MusicBot will be documented in this file.

## [1.1.0] — 2026-11-09

The Group Session Update. One Voice Chat group now has exactly one shared
music session, and the README's v1.1.0 notes now match the code.

### Fixed

- **Shared group session enforced:** `/music play` from any group member now
  queues into the group's active session instead of starting a parallel one,
  so two members can no longer play different songs over each other.
- **DJ-only controls:** stop, pause, resume, skip and volume changes are now
  explicitly DJ-only — other members get an "Only the DJ can control
  playback!" message instead of a misleading "Nothing is playing!".
- **Queue visibility:** `/music queue` shows the group's shared queue even
  when the caller has no session of their own; `/music volume` without
  arguments shows the session volume.
- **Stale audio cache:** the PCM cache key now includes the file's
  modification time and size, so replacing a music file with the same name
  no longer plays the old cached audio.

### Added

- Group-based sessions: each Voice Chat group has exactly one shared music
  session.
- DJ system: the player who starts the music becomes the DJ; only the DJ
  controls playback.
- BossBar now-playing display with progress, visible to group members.
- Smart song search: exact, filename, contains (scored) and word matching.
- Tab completion for song names.
- Session volume control.
- Custom Voice Chat volume category with icon.

### Security

- Path traversal protection (blocks `../../` attacks) and input sanitization
  on all commands.
- No system paths exposed to players; error messages sanitized.
- File access restricted to `plugins/MusicBot/`.
- 1-second command cooldown (rate limiting).

### Notes

- Group membership is checked every 3 seconds; when the DJ leaves the group,
  music stops automatically.
- Multi-language support (`/music lang`) is not part of this release — the
  README previously listed it by mistake.

## [1.0.0] — 2026-06-27

- Initial release: Spigot/Paper plugin that plays music through Simple Voice
  Chat groups — queue, skip, per-player volume, BossBar now-playing.
