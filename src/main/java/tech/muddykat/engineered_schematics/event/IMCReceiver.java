package tech.muddykat.engineered_schematics.event;

import tech.muddykat.engineered_schematics.EngineeredSchematics;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.event.FMLInterModComms;

public class IMCReceiver {
    private static final String TEMPLATE_KEY = "formation_item";
    private static final String KEY_MULTIBLOCK = "multiblock";
    private static final String KEY_ITEM = "item";

    private IMCReceiver() {}

    public static void processIMC(FMLInterModComms.IMCEvent event) {
        for (FMLInterModComms.IMCMessage message : event.getMessages()) {
            if (!TEMPLATE_KEY.equals(message.key)) { continue; }
            if (!message.isNBTMessage()) {
                EngineeredSchematics.LOGGER.warn("Ignoring a {} message from {} that did not carry NBT", TEMPLATE_KEY, message.getSender());
                continue;
            }
            NBTTagCompound nbt = message.getNBTValue();
            if (!nbt.hasKey(KEY_MULTIBLOCK, Constants.NBT.TAG_STRING) || !nbt.hasKey(KEY_ITEM, Constants.NBT.TAG_COMPOUND)) {
                EngineeredSchematics.LOGGER.warn("Ignoring a {} message from {} that was missing \"{}\" or \"{}\"", TEMPLATE_KEY, message.getSender(), KEY_MULTIBLOCK, KEY_ITEM);
                continue;
            }
            String uniqueName = nbt.getString(KEY_MULTIBLOCK);
            ItemStack item = new ItemStack(nbt.getCompoundTag(KEY_ITEM));
            if (item.isEmpty()) {
                EngineeredSchematics.LOGGER.warn("Ignoring a {} message from {} for {} that carried an empty item", TEMPLATE_KEY, message.getSender(), uniqueName);
                continue;
            }
            EngineeredSchematics.setTemplateFormationItem(uniqueName, item);
            EngineeredSchematics.LOGGER.info("Received IMC formation item from {}: {} <- {}", message.getSender(), uniqueName, item.getDisplayName());
        }
    }
}
