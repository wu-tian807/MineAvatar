package com.mineavatar;

import net.neoforged.neoforge.common.ModConfigSpec;

public class MineAvatarConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ── Agent ──
    public static final ModConfigSpec.DoubleValue AGENT_MOVE_SPEED = BUILDER
            .comment("Base movement speed multiplier for agents")
            .defineInRange("agent.moveSpeed", 1.0, 0.1, 5.0);

    public static final ModConfigSpec.IntValue MAX_AGENTS_PER_PLAYER = BUILDER
            .comment("Maximum number of active agents per player")
            .defineInRange("agent.maxPerPlayer", 3, 1, 10);

    // ── WebSocket ──
    public static final ModConfigSpec.BooleanValue WS_ENABLED = BUILDER
            .comment("Enable the WebSocket server for external agent control")
            .define("websocket.enabled", true);

    public static final ModConfigSpec.IntValue WS_PORT = BUILDER
            .comment("Port for the WebSocket server")
            .defineInRange("websocket.port", 19230, 1024, 65535);

    public static final ModConfigSpec.ConfigValue<String> WS_TOKEN = BUILDER
            .comment("Shared token for WebSocket authentication")
            .define("websocket.token", "mineavatar");

    static final ModConfigSpec SPEC = BUILDER.build();
}
