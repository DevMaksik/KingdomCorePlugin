# KingdomCore

KingdomCore is an advanced Paper/Spigot plugin for survival and RPG servers. It combines kingdoms, guild-style member management, economy, progression, upgrades, quests, boss events, GUI menus, rankings, PlaceholderAPI support and SQL persistence.

The project is designed as a portfolio-grade Java plugin: modular packages, service-based architecture, database repositories, event API, optional integrations and clear extension points.

## Features

- Kingdom creation, disbanding, invites, kicking, leaving and information commands.
- Role system: `OWNER`, `OFFICER`, `MEMBER`, `RECRUIT`.
- Permission-like role capabilities that are easy to extend in code.
- Kingdom bank with deposits, withdrawals and Vault/local economy support.
- SQLite by default and configurable MySQL support.
- Daily and weekly kingdom quests.
- Upgrade system using bank money and prestige points.
- Boss event with bossbar, phases, special attacks and top damage rewards.
- Rankings by prestige, money and kills.
- Modern inventory GUI for overview, members, bank, upgrades, quests, ranking and home teleport.
- PlaceholderAPI integration when PlaceholderAPI is installed.
- Audit log table for important actions, added as an extra admin/portfolio feature.
- Public Bukkit events for integrations:
  - `KingdomCreateEvent`
  - `KingdomPrestigeChangeEvent`

## Technologies Used

- Java 17
- Paper API `1.21.4-R0.1-SNAPSHOT`
- Maven
- SQLite JDBC
- Optional MySQL Connector/J
- Optional Vault integration through reflection
- Optional PlaceholderAPI expansion
- Bukkit inventory GUI and scheduler APIs

## Commands

### Player Commands

| Command | Description |
| --- | --- |
| `/kingdom create <name>` | Creates a kingdom. |
| `/kingdom disband` | Disbands your kingdom. Owner only. |
| `/kingdom invite <player>` | Invites a player. |
| `/kingdom accept` | Accepts the latest invite. |
| `/kingdom kick <player>` | Kicks a member. |
| `/kingdom leave` | Leaves your kingdom. |
| `/kingdom info [name]` | Shows kingdom details. |
| `/kingdom list` | Lists kingdoms. |
| `/kingdom top` | Shows top kingdoms by prestige. |
| `/kingdom home` | Teleports to kingdom home. |
| `/kingdom sethome` | Sets kingdom home. |
| `/kingdom upgrade [type]` | Opens upgrades or buys an upgrade. |
| `/kingdom bank` | Shows kingdom bank balance. |
| `/kingdom deposit <amount>` | Deposits money into the kingdom bank. |
| `/kingdom withdraw <amount>` | Withdraws money from the kingdom bank. |
| `/kingdom quests` | Shows quest progress. |
| `/kingdom menu` | Opens the main GUI. |

### Admin Commands

| Command | Description |
| --- | --- |
| `/kingdomadmin reload` | Reloads config and messages. |
| `/kingdomadmin giveprestige <kingdom> <amount>` | Adds prestige. |
| `/kingdomadmin removeprestige <kingdom> <amount>` | Removes prestige. |
| `/kingdomadmin setlevel <kingdom> <level>` | Sets kingdom level. |
| `/kingdomadmin delete <kingdom>` | Deletes a kingdom. |
| `/kingdomadmin boss start` | Starts the configured boss event. |
| `/kingdomadmin money give <player> <amount>` | Gives money in the active economy. |
| `/kingdomadmin money take <player> <amount>` | Takes money in the active economy. |

## Permissions

| Permission | Default | Description |
| --- | --- | --- |
| `kingdomcore.use` | true | Allows using player commands. |
| `kingdomcore.create` | true | Allows creating kingdoms. |
| `kingdomcore.admin` | op | Allows admin command access. |
| `kingdomcore.boss.start` | op | Allows starting boss events. |
| `kingdomcore.reload` | op | Allows config reload. |
| `kingdomcore.economy.admin` | op | Allows admin economy changes. |

## PlaceholderAPI

These placeholders are available when PlaceholderAPI is installed:

- `%kingdomcore_kingdom_name%`
- `%kingdomcore_kingdom_level%`
- `%kingdomcore_kingdom_prestige%`
- `%kingdomcore_kingdom_rank%`
- `%kingdomcore_player_role%`
- `%kingdomcore_player_balance%`

The plugin works normally without PlaceholderAPI.

## Configuration

The plugin generates:

- `config.yml` for database, economy, kingdom, upgrade, quest and boss settings.
- `messages.yml` for configurable messages and GUI labels.
- `kingdoms.yml` as an example/notes file. Production data is stored in SQL.

Important config areas:

- `database.type`: `sqlite` or `mysql`.
- `economy.prefer-vault`: uses Vault if available, otherwise local SQL economy.
- `kingdom.create-cost`: cost of creating a kingdom.
- `kingdom.default-member-limit`: base member limit.
- `upgrades`: max levels and costs.
- `quests.daily` and `quests.weekly`: quest definitions.
- `boss`: spawn location, entity type, health and rewards.

## Installation

1. Build the plugin:

```bash
mvn clean package
```

2. Copy `target/KingdomCore-1.0.0.jar` into your Paper server `plugins` folder.
3. Start the server once to generate configuration files.
4. Edit `plugins/KingdomCore/config.yml` and `messages.yml`.
5. Restart the server or use `/kingdomadmin reload`.

## Local Paper Test

1. Download a Paper server jar for Minecraft 1.20+ or 1.21+.
2. Put the compiled KingdomCore jar in `plugins`.
3. Start the server and accept the EULA.
4. Join with an operator account.
5. Test:

```text
/kingdom create Avalon
/kingdom menu
/kingdom sethome
/kingdom deposit 100
/kingdomadmin giveprestige Avalon 500
/kingdom upgrade member_limit
/kingdomadmin boss start
```

## Example GUI Screens, Described

- Main menu: a compact 27-slot interface with kingdom info, members, bank, upgrades, quests, ranking and home teleport.
- Upgrades menu: each upgrade has an icon, current level, max level and next cost.
- Quests menu: daily and weekly tasks show description, progress and reward.
- Ranking view: top prestige kingdoms appear in the GUI lore and `/kingdom top`.

## Why This Project Is Advanced

- It uses a service/repository architecture instead of putting all logic in command classes.
- Data is cached in memory and persisted through SQL repositories.
- Expensive database writes are scheduled asynchronously where practical.
- Optional integrations are isolated so the plugin can run without Vault or PlaceholderAPI.
- The role permission model is extensible.
- Quest, upgrade and boss systems are data-driven through config.
- Public events allow other plugins to integrate with KingdomCore.
- Audit logs give administrators traceability for sensitive kingdom actions.

## Portfolio Description

KingdomCore is a portfolio-focused Minecraft Paper plugin that implements a full RPG kingdom system for survival servers. It includes kingdom management, roles, economy, SQL persistence, configurable quests, progression upgrades, GUI menus, rankings, boss events with rewards, PlaceholderAPI support and an extensible Java API. The project demonstrates clean architecture, domain modeling, optional dependency handling, asynchronous persistence and production-minded configuration.

## Project Structure

```text
src/main/java/pl/kingdomcore
├── api/event
├── commands
├── database
├── economy
├── gui
├── listeners
├── managers
├── models
├── placeholder
├── services
├── tasks
└── utils
```
