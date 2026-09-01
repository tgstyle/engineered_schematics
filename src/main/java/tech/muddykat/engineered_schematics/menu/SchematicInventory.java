package tech.muddykat.engineered_schematics.menu;

import tech.muddykat.engineered_schematics.EngineeredSchematics;
import tech.muddykat.engineered_schematics.item.ESSchematicSettings;
import tech.muddykat.engineered_schematics.registry.ESRegistry;

import blusunrize.immersiveengineering.api.MultiblockHandler;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Rotation;

import java.util.List;

public class SchematicInventory extends InventoryBasic {
    private final List<MultiblockHandler.IMultiblock> schematics;
    private final SchematicsContainerMenu menu;

    public SchematicInventory(SchematicsContainerMenu menu, List<MultiblockHandler.IMultiblock> schematics) {
        super("engineered_schematics.schematics", false, Math.max(1, schematics.size()));
        this.schematics = schematics;
        this.menu = menu;
    }

    public void updateOutputs(IInventory input) {
        if (input.getStackInSlot(0).isEmpty()) { return; }
        for (int i = 0; i < this.schematics.size(); i++) {
            MultiblockHandler.IMultiblock multiblock = this.schematics.get(i);
            ItemStack schematic = new ItemStack(ESRegistry.SCHEMATIC_ITEM);
            ESSchematicSettings settings = new ESSchematicSettings(schematic);
            settings.setMultiblock(multiblock);
            settings.setMirror(this.menu.isMirrored());
            settings.setRotation(Rotation.NONE);
            settings.setPlaced(false);
            if (EngineeredSchematics.hasFormationItem(multiblock.getUniqueName())) { settings.setFormationTool(EngineeredSchematics.getFormationItem(multiblock.getUniqueName())); }
            settings.applyTo(schematic);
            setInventorySlotContents(i, schematic);
        }
    }

    public void reduceInputs(IInventory input) {
        consumePaper(input);
        updateOutputs(input);
    }

    private static void consumePaper(IInventory input) {
        ItemStack paper = input.getStackInSlot(0);
        if (paper.isEmpty()) { return; }
        if (paper.getItem() != Items.PAPER) {
            EngineeredSchematics.LOGGER.warn("The schematic table had an input that was not paper");
            return;
        }
        input.decrStackSize(0, 1);
    }
}
