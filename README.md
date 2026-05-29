[![Maven CI](https://img.shields.io/github/actions/workflow/status/Hihelloy-main/ChatModerator/maven.yml?branch=master&style=flat-square)](https://github.com/Hihelloy-main/ChatModerator/actions)
[![GitHub release](https://img.shields.io/github/v/release/Hihelloy-main/ChatModerator?style=flat-square)](https://github.com/Hihelloy-main/ChatModerator/releases)
[![Github Downloads](https://img.shields.io/github/downloads/Hihelloy-main/ChatModerator/total.svg)](https://github.com/Hihelloy-main/ChatModerator/releases)
![SpigotMC Downloads](https://img.shields.io/spiget/downloads/128458?label=Spigot%20Downloads)
![Modrinth Downloads](https://img.shields.io/modrinth/dt/chatmoderator?label=Modrinth%20Downloads)

# ChatModerator - AI-Powered Minecraft Chat Moderation Plugin

A sophisticated Minecraft plugin that automatically moderates chat messages using AI, keeping your server safe and friendly. ChatModerator supports **both OpenAI and Gemini AI providers** for content analysis, with a built-in keyword fallback so moderation works even without an API key.

## Features

* **AI-Powered Moderation**: Analyze chat messages with OpenAI or Gemini APIs to detect inappropriate content.
* **Universal Compatibility**: Works seamlessly on Spigot, Paper, Folia, and Luminol servers with automatic detection.
* **Configurable Word Filter**: Block specific words with a customizable blacklist using whole-word matching to avoid false positives.
* **Keyword Fallback**: Built-in rule-based moderation activates automatically when no AI provider is configured or an API call fails.
* **Flexible AI Thresholds**: Adjust sensitivity for different categories like hate speech, harassment, sexual content, and violence (OpenAI provider).
* **Hold-and-Release Messaging**: Messages are held while AI checks run asynchronously — clean players never see a delay, blocked messages are never delivered.
* **Admin Notifications**: Admins with `chatmoderator.admin` are privately notified of violations instead of a server-wide broadcast.
* **Admin Tools**: Manage the plugin, word lists, mutes, and moderation behavior via commands.
* **Permission System**: Allow trusted players to bypass moderation entirely.
* **Mute System**: Players are automatically muted on violation, with private message blocking included. Mutes clean up automatically on disconnect.
* **Comprehensive Logging**: Track all moderation actions and AI decisions.
* **AI Test Command**: Quickly test your API key and see the full verdict with category and reason.
* **Folia/Luminol Scheduler Support**: Ensures tasks run correctly on threaded server implementations.

## Installation

1. Download the plugin JAR file.
2. Stop your server.
3. Place the JAR in your server's `plugins/` folder.
4. Start your server to generate configuration files.
5. Configure your OpenAI or Gemini API key in `plugins/ChatModerator/config.yml`.
6. Restart your server or run `/chatmod reload`.

## Configuration

### AI Provider Setup

* **OpenAI** (default): Get an API key from [platform.openai.com](https://platform.openai.com) and replace `your-openai-api-key-here` in the config. Uses the free `/v1/moderations` endpoint — no prompt tokens consumed.
* **Gemini**: Get an API key from [aistudio.google.com](https://aistudio.google.com) and replace `your-gemini-api-key-here` in the config. Set `ai.preferred-provider` to `gemini`.

If no API key is configured, the plugin falls back to built-in keyword rules automatically.

### Blocked Words

Add words to the blocked list in the configuration. Matching is whole-word only — `sex` will not block `sextant` or `Sussex`.

```yaml
moderation:
  blocked-words:
    - "badword1"
    - "inappropriate"
```

### AI Moderation Thresholds

Adjust sensitivity for each category (OpenAI provider only). Lower values are more strict.

```yaml
moderation:
  thresholds:
    hate: 0.3
    harassment: 0.3
    sexual: 0.5
    violence: 0.3
```

## Commands

* `/chatmod reload` - Reload configuration and reinitialise AI clients
* `/chatmod status` - Show plugin status, API key state, and mute count
* `/chatmod toggle` - Enable/disable moderation
* `/chatmod add-word <word>` - Add a word to the block list
* `/chatmod remove-word <word>` - Remove a word from the block list
* `/chatmod unmute <player>` - Unmute a muted player
* `/chatmod mutedplayers` - List all currently muted players and time remaining
* `/chatmod aitest <message>` - Test AI moderation and see the full verdict

## Permissions

* `chatmoderator.admin` - Access to all admin commands (default: op)
* `chatmoderator.bypass` - Bypass chat moderation entirely (default: false)
* `chatmoderator.command.unmute` - Unmute players without full admin (default: op)

## How It Works

1. **Message Interception**: Catches all chat messages at the lowest event priority.
2. **Mute Check**: Muted players are blocked immediately.
3. **Bypass Check**: Players with `chatmoderator.bypass` are passed through.
4. **Word Filter**: Message tokens are checked against the blocked words list using whole-word matching.
5. **AI Analysis**: Message is held, then sent to OpenAI or Gemini asynchronously. If the AI clears it, the message is delivered; if not, punishment is applied.
6. **Fallback**: If AI is unavailable or the call fails, built-in keyword rules are used.
7. **Action Execution**: Mutes the player, warns them, notifies admins privately, and logs the violation.

## Requirements

* Minecraft Server 1.20+
* Spigot, Paper, Folia, or Luminol
* Java 17+
* OpenAI or Gemini API key (optional — keyword fallback works without one)
* Internet connection (for AI moderation)

## Building from Source

```bash
git clone https://github.com/Hihelloy-main/ChatModerator.git
cd ChatModerator
mvn clean package
```

The compiled JAR will be in the `target/` directory as `ChatModerator-4.0.jar`.

## Support

For issues or feature requests, create an issue on GitHub or contact the plugin developer.

## License

This project is licensed under the MIT License.

## Downloads

Spigot: https://www.spigotmc.org/resources/chatmoderator.128458/

Modrinth: https://modrinth.com/plugin/chatmoderator

Github: https://github.com/Hihelloy-main/ChatModerator

PolyMart: https://polymart.org/product/8425/chatmoderator