package com.mineavatar.action;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mineavatar.MineAvatar;
import com.mineavatar.entity.AgentEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Central registry for all agent actions.
 * Both the command system and WebSocket JSON-RPC route through here.
 *
 * Pure execution layer — no ownership, no permissions.
 * Authorization is the caller's responsibility.
 */
public class ActionRegistry {

    private static final ActionRegistry INSTANCE = new ActionRegistry();
    private final Map<String, ActionHandler> handlers = new HashMap<>();

    public static ActionRegistry get() { return INSTANCE; }

    private ActionRegistry() {
        registerDefaults();
    }

    public void register(String method, ActionHandler handler) {
        handlers.put(method, handler);
    }

    /**
     * Dispatch an action by method name.
     * Must be called on the server main thread.
     */
    public ActionResult dispatch(String method, ActionContext ctx, JsonObject params) {
        ActionHandler handler = handlers.get(method);
        if (handler == null) {
            return ActionResult.fail("METHOD_NOT_FOUND",
                    "Unknown method: " + method,
                    "Available: " + String.join(", ", handlers.keySet()));
        }
        try {
            return handler.execute(ctx, params);
        } catch (Exception e) {
            MineAvatar.LOGGER.error("Action '{}' threw exception", method, e);
            return ActionResult.fail("INTERNAL_ERROR", e.getMessage());
        }
    }

    public boolean hasMethod(String method) {
        return handlers.containsKey(method);
    }

    // ═══════════════════════════════════════════════════════════════
    //  Helpers
    // ═══════════════════════════════════════════════════════════════

    @Nullable
    private static AgentEntity resolveAgent(ActionContext ctx, JsonObject params, ActionResult[] errorOut) {
        String name = params.has("agent") ? params.get("agent").getAsString() : null;
        if (name == null || name.isEmpty()) {
            errorOut[0] = ActionResult.fail("MISSING_PARAM", "Parameter 'agent' is required");
            return null;
        }
        AgentEntity agent = ctx.findAgent(name);
        if (agent == null) {
            errorOut[0] = ActionResult.fail("AGENT_NOT_FOUND",
                    "No agent named '" + name + "' found");
            return null;
        }
        if (!agent.isAlive()) {
            errorOut[0] = ActionResult.fail("AGENT_DEAD", "Agent '" + name + "' is dead");
            return null;
        }
        return agent;
    }

    @Nullable
    private static Entity resolveTarget(ActionContext ctx, JsonObject params, ActionResult[] errorOut) {
        String targetStr = params.has("target") ? params.get("target").getAsString() : null;
        if (targetStr == null || targetStr.isEmpty()) {
            errorOut[0] = ActionResult.fail("MISSING_PARAM", "Parameter 'target' is required");
            return null;
        }
        Entity target = ctx.resolveEntity(targetStr);
        if (target == null) {
            errorOut[0] = ActionResult.fail("TARGET_NOT_FOUND",
                    "No entity found with UUID '" + targetStr + "'");
            return null;
        }
        return target;
    }

    // ═══════════════════════════════════════════════════════════════
    //  Default handlers
    // ═══════════════════════════════════════════════════════════════

    private void registerDefaults() {
        // ── Navigation ──
        register("agent.moveTo", this::handleMoveTo);
        register("agent.stop", this::handleStop);

        // ── Look ──
        register("agent.lookAt", this::handleLookAt);
        register("agent.lookAtBlock", this::handleLookAtBlock);
        register("agent.lookClear", this::handleLookClear);

        // ── Combat ──
        register("agent.attack", this::handleAttack);

        // ── Social ──
        register("agent.chat", this::handleChat);

        // ── Lifecycle ──
        register("agent.spawn", this::handleSpawn);
        register("agent.dismiss", this::handleDismiss);
        register("agent.setModel", this::handleSetModel);

        // ── Perception ──
        register("perception.self", this::handlePerceptionSelf);
        register("perception.agents", this::handlePerceptionAgents);
    }

    // ── Navigation ──────────────────────────────────────────────────

    private ActionResult handleMoveTo(ActionContext ctx, JsonObject params) {
        ActionResult[] err = {null};
        AgentEntity agent = resolveAgent(ctx, params, err);
        if (agent == null) return err[0];

        if (!params.has("x") || !params.has("y") || !params.has("z")) {
            return ActionResult.fail("MISSING_PARAM", "Parameters 'x', 'y', 'z' are required");
        }
        double x = params.get("x").getAsDouble();
        double y = params.get("y").getAsDouble();
        double z = params.get("z").getAsDouble();

        boolean started = agent.commandMoveTo(x, y, z);
        if (started) {
            JsonObject data = new JsonObject();
            data.addProperty("message", String.format("Moving to (%.1f, %.1f, %.1f)", x, y, z));
            return ActionResult.ok(data);
        } else {
            return ActionResult.fail("PATH_NOT_FOUND",
                    String.format("Cannot find path to (%.1f, %.1f, %.1f)", x, y, z));
        }
    }

