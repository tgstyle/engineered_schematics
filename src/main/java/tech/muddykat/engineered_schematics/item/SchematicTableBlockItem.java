package tech.muddykat.engineered_schematics.item;

import tech.muddykat.engineered_schematics.EngineeredSchematics;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;

import java.util.Objects;

public class SchematicTableBlockItem extends ItemBlock {
    public SchematicTableBlockItem(Block block) {
        super(block);
        setRegistryName(Objects.requireNonNull(block.getRegistryName()));
        setTranslationKey(EngineeredSchematics.MODID + ".schematic_table_block");
        setMaxStackSize(1);
        setCreativeTab(EngineeredSchematics.CREATIVE_TAB);
    }
}
