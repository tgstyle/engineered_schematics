package tech.muddykat.engineered_schematics.common;

import tech.muddykat.engineered_schematics.block.entity.SchematicTableBlockEntity;
import tech.muddykat.engineered_schematics.menu.SchematicsContainerMenu;
import tech.muddykat.engineered_schematics.registry.ESRegistry;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

import javax.annotation.Nullable;

public class CommonProxy implements IGuiHandler {
    public void preInit() { ESRegistry.registerTiles(); }

    public void init() {}

    public void postInit() {}

    @Nullable
    protected static SchematicTableBlockEntity getTable(World world, int x, int y, int z) {
        TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
        return te instanceof SchematicTableBlockEntity ? ((SchematicTableBlockEntity)te).getGuiMaster() : null;
    }

    @Nullable
    @Override public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        SchematicTableBlockEntity table = getTable(world, x, y, z);
        if (id != SchematicTableBlockEntity.GUI_ID || table == null) { return null; }
        return new SchematicsContainerMenu(player.inventory, table);
    }

    @Nullable
    @Override public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) { return null; }
}
