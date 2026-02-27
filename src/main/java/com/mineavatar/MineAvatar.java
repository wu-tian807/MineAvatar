package com.mineavatar;

import com.mineavatar.action.ActionRegistry;
import com.mineavatar.command.MineAvatarCommands;
import com.mineavatar.entity.AgentEntity;
import com.mineavatar.network.AgentTcpServer;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import javax.annotation.Nullable;

@Mod(MineAvatar.MODID)
public class MineAvatar {

    public static final String MODID = "mineavatar";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, MODID);

    public static final DeferredItem<Item> AGENT_SPAWNER = ITEMS.registerSimpleItem(
            "agent_spawner", new Item.Properties().stacksTo(1));

    public static final DeferredHolder<EntityType<?>, EntityType<AgentEntity>> AGENT_ENTITY =
            ENTITY_TYPES.register("agent", () -> EntityType.Builder
                    .of(AgentEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(64)
                    .build("agent"));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MINEAVATAR_TAB =
            CREATIVE_MODE_TABS.register("mineavatar_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.mineavatar"))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> AGENT_SPAWNER.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(AGENT_SPAWNER.get());
                    }).build());

    @Nullable
    private AgentTcpServer tcpServer;

    public MineAvatar(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerEntityAttributes);

        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);

        modContainer.registerConfig(ModConfig.Type.COMMON, MineAvatarConfig.SPEC);

        LOGGER.info("MineAvatar initializing");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("MineAvatar common setup complete");
    }

    private void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(AGENT_ENTITY.get(), AgentEntity.createAttributes().build());
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        MineAvatarCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        if (MineAvatarConfig.WS_ENABLED.get()) {
            int port = MineAvatarConfig.WS_PORT.get();
            tcpServer = new AgentTcpServer(port);
            tcpServer.start(event.getServer(), ActionRegistry.get());
        }
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        if (tcpServer != null) {
            tcpServer.stop();
            tcpServer = null;
        }
    }
}
