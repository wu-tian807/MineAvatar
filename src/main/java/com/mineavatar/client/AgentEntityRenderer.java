package com.mineavatar.client;

import com.mineavatar.entity.AgentEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.ArrowLayer;
import net.minecraft.client.renderer.entity.layers.BeeStingerLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.SpinAttackEffectLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders the AgentEntity using either:
 * - A custom MMD (PMX) model when MmdSkin is installed and a model is assigned, or
 * - The standard player (Steve) model as the default fallback.
 */
public class AgentEntityRenderer extends HumanoidMobRenderer<AgentEntity, PlayerModel<AgentEntity>> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");

    private static Boolean mmdSkinLoaded;

    public AgentEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.addLayer(new HumanoidArmorLayer<>(
                this,
                new HumanoidArmorModel(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidArmorModel(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()
        ));
        this.addLayer(new ArrowLayer<>(context, this));
        this.addLayer(new BeeStingerLayer<>(this));
        this.addLayer(new SpinAttackEffectLayer<>(this, context.getModelSet()));
    }

    @Override
    public void render(AgentEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        String mmdModel = entity.getMmdModel();
        if (!mmdModel.isEmpty() && isMmdSkinLoaded()) {
            boolean rendered = AgentMmdDelegate.render(
                    entity, entityYaw, partialTick, poseStack, buffer, packedLight, mmdModel);
            if (rendered) {
                if (this.shouldShowName(entity)) {
                    this.renderNameTag(entity, entity.getDisplayName(), poseStack, buffer, packedLight, partialTick);
                }
                return;
            }
            // Model still loading — fall through to Steve rendering
        }
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(AgentEntity entity) {
        return TEXTURE;
    }

    static boolean isMmdSkinLoaded() {
        if (mmdSkinLoaded == null) {
            try {
                Class.forName("com.shiroha.mmdskin.renderer.model.MMDModelManager");
                mmdSkinLoaded = true;
            } catch (ClassNotFoundException e) {
                mmdSkinLoaded = false;
            }
        }
        return mmdSkinLoaded;
    }
}
