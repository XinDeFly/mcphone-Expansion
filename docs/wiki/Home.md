# MCphone 市场扩展 · MCphone Market Expansion

[使用指南](使用指南) ｜ [常见问题](常见问题) ｜ [更新日志](https://github.com/XinDeFly/mcphone-Expansion/blob/main/CHANGELOG.md) ｜ [下载](https://github.com/XinDeFly/mcphone-Expansion/releases)

基于 [MCphone](https://github.com/november521/mcphone) 的 Minecraft 1.20.1 Forge 扩展模组：
给手机加一个金融 App，给世界加一套现货 / 期货 / 股票交易系统，再配一台能自动挂单的电脑机箱。

> 本仓库是 MCphone 的**第三方附属**，独立发布、独立维护、独立授权。
> 前置模组的问题请反馈到 [MCphone 仓库](https://github.com/november521/mcphone/issues)，
> 本模组的问题请开在 [本仓库 Issues](https://github.com/XinDeFly/mcphone-Expansion/issues)。

---

## 依赖

| 依赖 | 版本 |
|---|---|
| Minecraft | 1.20.1 |
| Forge | 47.4.0 及以上 |
| [MCphone](https://github.com/november521/mcphone) | **0.10.2 及以上（必需前置）** |
| [Rarity Core](https://www.curseforge.com/minecraft/mc-mods/raritycore) | 可选（装与不装都能正常运转） |

客户端与服务端都需安装。

---

## 最新版本：4.5.9（经济系统重构）

- **稀有度 7 级**：普通 / 稀有 / 罕见 / 史诗 / 传说 / 神话 / 唯一，等级与 Rarity Core 完全对齐（未安装该模组时使用本模组内置的 1262 条稀有度快照）。
- **日涨跌幅度按稀有度递增**：10% → 150%（等级越高、跨度越大）。
- **初始价格按稀有度分档**：$10 → $10000，且区间宽度逐级放大。
- **现货卖出新增手续费**：按稀有度 5.0% → 15.0%（越稀有越高）。
- **货币精度改为「分」**：钱包与价格统一显示两位小数（如 `$250.00`），旧存档自动迁移。

完整的参数表与说明见 [使用指南](使用指南)。

---

## 页面导航

| 页面 | 内容 |
|---|---|
| [使用指南](使用指南) | 物品方块、交易终端、手机 App、电脑设备、自动化规则、**稀有度与经济系统** |
| [常见问题](常见问题) | 安装、交易、手续费、稀有度、机箱、界面操作的问题排查 |
| [更新日志](https://github.com/XinDeFly/mcphone-Expansion/blob/main/CHANGELOG.md) | 每个版本的改动明细 |
| [从源码构建](https://github.com/XinDeFly/mcphone-Expansion/blob/main/docs/BUILD.md) | JDK 17 + Gradle 构建、离线构建与常见报错 |

---

## 三分钟上手

1. 安装 MCphone 与本模组，启动游戏。
2. 合成「便捷交易平台」并右键，买卖现货。
3. 合成「电脑显示器」与「电脑机箱」，把机箱放在显示器相邻方向。
4. 打开显示器控制台 → 选择机箱方位 → 用「内存条」（图书管理员 3 金锭）扩容 → 设置规则让它自动买卖。

---

## 许可

[MIT](https://github.com/XinDeFly/mcphone-Expansion/blob/main/LICENSE)。
前置模组 MCphone 由其作者独立授权，本仓库不包含其代码或二进制文件。
