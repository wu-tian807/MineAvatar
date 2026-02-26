package com.mineavatar.client;

import com.mineavatar.entity.AgentEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.shiroha.mmdskin.renderer.animation.MMDAnimManager;
import com.shiroha.mmdskin.renderer.core.EntityAnimState;
import com.shiroha.mmdskin.renderer.core.RenderContext;
import com.shiroha.mmdskin.renderer.core.RenderParams;
import com.shiroha.mmdskin.renderer.model.MMDModelManager;
import com.shiroha.mmdskin.renderer.render.EntityAnimationResolver;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;

/**
 * Isolated MmdSkin rendering delegate for AgentEntity.
 * This class directly references MmdSkin classes and is only loaded by the JVM
 * when MmdSkin has been confirmed present via {@link AgentEntityRenderer#isMmdSkinLoaded()}.
 */
public final class AgentMmdDelegate {

    private AgentMmdDelegate() {}

    /**
     * @return true if a model was rendered, false if model is still loading (caller should fall back)
     */
    public static boolean render(AgentEntity entity, float entityYaw, float tickDelta,
                                 PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                 String modelName) {
        MMDModelManager.Model model = MMDModelManager.GetModel(modelName, entity.getStringUUID());
        if (model == null) {
            return false;
        }

        model.loadModelProperties(false);
        float size = parseSize(model);

        RenderParams params = new RenderParams();
        EntityAnimationResolver.resolve(entity, model, entityYaw, tickDelta, params);
        resolveSwing(entity, model);

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

    /**
     * Handle swing (attack) animation on layer 1, overlaid on top of the base movement animation.
     */
    private static void resolveSwing(AgentEntity entity, MMDModelManager.Model model) {
        if (entity.swinging) {
            EntityAnimState.State swingState =
                    (entity.swingingArm == InteractionHand.MAIN_HAND)
                            ? EntityAnimState.State.SwingRight
                            : EntityAnimState.State.SwingLeft;

            if (model.entityData.stateLayers[1] != swingState) {
                model.entityData.stateLayers[1] = swingState;
                long anim = MMDAnimManager.GetAnimModel(model.model, swingState.propertyName);
                if (anim != 0) {
                    model.model.changeAnim(anim, 1);
                    model.model.setLayerLoop(1, false);
                }
            }
        } else if (model.entityData.stateLayers[1] != null) {
            model.entityData.stateLayers[1] = null;
        }
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
