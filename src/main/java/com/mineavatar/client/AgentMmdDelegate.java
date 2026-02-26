package com.mineavatar.client;

import com.mineavatar.entity.AgentEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.shiroha.mmdskin.renderer.animation.MMDAnimManager;
import com.shiroha.mmdskin.renderer.core.EntityAnimState;
import com.shiroha.mmdskin.renderer.core.RenderContext;
import com.shiroha.mmdskin.renderer.core.RenderParams;
import com.shiroha.mmdskin.renderer.model.MMDModelManager;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import org.joml.Vector3f;

/**
 * MmdSkin rendering delegate with full animation state resolution for AgentEntity.
 *
 * <p>Layer design (mirrors MmdSkin's player animation system):
 * <ul>
 *   <li>Layer 0 — full-body base: idle, walk, sprint, sneak, swim, climb, fly, ride, die, sleep …
 *   <li>Layer 1 — upper-body overlay: swingRight/Left, itemRight/Left (eat, drink, bow, shield …)
 * </ul>
 */
public final class AgentMmdDelegate {

    private AgentMmdDelegate() {}

    public static boolean render(AgentEntity entity, float entityYaw, float tickDelta,
                                 PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                 String modelName) {
        MMDModelManager.Model model = MMDModelManager.GetModel(modelName, entity.getStringUUID());
        if (model == null) return false;

        model.loadModelProperties(false);
        float size = parseSize(model);

        RenderParams params = new RenderParams();
        resolveLayer0(entity, model, tickDelta, params);
        resolveLayer1(entity, model);

        poseStack.pushPose();
        if (entity.isBaby()) {
            poseStack.scale(0.5f, 0.5f, 0.5f);
        }
        poseStack.scale(size, size, size);
        RenderSystem.setShader(GameRenderer::getRendertypeEntityCutoutNoCullShader);
        model.model.render(entity, params.bodyYaw, params.bodyPitch, params.translation,
                tickDelta, poseStack, packedLight, RenderContext.WORLD);
        poseStack.popPose();
        return true;
    }

    // ==================== Layer 0: full-body base ====================

    private static void resolveLayer0(AgentEntity entity, MMDModelManager.Model model,
                                      float tickDelta, RenderParams params) {
        params.bodyYaw = Mth.rotLerp(tickDelta, entity.yBodyRotO, entity.yBodyRot);
        params.bodyPitch = 0.0f;
        params.translation = new Vector3f(0.0f);

        // --- highest priority ---

        if (entity.getHealth() <= 0.0f) {
            setLayer0(model, EntityAnimState.State.Die);
            return;
        }

        if (entity.isSleeping()) {
            if (entity.getBedOrientation() != null) {
                params.bodyYaw = entity.getBedOrientation().toYRot() + 180.0f;
            }
            setLayer0(model, EntityAnimState.State.Sleep);
            return;
        }

        // --- special movement ---

        if (entity.isFallFlying()) {
            setLayer0(model, EntityAnimState.State.ElytraFly);
            return;
        }

        if (entity.isSwimming()) {
            setLayer0(model, EntityAnimState.State.Swim);
            return;
        }

        if (entity.onClimbable()) {
            double dy = entity.getY() - entity.yo;
            if (dy > 0.01)       setLayer0(model, EntityAnimState.State.OnClimbableUp);
            else if (dy < -0.01) setLayer0(model, EntityAnimState.State.OnClimbableDown);
            else                 setLayer0(model, EntityAnimState.State.OnClimbable);
            return;
        }

        // --- vehicle ---

        if (entity.isPassenger()) {
            setLayer0(model, EntityAnimState.State.Ride);
            return;
        }
        if (entity.isVehicle()) {
            setLayer0(model, hasMovement(entity)
                    ? EntityAnimState.State.Driven
                    : EntityAnimState.State.Ridden);
            return;
        }

        // --- ground movement ---

        boolean moving = hasMovement(entity);

        if (entity.isCrouching()) {
            setLayer0(model, EntityAnimState.State.Sneak);
        } else if (moving && entity.isSprinting()) {
            setLayer0(model, EntityAnimState.State.Sprint);
        } else if (moving) {
            setLayer0(model, EntityAnimState.State.Walk);
        } else {
            setLayer0(model, EntityAnimState.State.Idle);
        }
    }

    // ==================== Layer 1: upper-body overlay ====================

    private static void resolveLayer1(AgentEntity entity, MMDModelManager.Model model) {
        // Swing (attack) takes priority — plays once
        if (entity.swinging) {
            EntityAnimState.State state = (entity.swingingArm == InteractionHand.MAIN_HAND)
                    ? EntityAnimState.State.SwingRight
                    : EntityAnimState.State.SwingLeft;
            setLayer1(model, state);
            return;
        }

        // Item use (eat, drink, bow, shield, etc.)
        if (entity.isUsingItem()) {
            EntityAnimState.State state = (entity.getUsedItemHand() == InteractionHand.MAIN_HAND)
                    ? EntityAnimState.State.ItemRight
                    : EntityAnimState.State.ItemLeft;
            setLayer1(model, state);
            return;
        }

        // 动作结束 — 彻底清除 layer 1，让 layer 0 基础动画完全显示
        // 注意：即使将来有了 setLayerLoop，这里的 changeAnim(0,1) 仍然需要保留，
        // 因为 setLayerLoop 只控制"播放期间"是否循环，而 changeAnim(0,1) 负责"动作结束后"清除叠加层。
        if (model.entityData.stateLayers[1] != null) {
            model.entityData.stateLayers[1] = null;
            model.model.changeAnim(0, 1);
        }
    }

    // ==================== Helpers ====================

    private static void setLayer0(MMDModelManager.Model model, EntityAnimState.State state) {
        if (model.entityData.stateLayers[0] != state) {
            model.entityData.stateLayers[0] = state;
            model.model.changeAnim(
                    MMDAnimManager.GetAnimModel(model.model, state.propertyName), 0);
        }
    }

    /**
     * 切换 layer 1 动画（仅在状态变化时触发）。
     *
     * TODO: MmdSkin 1.0.3 Modrinth 版缺少 IMMDModel.setLayerLoop / NativeFunc.SetLayerLoop。
     *  当依赖升级到包含该方法的版本后，应在 changeAnim 之后加入循环控制：
     *    - 挥动动画（SwingRight/Left）：setLayerLoop(1, false)  → 播放一次后停留末帧，防止在 swinging 期间反复重播
     *    - 物品动画（ItemRight/Left）：  setLayerLoop(1, true)   → 持续循环直到松手
     *  setLayerLoop 与 resolveLayer1 末尾的 changeAnim(0,1) 互补：
     *    setLayerLoop  → 控制动画"播放中"的行为（单次 vs 循环）
     *    changeAnim(0,1) → 动作结束后彻底移除 layer 1 叠加
     */
    private static void setLayer1(MMDModelManager.Model model, EntityAnimState.State state) {
        if (model.entityData.stateLayers[1] != state) {
            model.entityData.stateLayers[1] = state;
            long anim = MMDAnimManager.GetAnimModel(model.model, state.propertyName);
            if (anim != 0) {
                model.model.changeAnim(anim, 1);
            }
        }
    }

    private static boolean hasMovement(AgentEntity entity) {
        return entity.getX() - entity.xo != 0.0 || entity.getZ() - entity.zo != 0.0;
    }

    private static float parseSize(MMDModelManager.Model model) {
        String value = model.properties.getProperty("size");
        if (value == null) return 1.0f;
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return 1.0f;
        }
    }
}
