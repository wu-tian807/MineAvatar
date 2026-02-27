package com.mineavatar.entity;

import com.mineavatar.MineAvatar;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * Agent entity — a command-driven humanoid mob.
 *
 * Inherits PathfinderMob for built-in GroundPathNavigation and LookControl.
 * No AI goals registered; the agent only acts on external commands (moveTo, lookAt).
 *
 * When MmdSkin mod is present and a model is assigned via {@link #setMmdModel(String)},
 * the renderer will use the specified PMX model instead of the default Steve skin.
 */
public class AgentEntity extends PathfinderMob {

    private static final EntityDataAccessor<String> AGENT_NAME =
            SynchedEntityData.defineId(AgentEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID =
            SynchedEntityData.defineId(AgentEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<String> MMD_MODEL =
            SynchedEntityData.defineId(AgentEntity.class, EntityDataSerializers.STRING);

    @Nullable
    private Entity lookTarget;

    @Nullable
    private BlockPos lookBlockTarget;

    public AgentEntity(EntityType<? extends AgentEntity> entityType, Level level) {
        super(entityType, level);
        this.setCustomNameVisible(true);
        this.setPersistenceRequired();
        this.setCanPickUpLoot(true);
    }

    private static final int REGEN_INTERVAL_TICKS = 80;

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3F)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.ATTACK_SPEED, 4.0)
                .add(Attributes.LUCK)
                .add(Attributes.BLOCK_INTERACTION_RANGE, 4.5)
                .add(Attributes.ENTITY_INTERACTION_RANGE, 3.0)
                .add(Attributes.BLOCK_BREAK_SPEED)
                .add(Attributes.SNEAKING_SPEED)
                .add(Attributes.MINING_EFFICIENCY)
                .add(Attributes.SWEEPING_DAMAGE_RATIO);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(AGENT_NAME, "Agent");
        builder.define(OWNER_UUID, Optional.empty());
        builder.define(MMD_MODEL, "");
    }

    @Override
    protected void registerGoals() {
        // No AI goals — the agent is purely command-driven.
    }

    // ========== Core Actions ==========

    /**
     * Navigate to coordinates using Mojang's built-in PathNavigation.
     * @return true if a path was successfully started
     */
    public boolean commandMoveTo(double x, double y, double z) {
        boolean started = this.getNavigation().moveTo(x, y, z, 1.0);
        if (started) {
            MineAvatar.LOGGER.debug("Agent '{}' moving to ({}, {}, {})", getAgentName(), x, y, z);
        } else {
            MineAvatar.LOGGER.debug("Agent '{}' could not find path to ({}, {}, {})", getAgentName(), x, y, z);
        }
        return started;
    }

    /**
     * Set the agent to continuously look at a target entity.
     * Pass null to clear the look target.
     */
    public void commandLookAt(@Nullable Entity target) {
        this.lookTarget = target;
        if (target != null) {
            this.lookBlockTarget = null;
            MineAvatar.LOGGER.debug("Agent '{}' looking at {}", getAgentName(), target.getName().getString());
        }
    }

    /**
     * Set the agent to continuously face a block position.
     * Pass null to clear the block look target.
     */
    public void commandLookAtBlock(@Nullable BlockPos target) {
        this.lookBlockTarget = target;
        if (target != null) {
            this.lookTarget = null;
            MineAvatar.LOGGER.debug("Agent '{}' looking at block ({}, {}, {})",
                    getAgentName(), target.getX(), target.getY(), target.getZ());
        }
    }

    public void commandLookAtBlock(int x, int y, int z) {
        commandLookAtBlock(new BlockPos(x, y, z));
    }

    public enum AttackResult {
        SUCCESS, TARGET_DEAD, OUT_OF_RANGE, TARGET_INVULNERABLE, PEACEFUL, MISSED
    }

    /**
     * Attack a target entity using the agent's ATTACK_DAMAGE attribute.
     * Range is governed by the ENTITY_INTERACTION_RANGE attribute (default 3.0 blocks).
     */
    public AttackResult commandAttack(Entity target) {
        if (!(target instanceof LivingEntity livingTarget) || !livingTarget.isAlive()) {
            return AttackResult.TARGET_DEAD;
        }

        this.getLookControl().setLookAt(target, 30.0F, 30.0F);

        double reach = this.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
        double dist = this.distanceTo(livingTarget);
        if (dist > reach) {
            return AttackResult.OUT_OF_RANGE;
        }

        if (livingTarget instanceof Player p && (p.isCreative() || p.isSpectator())) {
            return AttackResult.TARGET_INVULNERABLE;
        }

        if (this.level().getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL
                && livingTarget instanceof Player) {
            return AttackResult.PEACEFUL;
        }

        this.swing(InteractionHand.MAIN_HAND);
        boolean hit = this.doHurtTarget(target);
        return hit ? AttackResult.SUCCESS : AttackResult.MISSED;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.updateSwingTime();
    }

    @Override
    public void tick() {
        super.tick();

        if (lookTarget != null) {
            if (lookTarget.isAlive() && lookTarget.level() == this.level()) {
                this.getLookControl().setLookAt(lookTarget, 30.0F, 30.0F);
            } else {
                lookTarget = null;
            }
        } else if (lookBlockTarget != null) {
            this.getLookControl().setLookAt(
                    lookBlockTarget.getX() + 0.5,
                    lookBlockTarget.getY() + 0.5,
                    lookBlockTarget.getZ() + 0.5);
        }

        if (!level().isClientSide && this.isAlive() && this.tickCount % REGEN_INTERVAL_TICKS == 0) {
            if (this.getHealth() < this.getMaxHealth()) {
                this.heal(1.0F);
            }
        }
    }

    // ========== Initialization ==========

    public void initAgent(String name, Player owner) {
        setAgentName(name);
        setOwnerUUID(owner.getUUID());
        setCustomName(Component.literal(name));
    }

    // ========== Interaction ==========

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide && player.getUUID().equals(getOwnerUUID())) {
            String status = getNavigation().isInProgress() ? "navigating" : "idle";
            String lookInfo = lookTarget != null ? lookTarget.getName().getString() : "nothing";
            String modelInfo = getMmdModel().isEmpty() ? "Steve" : getMmdModel();
            player.sendSystemMessage(Component.literal(
                    String.format("[%s] Status: %s | Looking at: %s | Model: %s",
                            getAgentName(), status, lookInfo, modelInfo)));
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player && player.getUUID().equals(getOwnerUUID())) {
            if (!player.isShiftKeyDown()) {
                return false;
            }
        }
        return super.hurt(source, amount);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!level().isClientSide) {
            Player owner = getOwner();
            if (owner != null) {
                owner.sendSystemMessage(Component.literal(
                        String.format("[MineAvatar] Agent '%s' has died.", getAgentName())));
            }
        }
    }

    // ========== Persistence ==========

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("AgentName", getAgentName());
        if (getOwnerUUID() != null) {
            tag.putUUID("OwnerUUID", getOwnerUUID());
        }
        String mmdModel = getMmdModel();
        if (!mmdModel.isEmpty()) {
            tag.putString("MmdModel", mmdModel);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("AgentName")) {
            setAgentName(tag.getString("AgentName"));
            setCustomName(Component.literal(getAgentName()));
        }
        if (tag.hasUUID("OwnerUUID")) {
            setOwnerUUID(tag.getUUID("OwnerUUID"));
        }
        if (tag.contains("MmdModel")) {
            setMmdModel(tag.getString("MmdModel"));
        }
    }

    // ========== Accessors ==========

    public String getAgentName() { return entityData.get(AGENT_NAME); }
    public void setAgentName(String name) { entityData.set(AGENT_NAME, name); }

    @Nullable
    public UUID getOwnerUUID() { return entityData.get(OWNER_UUID).orElse(null); }
    public void setOwnerUUID(@Nullable UUID uuid) { entityData.set(OWNER_UUID, Optional.ofNullable(uuid)); }

    public String getMmdModel() { return entityData.get(MMD_MODEL); }
    public void setMmdModel(String model) { entityData.set(MMD_MODEL, model != null ? model : ""); }

    @Nullable
    public Player getOwner() {
        UUID ownerId = getOwnerUUID();
        if (ownerId == null) return null;
        return level().getPlayerByUUID(ownerId);
    }

    @Nullable
    public Entity getLookTarget() { return lookTarget; }
}
