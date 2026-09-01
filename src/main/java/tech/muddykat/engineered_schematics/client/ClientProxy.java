package tech.muddykat.engineered_schematics.client;

import tech.muddykat.engineered_schematics.EngineeredSchematics;
import tech.muddykat.engineered_schematics.block.entity.SchematicBoardBlockEntity;
import tech.muddykat.engineered_schematics.block.entity.SchematicTableBlockEntity;
import tech.muddykat.engineered_schematics.client.renderer.CorkboardRenderer;
import tech.muddykat.engineered_schematics.client.renderer.ESDynamicModel;
import tech.muddykat.engineered_schematics.client.screen.SchematicsScreen;
import tech.muddykat.engineered_schematics.common.CommonProxy;
import tech.muddykat.engineered_schematics.event.SchematicPickBlockHandler;
import tech.muddykat.engineered_schematics.event.SchematicRenderHandler;
import tech.muddykat.engineered_schematics.menu.SchematicsContainerMenu;
import tech.muddykat.engineered_schematics.registry.ESRegistry;

import blusunrize.immersiveengineering.api.ManualHelper;
import blusunrize.lib.manual.ManualPages;
import net.minecraft.item.ItemStack;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;

import javax.annotation.Nullable;

@SuppressWarnings("unused")
public class ClientProxy extends CommonProxy {
    private static final String CAT_ES = EngineeredSchematics.MODID;
    private static final String GUIDE = EngineeredSchematics.MODID + ":textures/misc/guide.png";

    @Override public void preInit() {
        super.preInit();
        OBJLoader.INSTANCE.addDomain(EngineeredSchematics.MODID);
        CorkboardRenderer.FRAME_EDGE = new ESDynamicModel("frame_edge");
        CorkboardRenderer.CORNER = new ESDynamicModel("corner");
        CorkboardRenderer.SCHEMATIC = new ESDynamicModel("schematic");
        ClientRegistry.bindTileEntitySpecialRenderer(SchematicBoardBlockEntity.class, new CorkboardRenderer());
    }

    @Override public void init() {
        super.init();
        MinecraftForge.EVENT_BUS.register(new SchematicRenderHandler());
        MinecraftForge.EVENT_BUS.register(new SchematicPickBlockHandler());
    }

    @Override public void postInit() {
        super.postInit();
        addManualEntries();
    }

    private static void addManualEntries() {
        ManualHelper.addEntry("es", CAT_ES,
                new ManualPages.Text(ManualHelper.getManual(), "es0"),
                new ManualPages.Text(ManualHelper.getManual(), "es1"));
        ManualHelper.addEntry("schematicTable", CAT_ES,
                new ManualPages.Crafting(ManualHelper.getManual(), "schematicTable0", new ItemStack(ESRegistry.SCHEMATIC_TABLE)));
        ManualHelper.addEntry("schematicItem", CAT_ES,
                new ManualPages.ItemDisplay(ManualHelper.getManual(), "schematicItem0", new ItemStack(ESRegistry.SCHEMATIC_ITEM)),
                new ManualPages.Image(ManualHelper.getManual(), "schematicItem1", GUIDE + ";0;0;110;47", GUIDE + ";0;47;110;58"),
                new ManualPages.Image(ManualHelper.getManual(), "schematicItem2", GUIDE + ";0;105;110;61"));
    }

    @Nullable
    @Override public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        SchematicTableBlockEntity table = getTable(world, x, y, z);
        if (id != SchematicTableBlockEntity.GUI_ID || table == null) { return null; }
        return new SchematicsScreen(new SchematicsContainerMenu(player.inventory, table));
    }
}
