package tech.muddykat.engineered_schematics.menu;

import tech.muddykat.engineered_schematics.block.entity.SchematicTableBlockEntity;

import blusunrize.immersiveengineering.api.MultiblockHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class SchematicsContainerMenu extends Container {
    public static final int BUTTON_MIRROR = 0;
    public static final int BUTTON_SELECT = 100;
    private static final int SCHEMATIC_SLOTS = 1;
    private static final Set<String> EXCLUDED = Collections.singleton("IE:Feedthrough");
    private final InventoryPlayer inventoryPlayer;
    private final SchematicTableBlockEntity tile;
    private final IInventory tableInventory;
    private final List<MultiblockHandler.IMultiblock> availableMultiblocks;
    private SchematicInventory inventorySchematic;
    private int selectedSchematic;
    private boolean mirrored;

    public SchematicsContainerMenu(InventoryPlayer inventoryPlayer, SchematicTableBlockEntity tile) {
        this.inventoryPlayer = inventoryPlayer;
        this.tile = tile;
        this.tableInventory = new SchematicTableInventory(tile);
        this.availableMultiblocks = new ArrayList<>();
        for (MultiblockHandler.IMultiblock candidate : MultiblockHandler.getMultiblocks()) {
            if (!EXCLUDED.contains(candidate.getUniqueName())) { this.availableMultiblocks.add(candidate); }
        }
        this.selectedSchematic = 0;
        this.mirrored = false;
        this.inventorySchematic = new SchematicInventory(this, selection());
        rebindSlots();
    }

    public List<MultiblockHandler.IMultiblock> getAvailableMultiblocks() { return this.availableMultiblocks; }

    public int getSelectedSchematic() { return this.selectedSchematic; }

    public boolean isMirrored() { return this.mirrored; }

    private List<MultiblockHandler.IMultiblock> selection() {
        if (this.availableMultiblocks.isEmpty()) { return Collections.emptyList(); }
        return Collections.singletonList(this.availableMultiblocks.get(this.selectedSchematic));
    }

    public final void rebindSlots() {
        this.inventorySlots.clear();
        this.inventoryItemStacks.clear();
        this.inventorySchematic = new SchematicInventory(this, selection());
        addSlotToContainer(new SchematicInputSlot(this, this.tableInventory, 0, 144, 89));
        int used = 1;
        if (!this.tableInventory.getStackInSlot(0).isEmpty() && !this.availableMultiblocks.isEmpty()) {
            this.inventorySchematic.updateOutputs(this.tableInventory);
            addSlotToContainer(new SchematicSlot(this.inventorySchematic, this.tableInventory, 0, 190, 89));
            used++;
        }
        for (; used < SCHEMATIC_SLOTS + 1; used++) {
            addSlotToContainer(new EmptySlot(this.inventorySchematic));
        }
        bindPlayerInventory();
    }

    private void bindPlayerInventory() {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlotToContainer(new Slot(this.inventoryPlayer, column + row * 9 + 9, 35 + column * 18, 137 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlotToContainer(new Slot(this.inventoryPlayer, column, 35 + column * 18, 195));
        }
    }

    public void selectSchematic(int index) {
        if (this.availableMultiblocks.isEmpty()) { return; }
        this.selectedSchematic = Math.floorMod(index, this.availableMultiblocks.size());
        rebindSlots();
    }

    public void flipSchematic() {
        this.mirrored = !this.mirrored;
        rebindSlots();
    }

    @Override public boolean enchantItem(@Nonnull EntityPlayer player, int id) {
        if (id == BUTTON_MIRROR) {
            flipSchematic();
            return true;
        }
        if (id >= BUTTON_SELECT) {
            selectSchematic(id - BUTTON_SELECT);
            return true;
        }
        return false;
    }

    @Override @Nonnull public ItemStack slotClick(int slotId, int dragType, @Nonnull ClickType clickType, @Nonnull EntityPlayer player) {
        ItemStack result = super.slotClick(slotId, dragType, clickType, player);
        this.tile.markContainingBlockForUpdate(null);
        if (!player.world.isRemote) { detectAndSendChanges(); }
        return result;
    }

    @Override @Nonnull public ItemStack transferStackInSlot(@Nonnull EntityPlayer player, int index) { return ItemStack.EMPTY; }

    @Override public boolean canInteractWith(@Nonnull EntityPlayer player) { return this.tile.getWorld().getTileEntity(this.tile.getPos()) == this.tile && player.getDistanceSq(this.tile.getPos()) <= 64.0D; }

    public static class SchematicTableInventory implements IInventory {
        private final SchematicTableBlockEntity tile;

        public SchematicTableInventory(SchematicTableBlockEntity tile) { this.tile = tile; }

        private NonNullList<ItemStack> items() { return this.tile.getInventory(); }

        @Override public int getSizeInventory() { return items().size(); }

        @Override public boolean isEmpty() {
            for (ItemStack stack : items()) {
                if (!stack.isEmpty()) { return false; }
            }
            return true;
        }

        @Override @Nonnull public ItemStack getStackInSlot(int index) { return items().get(index); }

        @Override @Nonnull public ItemStack decrStackSize(int index, int count) {
            ItemStack stack = net.minecraft.inventory.ItemStackHelper.getAndSplit(items(), index, count);
            if (!stack.isEmpty()) { markDirty(); }
            return stack;
        }

        @Override @Nonnull public ItemStack removeStackFromSlot(int index) { return net.minecraft.inventory.ItemStackHelper.getAndRemove(items(), index); }

        @Override public void setInventorySlotContents(int index, @Nonnull ItemStack stack) {
            items().set(index, stack);
            markDirty();
        }

        @Override public int getInventoryStackLimit() { return 64; }

        @Override public void markDirty() {
            this.tile.markDirty();
            this.tile.markContainingBlockForUpdate(null);
        }

        @Override public boolean isUsableByPlayer(@Nonnull EntityPlayer player) { return true; }

        @Override public void openInventory(@Nonnull EntityPlayer player) {}

        @Override public void closeInventory(@Nonnull EntityPlayer player) {}

        @Override public boolean isItemValidForSlot(int index, @Nonnull ItemStack stack) { return this.tile.isStackValid(index, stack); }

        @Override public int getField(int id) { return 0; }

        @Override public void setField(int id, int value) {}

        @Override public int getFieldCount() { return 0; }

        @Override public void clear() { items().clear(); }

        @Override @Nonnull public String getName() { return "engineered_schematics.schematic_table"; }

        @Override public boolean hasCustomName() { return false; }

        @Override @Nonnull public net.minecraft.util.text.ITextComponent getDisplayName() { return new net.minecraft.util.text.TextComponentTranslation("desc.engineered_schematics.schematic_table"); }
    }

    public static class SchematicInputSlot extends Slot {
        private final SchematicsContainerMenu menu;

        public SchematicInputSlot(SchematicsContainerMenu menu, IInventory inventory, int id, int x, int y) {
            super(inventory, id, x, y);
            this.menu = menu;
        }

        @Override public boolean isItemValid(@Nonnull ItemStack stack) { return !stack.isEmpty() && stack.getItem() == Items.PAPER; }

        @Override public void onSlotChanged() {
            super.onSlotChanged();
            this.menu.rebindSlots();
        }
    }

    public static class SchematicSlot extends Slot {
        private final IInventory inputInventory;

        public SchematicSlot(SchematicInventory inventory, IInventory inputInventory, int id, int x, int y) {
            super(inventory, id, x, y);
            this.inputInventory = inputInventory;
        }

        @Override public boolean isItemValid(@Nonnull ItemStack stack) { return false; }

        @Override @Nonnull public ItemStack onTake(@Nonnull EntityPlayer player, @Nonnull ItemStack stack) {
            ((SchematicInventory)this.inventory).reduceInputs(this.inputInventory);
            return super.onTake(player, stack);
        }
    }

    public static class EmptySlot extends Slot {
        public EmptySlot(IInventory inventory) {
            super(inventory, 0, -1000, -1000);
        }

        @Override public boolean isItemValid(@Nonnull ItemStack stack) { return false; }

        @Override public boolean canTakeStack(@Nonnull EntityPlayer player) { return false; }

        @Override @Nonnull public ItemStack getStack() { return ItemStack.EMPTY; }
    }
}
