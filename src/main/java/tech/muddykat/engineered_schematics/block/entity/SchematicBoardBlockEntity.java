package tech.muddykat.engineered_schematics.block.entity;

import tech.muddykat.engineered_schematics.block.SchematicCorkboardBlock;
import tech.muddykat.engineered_schematics.helper.BorderState;
import tech.muddykat.engineered_schematics.item.SchematicItem;

import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IBlockOverlayText;
import blusunrize.immersiveengineering.common.blocks.TileEntityIEBase;
import blusunrize.immersiveengineering.common.util.Utils;
import blusunrize.immersiveengineering.common.util.inventory.IIEInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class SchematicBoardBlockEntity extends TileEntityIEBase implements IIEInventory, IBlockOverlayText {
    public static final int NUM_SLOTS = 4;
    private final NonNullList<ItemStack> inventory = NonNullList.withSize(NUM_SLOTS, ItemStack.EMPTY);
    private final BorderState borderState = new BorderState();
    private final Map<EnumFacing, Boolean> adjacentBlocks = new EnumMap<>(EnumFacing.class);
    private final Map<BlockPos, Boolean> diagonalBlocks = new HashMap<>();
    private final List<Float> randomStates = new ArrayList<>(8);

    @Override public void readCustomNBT(NBTTagCompound nbt, boolean descPacket) {
        this.inventory.clear();
        NonNullList<ItemStack> read = Utils.readInventory(nbt.getTagList("inventory", 10), NUM_SLOTS);
        for (int i = 0; i < NUM_SLOTS; i++) {
            this.inventory.set(i, read.get(i));
        }
        this.borderState.updateFromNBT(nbt);
    }

    @Override public void writeCustomNBT(NBTTagCompound nbt, boolean descPacket) {
        nbt.setTag("inventory", Utils.writeInventory(this.inventory));
        this.borderState.writeToNBT(nbt);
    }

    @Override public NonNullList<ItemStack> getInventory() { return this.inventory; }

    @Override public boolean isStackValid(int slot, ItemStack stack) { return !stack.isEmpty() && stack.getItem() instanceof SchematicItem; }

    @Override public int getSlotLimit(int slot) { return 1; }

    @Override public void doGraphicalUpdates(int slot) {
        markDirty();
        markContainingBlockForUpdate(null);
    }

    public EnumFacing getFacing() { return SchematicCorkboardBlock.facingOf(world, pos); }

    public BorderState getBorderState() { return this.borderState; }

    public List<Float> getRandomState() {
        if (this.randomStates.isEmpty()) {
            Random rand = new Random(this.pos.toLong());
            while (this.randomStates.size() < 8) {
                this.randomStates.add(rand.nextFloat());
            }
        }
        return this.randomStates;
    }

    public static int getTargetedSlot(EnumFacing side, float hitX, float hitY, float hitZ) {
        float targetU = side == EnumFacing.NORTH ? (1 - hitX) : (side == EnumFacing.SOUTH ? hitX : (side == EnumFacing.EAST ? 1 - hitZ : hitZ));
        float targetV = side == EnumFacing.UP ? 1 - hitZ : 1 - hitY;
        return targetU < 0.5F ? (targetV < 0.5F ? 0 : 2) : (targetV < 0.5F ? 1 : 3);
    }

    public boolean interact(EnumFacing side, EntityPlayer player, EnumHand hand, ItemStack heldItem, float hitX, float hitY, float hitZ) {
        int slot = getTargetedSlot(side, hitX, hitY, hitZ);
        ItemStack stackInSlot = this.inventory.get(slot);
        if (!stackInSlot.isEmpty()) {
            if (heldItem.isEmpty()) { player.setHeldItem(hand, stackInSlot); }
            else if (!world.isRemote) { player.dropItem(stackInSlot, false); }
            this.inventory.set(slot, ItemStack.EMPTY);
            doGraphicalUpdates(slot);
            return true;
        }
        if (isStackValid(slot, heldItem)) {
            ItemStack pinned = heldItem.copy();
            pinned.setCount(1);
            this.inventory.set(slot, pinned);
            heldItem.shrink(1);
            doGraphicalUpdates(slot);
            return true;
        }
        return false;
    }

    @Override @Nonnull public String[] getOverlayText(@Nonnull EntityPlayer player, @Nonnull RayTraceResult mop, boolean hammer) {
        BlockPos hit = mop.getBlockPos();
        float hitX = (float)(mop.hitVec.x - hit.getX());
        float hitY = (float)(mop.hitVec.y - hit.getY());
        float hitZ = (float)(mop.hitVec.z - hit.getZ());
        ItemStack stackInSlot = this.inventory.get(getTargetedSlot(mop.sideHit, hitX, hitY, hitZ));
        if (stackInSlot.isEmpty()) { return new String[0]; }
        return new String[]{stackInSlot.getDisplayName()};
    }

    @Override public boolean useNixieFont(@Nonnull EntityPlayer player, @Nonnull RayTraceResult mop) { return false; }

    private boolean isSameBlockType(BlockPos other) { return world.getTileEntity(other) instanceof SchematicBoardBlockEntity; }

    public void refreshAdjacentBlocks() {
        EnumFacing facing = getFacing();
        for (EnumFacing dir : EnumFacing.values()) {
            boolean hasSameBlock = isSameBlockType(this.pos.offset(dir));
            this.adjacentBlocks.put(dir, hasSameBlock);
            this.borderState.updateEdge(dir, !hasSameBlock, facing);
        }
        updateDiagonalBlocks();
        this.borderState.updateCorners(this.adjacentBlocks, this.diagonalBlocks, facing, this.pos);
        markContainingBlockForUpdate(null);
    }

    private void updateDiagonalBlocks() {
        EnumFacing facing = getFacing();
        this.diagonalBlocks.clear();
        cacheDiagonal(EnumFacing.UP, facing.rotateY());
        cacheDiagonal(EnumFacing.UP, facing.rotateYCCW());
        cacheDiagonal(EnumFacing.DOWN, facing.rotateY());
        cacheDiagonal(EnumFacing.DOWN, facing.rotateYCCW());
    }

    private void cacheDiagonal(EnumFacing first, EnumFacing second) {
        BlockPos diagonal = this.pos.offset(first).offset(second);
        this.diagonalBlocks.put(diagonal, isSameBlockType(diagonal));
    }

    public void updateEdges(BlockPos neighborPos) {
        BlockPos delta = neighborPos.subtract(this.pos);
        int manhattan = Math.abs(delta.getX()) + Math.abs(delta.getY()) + Math.abs(delta.getZ());
        if (manhattan == 1) {
            EnumFacing side = EnumFacing.getFacingFromVector(delta.getX(), delta.getY(), delta.getZ());
            this.adjacentBlocks.put(side, isSameBlockType(neighborPos));
            this.borderState.updateEdge(side, !isSameBlockType(neighborPos), getFacing());
            updateDiagonalBlocks();
            this.borderState.updateCorners(this.adjacentBlocks, this.diagonalBlocks, getFacing(), this.pos);
            markContainingBlockForUpdate(null);
            notifyNeighborsToUpdate();
        }
        else if (manhattan == 2) {
            updateDiagonalBlocks();
            this.borderState.updateCorners(this.adjacentBlocks, this.diagonalBlocks, getFacing(), this.pos);
            markContainingBlockForUpdate(null);
        }
    }

    private void notifyNeighborsToUpdate() {
        for (EnumFacing dir : EnumFacing.values()) {
            notifyBoard(this.pos.offset(dir));
        }
    }

    private void notifyBoard(BlockPos other) {
        TileEntity te = world.getTileEntity(other);
        if (te instanceof SchematicBoardBlockEntity) { ((SchematicBoardBlockEntity)te).refreshAdjacentBlocks(); }
    }

    public void onInitialPlace() {
        getRandomState();
        refreshAdjacentBlocks();
        notifyNeighborsToUpdate();
        EnumFacing facing = getFacing();
        notifyBoard(this.pos.up().offset(facing.rotateY()));
        notifyBoard(this.pos.up().offset(facing.rotateYCCW()));
        notifyBoard(this.pos.down().offset(facing.rotateY()));
        notifyBoard(this.pos.down().offset(facing.rotateYCCW()));
    }

    @SideOnly(Side.CLIENT)
    @Override @Nonnull public net.minecraft.util.math.AxisAlignedBB getRenderBoundingBox() { return new net.minecraft.util.math.AxisAlignedBB(pos, pos.add(1, 1, 1)); }
}
