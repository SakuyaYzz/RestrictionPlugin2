# RestrictionPlugin2

[中文](#中文) | [English](#english)

[![Paper](https://img.shields.io/badge/Paper-1.21.4-blue)](https://papermc.io/)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)

---

## 中文

### 简介

**RestrictionPlugin2** 是一款适用于 **Paper 1.21.4** 的世界级限制插件，通过 `config.yml` 按世界名进行四项限制：**冰霜行者禁用、禁止飞行、按世界禁用命令、禁止传送到指定世界**。所有限制都以世界名为键配置，可选开启 `debug-mode` 输出详细的拦截日志。插件不修改玩家数据，仅在事件层拦截，并提供一套简单的 API 供其他插件标记「插件自身发起的传送」以免被误拦。

### 功能特性

- **冰霜行者限制**：监听 `EntityBlockFormEvent`（冰霜行者生成霜冰的瞬间），在 `frost-walker-disabled-worlds` 列出的世界中直接取消该方块生成。以**最低优先级**监听，便于在其他插件处理之前先拦截。
- **禁止飞行**：监听 `PlayerChangedWorldEvent` 与 `PlayerMoveEvent`，在 `no-fly-worlds` 列出的世界中若玩家处于可飞行（`getAllowFlight()`）或正在飞行（`isFlying()`）状态，立即取消其飞行能力并发送 `messages.fly-disabled` 提示。
- **按世界禁用命令**：监听 `PlayerCommandPreprocessEvent`，在 `command-restrictions` 中为每个世界配置一份命令黑名单。命令取首段（以空格分割）并**统一转小写**后比对，因此 `/Fly`、`/FLY` 同样会被拦截；命中时取消事件并发送 `messages.command-blocked`。
- **禁止传送到指定世界**：监听 `PlayerTeleportEvent`，当**目标世界**位于 `teleport-blocked-worlds` 时，仅拦截「由 `teleport-commands` 列表中的指令发起的传送」——插件会记录玩家执行指令前的原始坐标，取消该次传送后在下一 tick（`runTaskLater(..., 1L)`）把玩家送回原位置并发送 `messages.teleport-blocked`；**其他来源的传送（插件、传送门、末影珍珠等）一律放行**，避免误伤其他插件的功能。
- **传送上下文 API**：`markPluginTeleport(Player)` 可把玩家下一次传送标记为「插件传送」（放行），`clearTeleportContext(Player)` 用于清理标记，`getInstance()` 获取插件实例。
- **权限绕过**：`worldrestrictions.bypass.command` 与 `worldrestrictions.bypass.fly` 可绕过对应限制（OP 同样自动绕过）；其中命令绕过开启后，连传送上下文都不会记录。
- **热重载**：`/restrictionreload` 重新读取 `config.yml` 并刷新所有限制列表，无需重启服务器。
- **调试模式**：`debug-mode: true` 时，启动与拦截过程会把「冰霜行者禁用世界 / 飞行限制世界 / 传送限制世界 / 传送命令 / 各世界命令黑名单」以及每次拦截详情写入控制台。

### 命令与权限

| 命令 | 说明 | 权限 |
|------|------|------|
| `/restrictionreload` | 重新加载 `config.yml` 并刷新全部限制 | `worldrestrictions.reload`（默认 OP） |

| 权限节点 | 说明 | 默认值 |
|----------|------|--------|
| `worldrestrictions.reload` | 允许执行 `/restrictionreload` | `op` |
| `worldrestrictions.bypass.fly` | 绕过飞行限制 | `false`（OP 亦自动绕过） |
| `worldrestrictions.bypass.command` | 绕过命令限制 | `false`（OP 亦自动绕过） |
| `worldrestrictions.bypass.teleport` | 绕过传送限制（**当前版本代码未使用**，仅为后续扩展保留） | `false` |

> 传送限制目前**没有**权限绕过判断：只要目标是受限世界且传送由配置中的指令触发，任何玩家都会被拦回原地。

### 配置文件 `config.yml`

```yaml
# 禁用冰霜行者的世界列表
frost-walker-disabled-worlds:
  - '主世界'
  - '酒店'
  - "地狱之际"
  - "沙漠神殿"
  - "公会争霸赛"
  - "竞技场"
  - "失落之城"
  - "拍卖行"

# 禁止飞行的世界列表
no-fly-worlds:
  - '酒店'
  - "地狱之际"
  - "沙漠神殿"
  - "公会争霸赛"
  - "竞技场"
  - "失落之城"
  - "拍卖行"

# 禁止传送的目标世界
teleport-blocked-worlds:
  - '拍卖行'

# 需要检测的传送命令列表（不区分大小写）
teleport-commands:
  - tpa
  - tpahere
  - back
  - home
  - tpaccept
  - pp

# 世界命令限制配置
command-restrictions:
  survival_world:
    - fly
    - efly
    - tpa
  mining_world:
    - home
    - sethome
    - back

# 消息自定义
messages:
  command-blocked: "§c此命令在该世界被禁用!"
  fly-disabled: "§c此世界禁止飞行!"
  teleport-blocked: "§c目标世界禁止传送!"
```

> `command-restrictions` 下的**键即世界名**，值是该世界要禁用的命令列表（写命令本体，不带 `/`）。
> `messages` 三个键分别对应命中命令限制、飞行限制、传送限制时的提示文本，支持 `§` 颜色代码。

### 安装与使用

1. 确认服务端为 **Paper 1.21.4**、Java **21**。
2. 将 `RestrictionPlugin2.jar` 放入服务器 `plugins` 文件夹。
3. 启动或重启服务器，插件会生成 `plugins/RestrictionPlugin2/config.yml`。
4. 把配置中的示例世界名改成你服务器里**真实的世界名**（用与 `level-name` 一致的名字，注意大小写与中文名要完全匹配）。
5. 按需填写 `teleport-commands`（哪些指令算作「玩家自身发起的传送」）与各世界的命令黑名单。
6. 执行 `/restrictionreload` 生效，无需重启；调试阶段可开启 `debug-mode: true` 观察日志。

### 构建方法

本项目使用 **Gradle** 构建（`java-library` + `xyz.jpenilla.run-paper`）。确保已安装 JDK 21。

在项目根目录执行：

```bash
# Linux / macOS
./gradlew clean build

# Windows
gradlew.bat clean build
```

构建完成后，可在 `build/libs/` 目录找到 `RestrictionPlugin2-2.0-SNAPSHOT.jar`。

本地调试可直接启动一台 1.21.4 测试服：

```bash
gradlew.bat runServer
```

**依赖：**

- Paper API 1.21.4
- Java 21
- 无第三方前置插件（不依赖 Vault / PlaceholderAPI）

`build.gradle.kts` 示例（供参考）：

```kotlin
plugins {
    id("java-library")
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "org.shaku"
version = "2.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks {
    runServer {
        minecraftVersion("1.21.4")
        jvmArgs("-Xms2G", "-Xmx2G")
    }
    processResources {
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
```

### 注意事项

- **世界名必须精确匹配**：配置中的世界名与 `Bukkit.getWorld().getName()` 完全一致才会生效（区分大小写）。示例里的「主世界 / 酒店 / 拍卖行」是中文世界名，直接照抄通常不会命中，请替换为你服务器的实际世界名。
- **配置中的示例世界名是占位数据**：`command-restrictions` 下的 `survival_world`、`mining_world` 同样需要改成真实世界名，否则该世界的命令限制不会触发。
- **冰霜行者限制的作用范围**：仅阻止在受限世界生成霜冰方块，不禁止使用附魔本身。
- **飞行限制的触发时机**：在**进入世界**与**移动**时检查，会直接 `setAllowFlight(false)`；若玩家在受限世界中因其他插件反复获得飞行权限，会出现反复开关的提示刷屏。
- **传送限制只拦「指令传送」**：插件/传送门/末影珍珠等来源的传送一律放行；目标世界不在 `teleport-blocked-worlds` 中时也不会拦截。
- **`teleport-commands` 只写命令本体**：不要带 `/`，需与玩家实际输入的首段命令一致（如 Essentials 的 `/home` 与 `/homes` 是两个不同命令）。
- **绕过权限为 OP 自动生效**：`checkFlightRestriction` 与 `onCommand` 都额外判断 `player.isOp()`，因此 OP 玩家无法用这两项限制来限制。（传送限制不受此影响。）
- **`worldrestrictions.bypass.teleport` 当前未生效**：`plugin.yml` 中已声明该节点，但 Java 代码中没有对应判断。
- **两处 `plugin.yml` 版本不一致**：仓库中同时存在 `src/plugin.yml`（`version: 2.0-SNAPSHOT`）与 `src/main/resources/plugin.yml`（`version: 1.0`）。Gradle 只打包 `src/main/resources` 下的那份，所以**实际生效的是 1.0**；`src/plugin.yml` 是 IDE 构建时代的遗留文件，建议删除或同步版本。
- **`processResources` 的版本替换未生效**：构建脚本里配置了 `expand(mapOf("version" to version))`，但 `plugin.yml` 中并未使用 `${version}` 占位符，因此 jar 版本号（`2.0-SNAPSHOT`）与插件内声明版本（1.0）不一致。
- 插件未使用持久化数据，`onDisable()` 只清空传送上下文映射，卸载或热重载时不会有残留数据。

---

## English

### Introduction

**RestrictionPlugin2** is a world-level restriction plugin for **Paper 1.21.4**. Through `config.yml` it applies four per-world rules: **disabling Frost Walker, blocking flight, blocking per-world commands, and blocking teleports into specified worlds**. Every rule is keyed by world name, and an optional `debug-mode` prints detailed interception logs. The plugin stores no player data — it only intercepts events — and exposes a small API that lets other plugins mark "plugin-initiated teleports" so they are not blocked by mistake.

### Features

- **Frost Walker restriction**: Listens to `EntityBlockFormEvent` (the moment Frost Walker creates frosted ice) and cancels block formation in worlds listed in `frost-walker-disabled-worlds`. It runs at the **lowest priority** so it intercepts before other plugins handle the event.
- **Flight blocking**: Listens to `PlayerChangedWorldEvent` and `PlayerMoveEvent`; in worlds listed in `no-fly-worlds`, if a player has `getAllowFlight()` or `isFlying()`, flight is disabled immediately and the `messages.fly-disabled` text is sent.
- **Per-world command blocking**: Listens to `PlayerCommandPreprocessEvent`; `command-restrictions` maps each world to a command blacklist. The command's first token (split by spaces) is **lower-cased** before comparison, so `/Fly` and `/FLY` are blocked too. On a hit the event is cancelled and `messages.command-blocked` is sent.
- **Blocking teleports into specified worlds**: Listens to `PlayerTeleportEvent`; when the **destination world** is in `teleport-blocked-worlds`, only teleports **triggered by the commands in `teleport-commands`** are blocked — the plugin records the player's original location, cancels the teleport, then one tick later (`runTaskLater(..., 1L)`) teleports the player back and sends `messages.teleport-blocked`. Teleports from **any other source (plugins, portals, ender pearls, …) are allowed**, so other plugins are not broken.
- **Teleport context API**: `markPluginTeleport(Player)` marks a player's next teleport as plugin-initiated (allowed), `clearTeleportContext(Player)` clears the mark, and `getInstance()` returns the plugin instance.
- **Permission bypasses**: `worldrestrictions.bypass.command` and `worldrestrictions.bypass.fly` bypass the corresponding restrictions (OPs bypass automatically as well). When the command bypass applies, no teleport context is recorded at all.
- **Hot reload**: `/restrictionreload` re-reads `config.yml` and refreshes every restriction list without a server restart.
- **Debug mode**: with `debug-mode: true`, startup and every interception log the frost-walker/flight/teleport world lists, the teleport command list, each world's command blacklist, and interception details to the console.

### Commands & Permissions

| Command | Description | Permission |
|---------|-------------|------------|
| `/restrictionreload` | Reload `config.yml` and refresh all restrictions | `worldrestrictions.reload` (default: OP) |

| Permission node | Description | Default |
|-----------------|-------------|---------|
| `worldrestrictions.reload` | Allows running `/restrictionreload` | `op` |
| `worldrestrictions.bypass.fly` | Bypass the flight restriction | `false` (OPs bypass too) |
| `worldrestrictions.bypass.command` | Bypass the command restriction | `false` (OPs bypass too) |
| `worldrestrictions.bypass.teleport` | Bypass the teleport restriction (**not used by the current code**, reserved for future expansion) | `false` |

> The teleport restriction has **no** permission bypass: as long as the destination is a restricted world and the teleport was triggered by a configured command, any player is sent back.

### Configuration `config.yml`

```yaml
# Worlds where Frost Walker is disabled
frost-walker-disabled-worlds:
  - '主世界'
  - '酒店'
  - "地狱之际"
  - "沙漠神殿"
  - "公会争霸赛"
  - "竞技场"
  - "失落之城"
  - "拍卖行"

# Worlds where flight is not allowed
no-fly-worlds:
  - '酒店'
  - "地狱之际"
  - "沙漠神殿"
  - "公会争霸赛"
  - "竞技场"
  - "失落之城"
  - "拍卖行"

# Destination worlds that cannot be teleported into
teleport-blocked-worlds:
  - '拍卖行'

# Teleport commands to watch (case-insensitive)
teleport-commands:
  - tpa
  - tpahere
  - back
  - home
  - tpaccept
  - pp

# Per-world command restrictions
command-restrictions:
  survival_world:
    - fly
    - efly
    - tpa
  mining_world:
    - home
    - sethome
    - back

# Customizable messages
messages:
  command-blocked: "§c此命令在该世界被禁用!"
  fly-disabled: "§c此世界禁止飞行!"
  teleport-blocked: "§c目标世界禁止传送!"
```

> Under `command-restrictions`, **each key is a world name** and its value is the list of commands blocked in that world (the command itself, without `/`).
> The three `messages` keys are the texts shown when the command, flight, and teleport restrictions trigger, and support `§` color codes.

### Installation & Usage

1. Make sure the server runs **Paper 1.21.4** on Java **21**.
2. Place `RestrictionPlugin2.jar` into the server's `plugins` folder.
3. Start or restart the server; `plugins/RestrictionPlugin2/config.yml` is generated.
4. Replace the sample world names with the **real world names** of your server (use the same name as `level-name`; matching is case-sensitive and must match Chinese names exactly).
5. Fill in `teleport-commands` (which commands count as player-initiated teleports) and each world's command blacklist as needed.
6. Run `/restrictionreload` to apply changes without restarting; enable `debug-mode: true` while tuning to inspect the logs.

### Build

This project uses **Gradle** (`java-library` + `xyz.jpenilla.run-paper`). Make sure JDK 21 is installed.

From the project root, run:

```bash
# Linux / macOS
./gradlew clean build

# Windows
gradlew.bat clean build
```

After building, the `RestrictionPlugin2-2.0-SNAPSHOT.jar` will be in the `build/libs/` directory.

For local testing you can boot a 1.21.4 test server directly:

```bash
gradlew.bat runServer
```

**Dependencies:**

- Paper API 1.21.4
- Java 21
- No third-party plugins required (Vault / PlaceholderAPI are not used)

Example `build.gradle.kts`:

```kotlin
plugins {
    id("java-library")
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "org.shaku"
version = "2.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks {
    runServer {
        minecraftVersion("1.21.4")
        jvmArgs("-Xms2G", "-Xmx2G")
    }
    processResources {
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
```

### Notes

- **World names must match exactly**: a world name only takes effect when it is identical to `Bukkit.getWorld().getName()` (case-sensitive). The samples (主世界 / 酒店 / 拍卖行) are Chinese world names; copying them verbatim usually matches nothing, so replace them with your server's actual world names.
- **The sample world names are placeholders**: `survival_world` and `mining_world` under `command-restrictions` must also be renamed, otherwise those command restrictions never trigger.
- **Scope of the Frost Walker restriction**: it only prevents frosted ice from forming in the restricted worlds; the enchantment itself is not disabled.
- **When the flight check runs**: on **world change** and on **movement**, and it calls `setAllowFlight(false)` directly. If another plugin keeps granting flight in a restricted world, the denial message can spam repeatedly.
- **The teleport restriction only blocks command teleports**: teleports from plugins, portals or ender pearls are always allowed, and nothing is blocked when the destination world is not in `teleport-blocked-worlds`.
- **`teleport-commands` takes bare command names**: no leading `/`, and the entry must match the first token of what the player types (e.g. Essentials' `/home` and `/homes` are two distinct commands).
- **Bypass permissions are automatic for OPs**: both `checkFlightRestriction` and `onCommand` additionally test `player.isOp()`, so OPs cannot be restricted by these two rules. (The teleport restriction is unaffected.)
- **`worldrestrictions.bypass.teleport` has no effect yet**: it is declared in `plugin.yml` but there is no corresponding check in the Java code.
- **Two inconsistent `plugin.yml` files**: the repository contains both `src/plugin.yml` (`version: 2.0-SNAPSHOT`) and `src/main/resources/plugin.yml` (`version: 1.0`). Gradle only packages the one under `src/main/resources`, so **the effective version is 1.0**; `src/plugin.yml` is a leftover from the IDE build and should be deleted or kept in sync.
- **The `processResources` version expansion does nothing**: the build script configures `expand(mapOf("version" to version))`, but `plugin.yml` contains no `${version}` placeholder, so the jar version (`2.0-SNAPSHOT`) differs from the version declared inside the plugin (1.0).
- The plugin persists no data; `onDisable()` only clears the teleport context map, so disabling or reloading leaves nothing behind.
