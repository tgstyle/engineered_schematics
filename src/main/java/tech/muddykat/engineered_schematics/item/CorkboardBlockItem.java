package tech.muddykat.engineered_schematics.item;

import tech.muddykat.engineered_schematics.EngineeredSchematics;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;

import java.util.Objects;

public class CorkboardBlockItem extends ItemBlock {
    public CorkboardBlockItem(Block block) {
        super(block);
        setRegistryName(Objects.requireNonNull(block.getRegistryName()));
        setTranslationKey(EngineeredSchematics.MODID + ".corkboard");
        setCreativeTab(EngineeredSchematics.CREATIVE_TAB);
    }
}
