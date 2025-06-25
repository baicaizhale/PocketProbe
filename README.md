-----

# PocketProbe

## 简介

**PocketProbe** 是一款为 Minecraft Spigot 服务器设计的插件，它允许具有相应权限的玩家查看和编辑其他在线玩家的背包内容。这对于服务器管理员进行物品管理、协助玩家、或进行内容审核等场景非常有用。

## 功能特性

  * **查看背包**: 轻松打开并查看任何在线玩家的物品栏。
  * **实时编辑**: 在打开的背包界面中直接添加、移除、移动或修改物品，这些更改会即时同步到目标玩家的背包中。
  * **权限控制**: 通过 Spigot 权限系统，精确控制哪些玩家可以使用该功能。
  * **简洁易用**: 简单的命令和直观的操作界面。

## 命令

所有命令都通过 `/pocketprobe` 或其别名执行。

| 命令                 | 描述                                     | 权限           |
| :------------------- | :--------------------------------------- | :------------- |
| `/pocketprobe <玩家名>` | 打开指定在线玩家的背包进行查看和编辑。 | `pocketprobe.use` |

## 权限

| 权限节点        | 描述                                 | 默认         |
| :-------------- | :----------------------------------- | :----------- |
| `pocketprobe.use` | 允许玩家使用 `/pocketprobe` 命令。 | `op` (管理员) |

### 如何授予权限

你可以使用权限管理插件（例如 LuckPerms）来管理权限。

**示例 (使用 LuckPerms):**

  * **授予特定玩家权限**:
    ```
    /lp user <玩家名> permission set pocketprobe.use true
    ```
  * **授予特定玩家组权限**:
    ```
    /lp group <组名> permission set pocketprobe.use true
    ```

## 安装

1.  **下载**: 从 [Github Releaces](https://github.com/baicaizhale/PocketProbe/releases) 下载最新版本的 `PocketProbe-1.0.0.jar` 文件。
2.  **部署**: 将下载的 `.jar` 文件放入你的 Minecraft Spigot(或分支) 服务器的 `plugins/` 文件夹中。
3.  **重启/重载**: 重启你的服务器，或者使用 `/reload` 命令（不推荐在生产环境频繁使用，可能导致问题）来加载插件。

## 构建 (开发者)

如果你是开发者并希望从源代码构建此插件：

1.  **克隆仓库**:
    ```bash
    git clone https://github.com/baicaizhale/PocketProbe.git
    cd PocketProbe
    ```
2.  **使用 Maven 构建**:
    确保你已安装 Maven。
    ```bash
    mvn clean package
    ```
    构建成功后，生成的 `.jar` 文件将位于 `target/` 目录下。

## 支持与贡献

如果你在使用过程中遇到任何问题、有改进建议或希望贡献代码，请通过 [Github仓库](https://github.com/baicaizhale/PocketProbe) 或其他指定渠道联系。

## 许可证

## 本项目采用 GPL v3 许可证。