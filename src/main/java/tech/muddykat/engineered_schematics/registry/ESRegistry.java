package tech.muddykat.engineered_schematics.registry;

import tech.muddykat.engineered_schematics.EngineeredSchematics;
import tech.muddykat.engineered_schematics.block.SchematicCorkboardBlock;
import tech.muddykat.engineered_schematics.block.SchematicDeskBlock;
import tech.muddykat.engineered_schematics.block.entity.SchematicBoardBlockEntity;
import tech.muddykat.engineered_schematics.block.entity.SchematicTableBlockEntity;
import tech.muddykat.engineered_schematics.item.CorkboardBlockItem;
import tech.muddykat.engineered_schematics.item.SchematicItem;
import tech.muddykat.engineered_schematics.item.SchematicTableBlockItem;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Objects;

@EventBusSubscriber(modid = EngineeredSchematics.MODID)
public class ESRegistry {
    public static final SchematicItem SCHEMATIC_ITEM = new SchematicItem();
    public static final SchematicDeskBlock SCHEMATIC_TABLE = new SchematicDeskBlock();
    public static final SchematicTableBlockItem SCHEMATIC_TABLE_ITEM = new SchematicTableBlockItem(SCHEMATIC_TABLE);
    public static final SchematicCorkboardBlock CORKBOARD = new SchematicCorkboardBlock();
    public static final CorkboardBlockItem CORKBOARD_ITEM = new CorkboardBlockItem(CORKBOARD);

    private ESRegistry() {}

    public static void registerTiles() {
        GameRegistry.registerTileEntity(SchematicTableBlockEntity.class, EngineeredSchematics.rl("schematic_table"));
        GameRegistry.registerTileEntity(SchematicBoardBlockEntity.class, EngineeredSchematics.rl("corkboard"));
    }

    @SubscribeEvent public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().register(SCHEMATIC_TABLE);
        event.getRegistry().register(CORKBOARD);
    }

    @SubscribeEvent public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(SCHEMATIC_ITEM);
        event.getRegistry().register(SCHEMATIC_TABLE_ITEM);
        event.getRegistry().register(CORKBOARD_ITEM);
    }

    @EventBusSubscriber(value = Side.CLIENT, modid = EngineeredSchematics.MODID)
    public static class ClientEvents {
        @SubscribeEvent public static void registerModels(ModelRegistryEvent event) {
            registerInventoryModel(SCHEMATIC_ITEM);
            registerInventoryModel(SCHEMATIC_TABLE_ITEM);
            registerInventoryModel(CORKBOARD_ITEM);
        }

        private static void registerInventoryModel(Item item) { ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(Objects.requireNonNull(item.getRegistryName()), "inventory")); }
    }
}
