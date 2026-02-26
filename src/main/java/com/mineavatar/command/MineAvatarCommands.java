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
                        .then(Commands.argument("name", StringArgumentType.string())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    String name = StringArgumentType.getString(ctx, "name");
                                    return dismissAgent(ctx.getSource(), player, name);
                                }))
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            return dismissAllAgents(ctx.getSource(), player);
                        }))
                .then(Commands.literal("moveto")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .then(Commands.argument("pos", Vec3Argument.vec3())
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            String name = StringArgumentType.getString(ctx, "name");
                                            Vec3 pos = Vec3Argument.getVec3(ctx, "pos");
                                            return moveToCommand(ctx.getSource(), player, name, pos);
                                        }))))
                .then(Commands.literal("lookat")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .then(Commands.literal("clear")
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            String name = StringArgumentType.getString(ctx, "name");
                                            return lookAtClear(ctx.getSource(), player, name);
                                        }))
                                .then(Commands.argument("target", EntityArgument.entity())
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            String name = StringArgumentType.getString(ctx, "name");
                                            Entity target = EntityArgument.getEntity(ctx, "target");
                                            return lookAtCommand(ctx.getSource(), player, name, target);
                                        }))))
                .then(Commands.literal("attack")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .then(Commands.argument("target", EntityArgument.entity())
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            String name = StringArgumentType.getString(ctx, "name");
                                            Entity target = EntityArgument.getEntity(ctx, "target");
                                            return attackCommand(ctx.getSource(), player, name, target);
                                        }))))
                .then(Commands.literal("model")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .then(Commands.literal("clear")
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            String name = StringArgumentType.getString(ctx, "name");
                                            return modelClear(ctx.getSource(), player, name);
                                        }))
                                .then(Commands.argument("modelFolder", StringArgumentType.string())
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            String name = StringArgumentType.getString(ctx, "name");
                                            String modelFolder = StringArgumentType.getString(ctx, "modelFolder");
                                            return modelSet(ctx.getSource(), player, name, modelFolder);
                                        }))))
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

    private static int dismissAgent(CommandSourceStack source, ServerPlayer player, String name) {
        AgentEntity agent = findOwnedAgent(player, name);
        if (agent == null) {
            source.sendFailure(Component.literal(
                    String.format("No agent named '%s' found.", name)));
            return 0;
        }
        agent.remove(Entity.RemovalReason.DISCARDED);
        source.sendSuccess(() -> Component.translatable("mineavatar.message.agent_dismissed", 1), true);
        return 1;
    }

    private static int dismissAllAgents(CommandSourceStack source, ServerPlayer player) {
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

    private static int moveToCommand(CommandSourceStack source, ServerPlayer player, String name, Vec3 pos) {
        AgentEntity agent = findOwnedAgent(player, name);
        if (agent == null) {
            source.sendFailure(Component.literal(
                    String.format("No agent named '%s' found.", name)));
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

    private static int lookAtCommand(CommandSourceStack source, ServerPlayer player, String name, Entity target) {
        AgentEntity agent = findOwnedAgent(player, name);
        if (agent == null) {
            source.sendFailure(Component.literal(
                    String.format("No agent named '%s' found.", name)));
            return 0;
        }

        agent.commandLookAt(target);
        source.sendSuccess(() -> Component.literal(
                String.format("Agent '%s' now looking at %s",
                        agent.getAgentName(), target.getName().getString())), false);
        return 1;
    }

    private static int lookAtClear(CommandSourceStack source, ServerPlayer player, String name) {
        AgentEntity agent = findOwnedAgent(player, name);
        if (agent == null) {
            source.sendFailure(Component.literal(
                    String.format("No agent named '%s' found.", name)));
            return 0;
        }

        agent.commandLookAt(null);
        source.sendSuccess(() -> Component.literal(
                String.format("Agent '%s' stopped looking at target.", agent.getAgentName())), false);
        return 1;
    }

    private static int attackCommand(CommandSourceStack source, ServerPlayer player, String name, Entity target) {
        AgentEntity agent = findOwnedAgent(player, name);
        if (agent == null) {
            source.sendFailure(Component.literal(
                    String.format("No agent named '%s' found.", name)));
            return 0;
        }

        AgentEntity.AttackResult result = agent.commandAttack(target);
        switch (result) {
            case SUCCESS -> source.sendSuccess(() -> Component.literal(
                    String.format("Agent '%s' attacked %s", name, target.getName().getString())), false);
            case TARGET_DEAD -> source.sendFailure(Component.literal(
                    String.format("Agent '%s': target is dead or invalid.", name)));
            case OUT_OF_RANGE -> source.sendFailure(Component.literal(
                    String.format("Agent '%s': target '%s' out of range (%.1f blocks away, reach %.1f).",
                            name, target.getName().getString(),
                            agent.distanceTo(target),
                            agent.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE))));
            case TARGET_INVULNERABLE -> source.sendFailure(Component.literal(
                    String.format("Agent '%s': target '%s' is invulnerable (creative/spectator mode).",
                            name, target.getName().getString())));
            case PEACEFUL -> source.sendFailure(Component.literal(
                    String.format("Agent '%s': cannot hurt players in peaceful difficulty. Use /difficulty easy",
                            name)));
            case MISSED -> source.sendFailure(Component.literal(
                    String.format("Agent '%s': attack on '%s' did not land.",
                            name, target.getName().getString())));
        }
        return result == AgentEntity.AttackResult.SUCCESS ? 1 : 0;
    }

    private static int modelSet(CommandSourceStack source, ServerPlayer player, String name, String modelFolder) {
        AgentEntity agent = findOwnedAgent(player, name);
        if (agent == null) {
            source.sendFailure(Component.literal(
                    String.format("No agent named '%s' found.", name)));
            return 0;
        }

        agent.setMmdModel(modelFolder);
        source.sendSuccess(() -> Component.translatable(
                "mineavatar.message.model_set", name, modelFolder), false);
        return 1;
    }

    private static int modelClear(CommandSourceStack source, ServerPlayer player, String name) {
        AgentEntity agent = findOwnedAgent(player, name);
        if (agent == null) {
            source.sendFailure(Component.literal(
                    String.format("No agent named '%s' found.", name)));
            return 0;
        }

        agent.setMmdModel("");
        source.sendSuccess(() -> Component.translatable(
                "mineavatar.message.model_cleared", name), false);
        return 1;
    }

    @javax.annotation.Nullable
    private static AgentEntity findOwnedAgent(ServerPlayer player, String name) {
        ServerLevel level = player.serverLevel();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof AgentEntity agent
                    && player.getUUID().equals(agent.getOwnerUUID())
                    && agent.getAgentName().equals(name)) {
                return agent;
            }
        }
        return null;
    }
}
