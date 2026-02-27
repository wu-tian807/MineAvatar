package com.mineavatar.command;

import com.google.gson.JsonObject;
import com.mineavatar.action.ActionContext;
import com.mineavatar.action.ActionRegistry;
import com.mineavatar.action.ActionResult;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Game commands that route through ActionRegistry.
 * Same action handlers as WebSocket — guaranteed consistent behavior.
 */
public class MineAvatarCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mineavatar")
                .then(Commands.literal("spawn")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .then(Commands.argument("pos", Vec3Argument.vec3())
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            Vec3 pos = Vec3Argument.getVec3(ctx, "pos");
                                            JsonObject params = new JsonObject();
                                            params.addProperty("name", StringArgumentType.getString(ctx, "name"));
                                            params.addProperty("x", pos.x);
                                            params.addProperty("y", pos.y);
                                            params.addProperty("z", pos.z);
                                            return dispatch(ctx.getSource(), player, "agent.spawn", params);
                                        }))
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    JsonObject params = new JsonObject();
                                    params.addProperty("name", StringArgumentType.getString(ctx, "name"));
                                    params.addProperty("x", player.getX() + 2);
                                    params.addProperty("y", player.getY());
                                    params.addProperty("z", player.getZ());
                                    return dispatch(ctx.getSource(), player, "agent.spawn", params);
                                })))
                .then(Commands.literal("dismiss")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    JsonObject params = new JsonObject();
                                    params.addProperty("agent", StringArgumentType.getString(ctx, "name"));
                                    return dispatch(ctx.getSource(), player, "agent.dismiss", params);
                                }))
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            return dispatch(ctx.getSource(), player, "agent.dismiss", new JsonObject());
                        }))
                .then(Commands.literal("moveto")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .then(Commands.argument("pos", Vec3Argument.vec3())
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            Vec3 pos = Vec3Argument.getVec3(ctx, "pos");
                                            JsonObject params = new JsonObject();
                                            params.addProperty("agent", StringArgumentType.getString(ctx, "name"));
                                            params.addProperty("x", pos.x);
                                            params.addProperty("y", pos.y);
                                            params.addProperty("z", pos.z);
                                            return dispatch(ctx.getSource(), player, "agent.moveTo", params);
                                        }))))
                .then(Commands.literal("stop")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    JsonObject params = new JsonObject();
                                    params.addProperty("agent", StringArgumentType.getString(ctx, "name"));
                                    return dispatch(ctx.getSource(), player, "agent.stop", params);
                                })))
                .then(Commands.literal("lookat")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .then(Commands.literal("clear")
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            JsonObject params = new JsonObject();
                                            params.addProperty("agent", StringArgumentType.getString(ctx, "name"));
                                            return dispatch(ctx.getSource(), player, "agent.lookClear", params);
                                        }))
                                .then(Commands.argument("target", EntityArgument.entity())
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            Entity target = EntityArgument.getEntity(ctx, "target");
                                            JsonObject params = new JsonObject();
                                            params.addProperty("agent", StringArgumentType.getString(ctx, "name"));
                                            params.addProperty("target", target.getStringUUID());
                                            return dispatch(ctx.getSource(), player, "agent.lookAt", params);
                                        }))))
                .then(Commands.literal("attack")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .then(Commands.argument("target", EntityArgument.entity())
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            Entity target = EntityArgument.getEntity(ctx, "target");
                                            JsonObject params = new JsonObject();
                                            params.addProperty("agent", StringArgumentType.getString(ctx, "name"));
                                            params.addProperty("target", target.getStringUUID());
                                            return dispatch(ctx.getSource(), player, "agent.attack", params);
                                        }))))
                .then(Commands.literal("chat")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .then(Commands.argument("message", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            JsonObject params = new JsonObject();
                                            params.addProperty("agent", StringArgumentType.getString(ctx, "name"));
                                            params.addProperty("message", StringArgumentType.getString(ctx, "message"));
                                            return dispatch(ctx.getSource(), player, "agent.chat", params);
                                        }))))
                .then(Commands.literal("model")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .then(Commands.literal("clear")
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            JsonObject params = new JsonObject();
                                            params.addProperty("agent", StringArgumentType.getString(ctx, "name"));
                                            params.addProperty("modelFolder", "");
                                            return dispatch(ctx.getSource(), player, "agent.setModel", params);
                                        }))
                                .then(Commands.argument("modelFolder", StringArgumentType.string())
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            JsonObject params = new JsonObject();
                                            params.addProperty("agent", StringArgumentType.getString(ctx, "name"));
                                            params.addProperty("modelFolder", StringArgumentType.getString(ctx, "modelFolder"));
                                            return dispatch(ctx.getSource(), player, "agent.setModel", params);
                                        }))))
                .then(Commands.literal("status")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    JsonObject params = new JsonObject();
                                    params.addProperty("agent", StringArgumentType.getString(ctx, "name"));
                                    return dispatch(ctx.getSource(), player, "perception.self", params);
                                })))
                .then(Commands.literal("list")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            return dispatch(ctx.getSource(), player, "perception.agents", new JsonObject());
                        }))
        );
    }

    /**
     * Dispatch through ActionRegistry.
     * Silent on success; only shows errors to prevent silent failures during testing.
     * In production, results go to the agent via TCP, not to the player's chat.
     */
    private static int dispatch(CommandSourceStack source, ServerPlayer player, String method, JsonObject params) {
        ActionContext ctx = new ActionContext(source.getServer());
        ActionResult result = ActionRegistry.get().dispatch(method, ctx, params);

        if (!result.isSuccess()) {
            source.sendFailure(Component.literal("[MineAvatar] " + result.toReadable()));
        }
        return result.isSuccess() ? 1 : 0;
    }
}
