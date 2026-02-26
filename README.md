# MineAvatar

**Universal Minecraft Companion Platform**

Minecraft 1.21.1 NeoForge Mod — AI Agent 实体平台。

## 当前功能

- **Agent 实体** — 基于 `PathfinderMob` 的人形实体，通过命令驱动行为
- **MoveTo** — 使用 Mojang 内置的 `PathNavigation` 导航到指定坐标
- **LookAt** — 持续注视指定实体
- **MmdSkin 可选依赖** — 预留了与 [MmdSkin](https://modrinth.com/mod/mmdskin) 的集成接口，未来支持自定义 MMD 形象

## 技术栈

| 组件 | 版本 |
|------|------|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.217 |
| Java | 21 |

## 命令

```
/mineavatar spawn <name>        — 在玩家身边生成 Agent
/mineavatar dismiss             — 遣散所有自己的 Agent
/mineavatar moveto <x> <y> <z>  — 让 Agent 导航到坐标
/mineavatar lookat <entity>     — 让 Agent 注视目标实体
```

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
- [ ] MmdSkin 自定义形象渲染
- [ ] 更多 Agent 动作（攻击、采集、跟随等）
- [ ] 多 Agent 管理与选择

## 许可证

MIT License
