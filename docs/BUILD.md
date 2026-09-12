# 从源码构建

## 环境要求

| 项目 | 版本 |
|---|---|
| JDK | **17**（必须；ForgeGradle 6 不支持 8/21） |
| Gradle | 8.x（仓库使用 8.1.1 验证） |
| 网络 | 首次构建需要联网下载 Forge/Minecraft 依赖 |

## 步骤

1. **放入前置模组**（仓库不附带第三方 jar）：

   ```
   libs/MCphone-0.10.2.jar
   ```

   从 [MCphone Releases](https://github.com/november521/mcphone/releases) 下载对应版本后改名放入即可。
   `build.gradle` 中通过 `compileOnly files('libs/MCphone-0.10.2.jar')` 引用。

2. **构建**：

   ```bash
   gradle build
   ```

   产物：`build/libs/generated_mod-<版本>.jar`

3. **安装**：把产物与 MCphone 一起放进 `mods/`（客户端与服务端都要放）。

## 离线构建（可选）

若本机已有依赖缓存，可加 `--offline` 跳过网络检查：

```bash
gradle --offline build
```

本机还可使用自备的 `mirror.init.gradle`（仓库未收录，含本机绝对路径）来指定镜像仓库：

```bash
gradle --offline -I mirror.init.gradle build
```

## 常见报错

| 报错 | 原因与处理 |
|---|---|
| `Could not find net.minecraftforge:forge:1.20.1-47.4.0` | 首次构建需要联网；或镜像仓库未配置 |
| `JAVA_HOME is not set` / 编译报 `records are not supported` | JDK 版本不对，必须 JDK 17 |
| `package cn.blockforge.generated.generatedmod.api.client.phone does not exist` 之类 | `libs/MCphone-0.10.2.jar` 缺失或版本不符 |
| 构建成功但进游戏无反应 | MCphone 未安装，或未放到服务端 `mods/` |

## 版本号规则

`主.次.修订` 三段式，**修订位从 1 递增到 10 后进位**（例：`4.4.10 → 4.5.1`）。
发布时同步修改 `build.gradle` 的 `version` 与 `CHANGELOG.md`。
