package com.mineavatar.command;

import com.mineavatar.MineAvatar;
import com.mineavatar.entity.AgentEntity;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class MineAvatarCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mineavatar")
                .then(Commands.literal("spawn")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    String name = StringArgumentType.getString(ctx, "name");
                                    return spawnAgent(ctx.getSource(), player, name);
                                })))
                .then(Commands.literal("dismiss")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            return dismissAgents(ctx.getSource(), player);
                        }))
                .then(Commands.literal("moveto")
                        .then(Commands.argument("pos", Vec3Argument.vec3())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    Vec3 pos = Vec3Argument.getVec3(ctx, "pos");
                                    return moveToCommand(ctx.getSource(), player, pos);
                                })))
                .then(Commands.literal("lookat")
                        .then(Commands.literal("clear")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    return lookAtClear(ctx.getSource(), player);
                                }))
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    Entity target = EntityArgument.getEntity(ctx, "target");
                                    return lookAtCommand(ctx.getSource(), player, target);
                                })))
                .then(Commands.literal("attack")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    Entity target = EntityArgument.getEntity(ctx, "target");
                                    return attackCommand(ctx.getSource(), player, target);
                                })))
        );
    }

    private static int spawnAgent(CommandSourceStack source, ServerPlayer player, String name) {
        ServerLevel level = player.serverLevel();

        AgentEntity agent = MineAvatar.AGENT_ENTITY.get().create(level);
        if (agent == null) {
            source.sendFailure(Component.literal("Failed to create agent entity."));
            return 0;
        }

        agent.moveTo(player.getX() + 2, player.getY(), player.getZ(), player.getYRot(), 0);
        agent.initAgent(name, player);
        level.addFreshEntity(agent);

        source.sendSuccess(() -> Component.translatable("mineavatar.message.agent_spawned", name), true);
        return 1;
    }

    private static int dismissAgents(CommandSourceStack source, ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        int dismissed = 0;

        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof AgentEntity agent
                    && player.getUUID().equals(agent.getOwnerUUID())) {
                agent.remove(Entity.RemovalReason.DISCARDED);
                dismissed++;
            }
        }

        if (dismissed > 0) {
            int count = dismissed;
            source.sendSuccess(() -> Component.translatable("mineavatar.message.agent_dismissed", count), true);
        } else {
            source.sendFailure(Component.literal("No agents found."));
        }
        return dismissed;
    }

    private static int moveToCommand(CommandSourceStack source, ServerPlayer player, Vec3 pos) {
        AgentEntity agent = findOwnedAgent(player);
        if (agent == null) {
            source.sendFailure(Component.literal("No agent found. Use /mineavatar spawn <name> first."));
            return 0;
        }

        boolean success = agent.commandMoveTo(pos.x, pos.y, pos.z);
        if (success) {
            source.sendSuccess(() -> Component.literal(
                    String.format("Agent '%s' moving to (%.1f, %.1f, %.1f)",
                            agent.getAgentName(), pos.x, pos.y, pos.z)), false);
        } else {
            source.sendFailure(Component.literal(
                    String.format("Agent '%s' cannot find path to (%.1f, %.1f, %.1f)",
                            agent.getAgentName(), pos.x, pos.y, pos.z)));
        }
        return success ? 1 : 0;
    }

    private static int lookAtCommand(CommandSourceStack source, ServerPlayer player, Entity target) {
        AgentEntity agent = findOwnedAgent(player);
        if (agent == null) {
            source.sendFailure(Component.literal("No agent found. Use /mineavatar spawn <name> first."));
            return 0;
        }

        agent.commandLookAt(target);
        source.sendSuccess(() -> Component.literal(
                String.format("Agent '%s' now looking at %s",
                        agent.getAgentName(), target.getName().getString())), false);
        return 1;
    }

    private static int lookAtClear(CommandSourceStack source, ServerPlayer player) {
        AgentEntity agent = findOwnedAgent(player);
        if (agent == null) {
            source.sendFailure(Component.literal("No agent found. Use /mineavatar spawn <name> first."));
            return 0;
        }

        agent.commandLookAt(null);
        source.sendSuccess(() -> Component.literal(
                String.format("Agent '%s' stopped looking at target.", agent.getAgentName())), false);
        return 1;
    }

    private static int attackCommand(CommandSourceStack source, ServerPlayer player, Entity target) {
        AgentEntity agent = findOwnedAgent(player);
        if (agent == null) {
            source.sendFailure(Component.literal("No agent found. Use /mineavatar spawn <name> first."));
            return 0;
        }

        boolean success = agent.commandAttack(target);
        if (success) {
            source.sendSuccess(() -> Component.literal(
                    String.format("Agent '%s' attacked %s",
                            agent.getAgentName(), target.getName().getString())), false);
        } else {
            source.sendFailure(Component.literal(
                    String.format("Agent '%s' failed to attack %s",
                            agent.getAgentName(), target.getName().getString())));
        }
        return success ? 1 : 0;
    }

    /**
     * Find the first agent entity owned by the given player in the same level.
     */
    @javax.annotation.Nullable
    private static AgentEntity findOwnedAgent(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof AgentEntity agent
                    && player.getUUID().equals(agent.getOwnerUUID())) {
                return agent;
            }
        }
        return null;
    }
}