    private ActionResult handleStop(ActionContext ctx, JsonObject params) {
        ActionResult[] err = {null};
        AgentEntity agent = resolveAgent(ctx, params, err);
        if (agent == null) return err[0];

        agent.getNavigation().stop();
        agent.commandLookAt(null);
        agent.commandLookAtBlock(null);
        return ActionResult.ok();
    }

    // ── Look ────────────────────────────────────────────────────────

    private ActionResult handleLookAt(ActionContext ctx, JsonObject params) {
        ActionResult[] err = {null};
        AgentEntity agent = resolveAgent(ctx, params, err);
        if (agent == null) return err[0];

        Entity target = resolveTarget(ctx, params, err);
        if (target == null) return err[0];

        agent.commandLookAt(target);
        JsonObject data = new JsonObject();
        data.addProperty("message", "Looking at " + target.getName().getString());
        return ActionResult.ok(data);
    }

    private ActionResult handleLookAtBlock(ActionContext ctx, JsonObject params) {
        ActionResult[] err = {null};
        AgentEntity agent = resolveAgent(ctx, params, err);
        if (agent == null) return err[0];

        if (!params.has("x") || !params.has("y") || !params.has("z")) {
            return ActionResult.fail("MISSING_PARAM", "Parameters 'x', 'y', 'z' are required");
        }
        int x = params.get("x").getAsInt();
        int y = params.get("y").getAsInt();
        int z = params.get("z").getAsInt();

        agent.commandLookAtBlock(x, y, z);
        return ActionResult.ok();
    }

    private ActionResult handleLookClear(ActionContext ctx, JsonObject params) {
        ActionResult[] err = {null};
        AgentEntity agent = resolveAgent(ctx, params, err);
        if (agent == null) return err[0];

        agent.commandLookAt(null);
        agent.commandLookAtBlock(null);
        return ActionResult.ok();
    }

    // ── Combat ──────────────────────────────────────────────────────

