# MCphone 市场扩展（MCphone Market Expansion）

[使用指南](../../wiki/使用指南) ｜ [常见问题](../../wiki/常见问题) ｜ [从源码构建](docs/BUILD.md) ｜ [更新日志](CHANGELOG.md) ｜ [发布版本](../../releases)

基于 [MCphone](https://github.com/november521/mcphone) 的 Minecraft 1.20.1 Forge 扩展模组：给手机加一个金融 App，
给世界加一套「现货 / 期货 / 股票」交易系统，再配一台能自动挂单的电脑机箱。

> 本仓库是 MCphone 的**第三方附属**，独立发布、独立维护、独立授权。
> 前置模组的问题请反馈到 [MCphone 仓库](https://github.com/november521/mcphone/issues)，
> 本模组的问题请开在 [本仓库 Issues](https://github.com/XinDeFly/mcphone-Expansion/issues)。

---

## 功能一览

| 模块 | 内容 |
|---|---|
| 交易终端 | 现货买卖、期货开平仓、股票持仓，实时报价与涨跌幅，钱包余额结算 |
| 市场引擎 | 服务端统一价格与每日更新，价格与持仓持久化到存档；同一物品每日行情全局一致 |
| 手机 App | 基于 MCphone 的 App SPI，手机内新增「金融」App：行情页、下单、持仓、帮助页 |
| 电脑设备 | 显示器（控制台）+ 机箱（存储/自动化），独有的机箱元件界面与桌面式外壳 |
| 机箱存储 | 基础 9 格，每插一条内存条 +9 格，**上限 81 格**；显示器内 4 行视口平滑滚动查看 |
| 自动化交易 | 机箱可设「购入 / 出售 / 存储」角色，配合红石条件与三种触发时机，按物品规则自动成交 |
| 存储元件 | 内存条：图书管理员村民出售（3 金锭），插入机箱即扩容 |
| 可复用组件 | 模组内含公开 UI 组件（滚动视口、设备屏幕基类、拼音检索、输入保护、统一绘制），可被其它模组直接调用 |

### 三个触发时机

| 触发 | 说明 |
|---|---|
| 更新后 | 每日价格更新完成后立即执行一次 |
| 达价 | 每 20 刻扫描，价格达到规则阈值时执行（同一规则每日最多一次） |
| 红石 | 红石信号满足「需信号 / 需无信号」条件时执行（同一规则每日最多一次） |

---

## 依赖

| 依赖 | 版本 | 说明 |
|---|---|---|
| Minecraft | 1.20.1 | — |
| Forge | 47.4.0 及以上 | `loaderVersion="[47,)"` |
| [MCphone](https://github.com/november521/mcphone) | **0.10.2 及以上** | **必需前置**，未安装将无法启动 |

客户端与服务端**都需要**安装本模组与 MCphone。

---

## 安装

1. 安装 Minecraft 1.20.1 + Forge 47.4.0（或更高）。
2. 下载 [MCphone](https://github.com/november521/mcphone) 并放入 `mods/`。
3. 从 [Releases](../../releases) 下载 `block_finance_V*.jar`，放入 `mods/`。
4. 启动游戏，进入世界后即可使用。

---

## 快速上手

1. **合成交易平台**：按配方做出「便捷交易平台」并放置，右键打开金融交易终端。
2. **领钱与交易**：终端内买卖现货；手机里的「金融」App 同样可以看行情、下单。
3. **造一台电脑**：合成「电脑显示器」与「电脑机箱」，把机箱摆放在显示器**相邻**的任意方向。
4. **扩容与自动化**：用「内存条」（图书管理员处 3 金锭购买）插入机箱扩容；
   在显示器控制台里选择机箱方向、设定角色与规则，机箱即可自动买入/卖出/囤货。

详细操作见 [使用指南](../../wiki/使用指南)。

---

## 版本与更新

- 完整更新记录：[CHANGELOG.md](CHANGELOG.md)
- 发布包与安装说明：[Releases](../../releases)
- 版本号规则：`主.次.修订`，修订位从 1 递增到 10 后进位（例如 `4.4.10 → 4.5.1`）。

---

## 从源码构建

需要 JDK 17 与 Gradle 8.x：

```bash
# 1) 放入前置模组（仓库不附带第三方 jar）
#    下载 MCphone 0.10.2，放到 libs/MCphone-0.10.2.jar

# 2) 构建
gradle build

# 产物：build/libs/generated_mod-<版本>.jar
```

更多细节（离线构建、常见报错）见 [docs/BUILD.md](docs/BUILD.md)。

---

## 目录结构

```
src/main/java/cn/blockforge/generated/
├── generatedmod/              金融模组本体
│   ├── api/client/            可复用 UI 组件（公开 API，可被其它模组调用）
│   ├── api/client/phone/      MCphone 横屏 App/页面接入层
│   ├── client/                现货/期货/股票/显示器/机箱界面
│   ├── block/ blockentity/    交易平台、公共市场终端
│   ├── data/                  行情、持仓、存档数据
│   ├── menu/ network/         容器菜单与网络包
│   └── AutoTradeManager.java  机箱自动化交易引擎（服务端）
└── mod3ce985ee/               电脑设备（显示器 / 机箱）
    ├── client/                外壳绘制与自适应几何
    └── ComputerTower*.java    机箱容器、容量与元件布局

src/main/resources/
├── assets/generated_mod/      贴图、语言文件、拼音表
├── assets/mod_3ce985ee/       电脑设备贴图与语言文件
└── data/                      配方、战利品表、物品标签
```

---

## 许可

本项目使用 [MIT 许可证](LICENSE)。

**前置模组 MCphone 由其作者 november521 独立发布与授权，本仓库不包含其代码或二进制文件。**

---

## 致谢

- [MCphone](https://github.com/november521/mcphone) —— 手机框架与 App SPI，本模组的手机端完全建立在其之上。
- Minecraft Forge 与社区文档。
