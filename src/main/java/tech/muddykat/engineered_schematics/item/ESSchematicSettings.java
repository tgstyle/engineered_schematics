package tech.muddykat.engineered_schematics.item;

import tech.muddykat.engineered_schematics.util.ESMultiblocks;

import blusunrize.immersiveengineering.api.MultiblockHandler;
import blusunrize.immersiveengineering.common.IEContent;
import blusunrize.immersiveengineering.common.items.ItemIETool;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;

public class ESSchematicSettings {
    public static final String KEY_SELF = "settings";
    public static final String KEY_MULTIBLOCK = "multiblock";
    public static final String KEY_MIRROR = "mirror";
    public static final String KEY_PLACED = "placed";
    public static final String KEY_ROTATION = "rotation";
    public static final String KEY_POSITION = "pos";
    public static final String KEY_FORMATION_TOOL = "formation_tool";
    private ItemStack formationTool;
    private Rotation rotation;
    private BlockPos pos = null;
    private MultiblockHandler.IMultiblock multiblock = null;
    private boolean mirror;
    private boolean isPlaced;

    public ESSchematicSettings(@Nullable final ItemStack stack) { this(readSettings(stack)); }

    public ESSchematicSettings(@Nullable NBTTagCompound settingsNbt) {
        if (settingsNbt == null || settingsNbt.isEmpty()) {
            this.rotation = Rotation.NONE;
            this.mirror = false;
            this.isPlaced = false;
            this.formationTool = defaultFormationTool();
        }
        else {
            this.rotation = Rotation.values()[settingsNbt.hasKey(KEY_ROTATION) ? settingsNbt.getInteger(KEY_ROTATION) : 0];
            this.mirror = settingsNbt.getBoolean(KEY_MIRROR);
            this.isPlaced = settingsNbt.getBoolean(KEY_PLACED);
            this.formationTool = new ItemStack(settingsNbt.getCompoundTag(KEY_FORMATION_TOOL));
            if (settingsNbt.hasKey(KEY_MULTIBLOCK, Constants.NBT.TAG_STRING)) { this.multiblock = ESMultiblocks.getByUniqueName(settingsNbt.getString(KEY_MULTIBLOCK)); }
            if (settingsNbt.hasKey(KEY_POSITION, Constants.NBT.TAG_COMPOUND)) {
                NBTTagCompound posNbt = settingsNbt.getCompoundTag(KEY_POSITION);
                this.pos = new BlockPos(posNbt.getInteger("x"), posNbt.getInteger("y"), posNbt.getInteger("z"));
            }
        }
    }

    public static ItemStack defaultFormationTool() { return new ItemStack(IEContent.itemTool, 1, ItemIETool.HAMMER_META); }

    public static boolean hasSettings(@Nullable ItemStack stack) { return stack != null && !stack.isEmpty() && stack.getSubCompound(KEY_SELF) != null; }

    @Nullable
    private static NBTTagCompound readSettings(@Nullable final ItemStack stack) {
        if (stack == null || stack.isEmpty()) { return null; }
        NBTTagCompound settingsNbt = stack.getSubCompound(KEY_SELF);
        return settingsNbt == null ? new NBTTagCompound() : settingsNbt;
    }

    public void setRotation(Rotation rotation) { this.rotation = rotation; }

    public void setMultiblock(@Nullable MultiblockHandler.IMultiblock multiblock) { this.multiblock = multiblock; }

    public void setFormationTool(ItemStack tool) { this.formationTool = tool; }

    public void setMirror(boolean mirror) { this.mirror = mirror; }

    public void setPlaced(boolean isPlaced) { this.isPlaced = isPlaced; }

    public void setPos(@Nullable BlockPos pos) { this.pos = pos; }

    public Rotation getRotation() { return this.rotation; }

    public boolean isMirrored() { return this.mirror; }

    public boolean isPlaced() { return this.isPlaced; }

    @Nullable
    public BlockPos getPos() { return this.pos; }

    @Nullable
    public MultiblockHandler.IMultiblock getMultiblock() { return this.multiblock; }

    public ItemStack getFormationTool() { return this.formationTool; }

    public NBTTagCompound toNbt() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger(KEY_ROTATION, this.rotation.ordinal());
        nbt.setBoolean(KEY_MIRROR, this.mirror);
        nbt.setBoolean(KEY_PLACED, this.isPlaced);
        if (this.multiblock != null) { nbt.setString(KEY_MULTIBLOCK, this.multiblock.getUniqueName()); }
        if (this.formationTool != null) { nbt.setTag(KEY_FORMATION_TOOL, this.formationTool.writeToNBT(new NBTTagCompound())); }
        if (this.pos != null) {
            NBTTagCompound posNbt = new NBTTagCompound();
            posNbt.setInteger("x", this.pos.getX());
            posNbt.setInteger("y", this.pos.getY());
            posNbt.setInteger("z", this.pos.getZ());
            nbt.setTag(KEY_POSITION, posNbt);
        }
        return nbt;
    }

    public void applyTo(ItemStack stack) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            stack.setTagCompound(nbt);
        }
        nbt.setTag(KEY_SELF, toNbt());
    }

    @Override public String toString() { return "\"Settings\":[" + toNbt().toString() + "]"; }
}