    private ActionResult handleAttack(ActionContext ctx, JsonObject params) {
        ActionResult[] err = {null};
        AgentEntity agent = resolveAgent(ctx, params, err);
        if (agent == null) return err[0];

        Entity target = resolveTarget(ctx, params, err);
        if (target == null) return err[0];

        AgentEntity.AttackResult result = agent.commandAttack(target);
        JsonObject data = new JsonObject();
        data.addProperty("result", result.name());

        switch (result) {
            case SUCCESS -> {
                data.addProperty("target", target.getName().getString());
                data.addProperty("distance", agent.distanceTo(target));
                return ActionResult.ok(data);
            }
            case TARGET_DEAD -> { return ActionResult.fail("TARGET_DEAD", "Target is dead or invalid"); }
            case OUT_OF_RANGE -> {
                return ActionResult.fail("OUT_OF_RANGE",
                        String.format("Target is %.1f blocks away, reach is %.1f",
                                agent.distanceTo(target),
                                agent.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE)));
            }
            case TARGET_INVULNERABLE -> { return ActionResult.fail("TARGET_INVULNERABLE", "Target is in creative/spectator mode"); }
            case PEACEFUL -> { return ActionResult.fail("PEACEFUL_MODE", "Cannot hurt players in peaceful difficulty"); }
            case MISSED -> { return ActionResult.fail("MISSED", "Attack did not land"); }
            default -> { return ActionResult.fail("UNKNOWN", "Unexpected result: " + result); }
        }
    }

    // ── Social ──────────────────────────────────────────────────────

    private ActionResult handleChat(ActionContext ctx, JsonObject params) {
        ActionResult[] err = {null};
        AgentEntity agent = resolveAgent(ctx, params, err);
        if (agent == null) return err[0];

        String message = params.has("message") ? params.get("message").getAsString() : null;
        if (message == null || message.isEmpty()) {
            return ActionResult.fail("MISSING_PARAM", "Parameter 'message' is required");
        }

        Component chatMsg = Component.literal("<" + agent.getAgentName() + "> " + message);
        ctx.getServer().getPlayerList().broadcastSystemMessage(chatMsg, false);
        return ActionResult.ok();
    }

    // ── Lifecycle ───────────────────────────────────────────────────

    /**
     * Spawn an agent at specified coordinates.
     * params: name (required), x/y/z (optional — defaults to world spawn),
     *         dimension (optional — defaults to overworld)
     */
    private ActionResult handleSpawn(ActionContext ctx, JsonObject params) {
        String name = params.has("name") ? params.get("name").getAsString() : null;
        if (name == null || name.isEmpty()) {
            return ActionResult.fail("MISSING_PARAM", "Parameter 'name' is required");
        }

        if (ctx.findAgent(name) != null) {
            return ActionResult.fail("AGENT_EXISTS", "An agent named '" + name + "' already exists");
        }

        ServerLevel level = ctx.getOverworld();
        AgentEntity agent = MineAvatar.AGENT_ENTITY.get().create(level);
        if (agent == null) {
            return ActionResult.fail("INTERNAL_ERROR", "Failed to create agent entity");
        }

        double x, y, z;
        if (params.has("x") && params.has("y") && params.has("z")) {
            x = params.get("x").getAsDouble();
            y = params.get("y").getAsDouble();
            z = params.get("z").getAsDouble();
        } else {
            var spawnPos = level.getSharedSpawnPos();
            x = spawnPos.getX() + 0.5;
            y = spawnPos.getY();
            z = spawnPos.getZ() + 0.5;
        }

        agent.moveTo(x, y, z, 0, 0);
        agent.setAgentName(name);
        agent.setCustomName(Component.literal(name));
        level.addFreshEntity(agent);

        JsonObject data = new JsonObject();
        data.addProperty("name", name);
        data.addProperty("uuid", agent.getStringUUID());
        JsonObject pos = new JsonObject();
        pos.addProperty("x", x);
        pos.addProperty("y", y);
        pos.addProperty("z", z);
        data.add("position", pos);
        return ActionResult.ok(data);
    }

    private ActionResult handleDismiss(ActionContext ctx, JsonObject params) {
        if (params.has("agent")) {
            ActionResult[] err = {null};
            AgentEntity agent = resolveAgent(ctx, params, err);
            if (agent == null) return err[0];

            agent.remove(Entity.RemovalReason.DISCARDED);
            JsonObject data = new JsonObject();
            data.addProperty("dismissed", 1);
            return ActionResult.ok(data);
        }

        List<AgentEntity> agents = ctx.listAgents();
        if (agents.isEmpty()) {
            return ActionResult.fail("AGENT_NOT_FOUND", "No agents found");
        }
        int count = agents.size();
        agents.forEach(a -> a.remove(Entity.RemovalReason.DISCARDED));
        JsonObject data = new JsonObject();
        data.addProperty("dismissed", count);
        return ActionResult.ok(data);
    }

    private ActionResult handleSetModel(ActionContext ctx, JsonObject params) {
        ActionResult[] err = {null};
        AgentEntity agent = resolveAgent(ctx, params, err);
        if (agent == null) return err[0];

        String modelFolder = params.has("modelFolder") ? params.get("modelFolder").getAsString() : "";
        agent.setMmdModel(modelFolder);

        JsonObject data = new JsonObject();
        data.addProperty("model", modelFolder.isEmpty() ? "Steve (default)" : modelFolder);
        return ActionResult.ok(data);
    }

    // ── Perception ──────────────────────────────────────────────────

    private ActionResult handlePerceptionSelf(ActionContext ctx, JsonObject params) {
        ActionResult[] err = {null};
        AgentEntity agent = resolveAgent(ctx, params, err);
        if (agent == null) return err[0];

        JsonObject data = new JsonObject();
        data.addProperty("name", agent.getAgentName());

        JsonObject pos = new JsonObject();
        pos.addProperty("x", Math.round(agent.getX() * 10.0) / 10.0);
        pos.addProperty("y", Math.round(agent.getY() * 10.0) / 10.0);
        pos.addProperty("z", Math.round(agent.getZ() * 10.0) / 10.0);
        data.add("position", pos);

        data.addProperty("health", agent.getHealth());
        data.addProperty("maxHealth", agent.getMaxHealth());
        data.addProperty("yaw", agent.getYRot());
        data.addProperty("pitch", agent.getXRot());
        data.addProperty("isNavigating", agent.getNavigation().isInProgress());
        data.addProperty("onGround", agent.onGround());
        data.addProperty("inWater", agent.isInWater());

        Entity lookTarget = agent.getLookTarget();
        data.addProperty("lookTarget", lookTarget != null ? lookTarget.getName().getString() : null);

        data.addProperty("uuid", agent.getStringUUID());
        return ActionResult.ok(data);
    }

    private ActionResult handlePerceptionAgents(ActionContext ctx, JsonObject params) {
        List<AgentEntity> agents = ctx.listAgents();
        JsonArray arr = new JsonArray();
        for (AgentEntity agent : agents) {
            JsonObject entry = new JsonObject();
            entry.addProperty("name", agent.getAgentName());
            entry.addProperty("uuid", agent.getStringUUID());
            entry.addProperty("health", agent.getHealth());
            entry.addProperty("alive", agent.isAlive());
            arr.add(entry);
        }
        JsonObject data = new JsonObject();
        data.add("agents", arr);
        return ActionResult.ok(data);
    }
}
