# MineAvatar

**Universal Minecraft Companion Platform**

Minecraft 1.21.1 NeoForge Mod — AI Agent 实体平台。

## 当前功能

- **Agent 实体** — 基于 `PathfinderMob` 的人形实体，命名归属、数据持久化
- **MoveTo** — 使用 Mojang 内置的 `PathNavigation` 导航到指定坐标
- **LookAt** — 持续注视指定实体（支持 `clear` 取消）
- **Attack** — 命令 Agent 攻击目标，附带距离/无敌/和平等状态反馈，带击退与挥动动画
- **MMD 自定义形象** — 安装 [MmdSkin](https://modrinth.com/mod/mmdskin) 后，可通过命令为 Agent 指定 MMD 模型（PMX/PMD），自动适配全套动画状态：
  - Layer 0（全身）：idle、walk、sprint、sneak、swim、climb、elytra fly、ride、die、sleep 等
  - Layer 1（上半身叠加）：swing（攻击）、item use（进食/拉弓/举盾）等
  - 不装 MmdSkin 时自动 fallback 为默认 Steve 模型

## 技术栈

| 组件 | 版本 |
|------|------|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.217 |
| Java | 21 |
| MmdSkin（可选） | 1.0.3 |

## 命令

```
/mineavatar spawn <name>                 — 在玩家身边生成 Agent
/mineavatar dismiss                      — 遣散所有自己的 Agent
/mineavatar dismiss <name>               — 遣散指定名称的 Agent
/mineavatar moveto <name> <x> <y> <z>    — 让 Agent 导航到坐标
/mineavatar lookat <name> <entity>       — 让 Agent 注视目标实体
/mineavatar lookat <name> clear          — 取消注视
/mineavatar attack <name> <entity>       — 让 Agent 攻击目标
/mineavatar model <name> <modelFolder>   — 为 Agent 指定 MMD 模型（需安装 MmdSkin）
/mineavatar model <name> clear           — 清除 MMD 模型，恢复 Steve
```

## MMD 模型使用

1. 安装 [MmdSkin](https://modrinth.com/mod/mmdskin) mod
2. 将模型文件夹放入 `.minecraft/3d-skin/EntityPlayer/<模型名>/`，文件夹内需包含 `.pmx` 或 `.pmd` 模型文件
3. 游戏内执行 `/mineavatar model <name> <模型名>` 即可替换形象
4. 右键点击 Agent 可查看当前使用的模型名称

## 构建

```bash
./gradlew build
```

## 运行

```bash
./gradlew runClient   # 启动客户端
./gradlew runServer   # 启动服务端
```

## 路线图

- [ ] OpenClaw 智能体框架集成
- [ ] 更多 Agent 动作（采集、跟随等）
- [ ] 多 Agent 管理与选择

## 许可证

MIT License
