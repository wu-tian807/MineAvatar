package com.mineavatar.client;

import com.mineavatar.entity.AgentEntity;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.ArrowLayer;
import net.minecraft.client.renderer.entity.layers.BeeStingerLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.SpinAttackEffectLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders the AgentEntity using the standard player (Steve) model as a placeholder.
 * Includes armor, arrow, bee stinger, and spin attack layers to match player visuals.
 *
 * TODO: When MmdSkin is present, this renderer should be replaced or extended
 *  to support custom PMX model rendering via MmdSkin's API.
 */
public class AgentEntityRenderer extends HumanoidMobRenderer<AgentEntity, HumanoidModel<AgentEntity>> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");

    public AgentEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
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
    public ResourceLocation getTextureLocation(AgentEntity entity) {
        return TEXTURE;
    }
}
