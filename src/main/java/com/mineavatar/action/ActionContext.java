package com.mineavatar.action;

import com.mineavatar.entity.AgentEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Execution context for the action layer.
 * Only holds the MinecraftServer reference — no ownership, no permissions.
 * Authorization is the caller's responsibility (command / WebSocket layer).
 */
public class ActionContext {

    private final MinecraftServer server;

    public ActionContext(MinecraftServer server) {
        this.server = server;
    }

    public MinecraftServer getServer() { return server; }

    /**
     * Find an agent by name (global, across all dimensions).
     */
    @Nullable
    public AgentEntity findAgent(String name) {
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof AgentEntity agent
                        && agent.getAgentName().equals(name)) {
                    return agent;
                }
            }
        }
        return null;
    }

    /**
     * List all agents in the server.
     */
    public List<AgentEntity> listAgents() {
        List<AgentEntity> agents = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof AgentEntity agent) {
                    agents.add(agent);
                }
            }
        }
        return agents;
    }

    /**
     * Resolve an entity by UUID string in any loaded level.
     */
    @Nullable
    public Entity resolveEntity(String uuidStr) {
        UUID uuid;
        try {
            uuid = UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(uuid);
            if (entity != null) return entity;
        }
        return null;
    }

    /**
     * Get the overworld (default dimension for spawning).
     */
    public ServerLevel getOverworld() {
        return server.overworld();
    }
}
