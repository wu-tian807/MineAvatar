package com.mineavatar;

import net.neoforged.neoforge.common.ModConfigSpec;

public class MineAvatarConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.DoubleValue AGENT_MOVE_SPEED = BUILDER
            .comment("Base movement speed multiplier for agents")
            .defineInRange("agent.moveSpeed", 1.0, 0.1, 5.0);

    public static final ModConfigSpec.IntValue MAX_AGENTS_PER_PLAYER = BUILDER
            .comment("Maximum number of active agents per player")
            .defineInRange("agent.maxPerPlayer", 3, 1, 10);

    static final ModConfigSpec SPEC = BUILDER.build();
}
