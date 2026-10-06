# Lyric Providers

Lyric providers send lyrics, playback state, and translation data to Lyricon. The main
Lyricon app manages display configuration, but it does not guarantee lyrics for every player by
itself.

## Sources

Install lyric providers from the official source:

- [LyricProvider Releases](https://github.com/proify/LyricProvider/releases)

## View Available Providers

Open Lyricon and enter **Lyric Providers**. Available and recognized providers are grouped by
category.

| Field   | Description                                                        |
|:--------|:-------------------------------------------------------------------|
| Name    | Provider or adapter name                                           |
| Author  | Provider author information                                        |
| Version | Installed provider version                                         |
| Tags    | Supported capabilities, such as word-by-word lyrics or translation |

## List Display Mode

The top-right menu can switch the provider list mode.

| Mode    | Description                                              |
|:--------|:---------------------------------------------------------|
| Compact | Shows essential information for daily use                |
| Full    | Shows more tags and provider details                    |

## Per-provider lyric controls

Tap the **gear icon** in the top-right corner of a provider card to open lyric controls. Settings belong to the plugin package, defaulting to all lyrics enabled and zero delay. A plugin serving multiple players shares its own settings. Options include disabling all lyrics, only the primary line, or subtitles (translation, romanization and additional text below the primary line).

Delay ranges from -30000 to 30000 milliseconds: positive values delay lyrics, negative values advance them, and zero preserves original synchronization. Changes take effect immediately for the current app; the top-right Miuix **Reset icon button** only restores the selected provider. Timed lyrics, word highlighting, seeking and retained paused lyrics share the same lyric offset. Player progress and listening history use actual playback time. Plain text can be delayed but cannot be shown before arrival; switching sources, seeking or pausing cancels pending old text. Controls are included in style backups. Restart System UI after first upgrading to a version with this feature.

## Capability Tags

| Tag                 | Meaning                                        |
|:--------------------|:-----------------------------------------------|
| Word-by-word lyrics | The provider can supply word-level timing data |
| Translation         | The provider can supply translated lyrics      |

If a tag is missing, Lyricon may still show plain lyrics, but that capability will not be available.

## Provider Installed But Not Working

Check in this order:

1. The provider APK was installed successfully.
2. The provider supports the current player version.
3. The player is playing a track with available lyrics.
4. The provider appears in the Lyricon provider page.
5. The player was reopened after installing or updating the provider.

## No Available Providers

If the page shows that no lyric providers are available:

- Confirm that a provider is installed.
- Confirm that Lyricon has permission to query installed apps.
- Confirm that the provider package and version are compatible with Lyricon.
- Reopen Lyricon and the music player.
