package tech.muddykat.engineered_schematics.helper;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import java.util.Map;

public class BorderState {
    private boolean topEdge = true;
    private boolean rightEdge = true;
    private boolean bottomEdge = true;
    private boolean leftEdge = true;
    private boolean topRightCorner = true;
    private boolean bottomRightCorner = true;
    private boolean bottomLeftCorner = true;
    private boolean topLeftCorner = true;

    public boolean hasTopEdge() { return this.topEdge; }

    public boolean hasRightEdge() { return this.rightEdge; }

    public boolean hasBottomEdge() { return this.bottomEdge; }

    public boolean hasLeftEdge() { return this.leftEdge; }

    public boolean hasTopRightCorner() { return this.topRightCorner; }

    public boolean hasBottomRightCorner() { return this.bottomRightCorner; }

    public boolean hasBottomLeftCorner() { return this.bottomLeftCorner; }

    public boolean hasTopLeftCorner() { return this.topLeftCorner; }

    public void updateEdge(EnumFacing dir, boolean visible, EnumFacing facing) {
        if (dir == EnumFacing.UP) { this.topEdge = visible; }
        else if (dir == EnumFacing.DOWN) { this.bottomEdge = visible; }
        else if (dir == facing.rotateY()) { this.rightEdge = visible; }
        else if (dir == facing.rotateYCCW()) { this.leftEdge = visible; }
    }

    public void updateCorners(Map<EnumFacing, Boolean> adjacent, Map<BlockPos, Boolean> diagonals, EnumFacing facing, BlockPos pos) {
        this.topRightCorner = !(cornerFilled(adjacent, diagonals, pos, EnumFacing.UP, facing.rotateY()));
        this.bottomRightCorner = !(cornerFilled(adjacent, diagonals, pos, EnumFacing.DOWN, facing.rotateY()));
        this.bottomLeftCorner = !(cornerFilled(adjacent, diagonals, pos, EnumFacing.DOWN, facing.rotateYCCW()));
        this.topLeftCorner = !(cornerFilled(adjacent, diagonals, pos, EnumFacing.UP, facing.rotateYCCW()));
    }

    private static boolean cornerFilled(Map<EnumFacing, Boolean> adjacent, Map<BlockPos, Boolean> diagonals, BlockPos pos, EnumFacing vertical, EnumFacing lateral) {
        Boolean diagonal = diagonals.get(pos.offset(vertical).offset(lateral));
        return Boolean.TRUE.equals(adjacent.get(vertical)) && Boolean.TRUE.equals(adjacent.get(lateral)) && Boolean.TRUE.equals(diagonal);
    }

    public void updateFromNBT(NBTTagCompound nbt) {
        this.topEdge = nbt.getBoolean("topEdge");
        this.rightEdge = nbt.getBoolean("rightEdge");
        this.bottomEdge = nbt.getBoolean("bottomEdge");
        this.leftEdge = nbt.getBoolean("leftEdge");
        this.topRightCorner = nbt.getBoolean("topRightCorner");
        this.bottomRightCorner = nbt.getBoolean("bottomRightCorner");
        this.bottomLeftCorner = nbt.getBoolean("bottomLeftCorner");
        this.topLeftCorner = nbt.getBoolean("topLeftCorner");
    }

    public void writeToNBT(NBTTagCompound nbt) {
        nbt.setBoolean("topEdge", this.topEdge);
        nbt.setBoolean("rightEdge", this.rightEdge);
        nbt.setBoolean("bottomEdge", this.bottomEdge);
        nbt.setBoolean("leftEdge", this.leftEdge);
        nbt.setBoolean("topRightCorner", this.topRightCorner);
        nbt.setBoolean("bottomRightCorner", this.bottomRightCorner);
        nbt.setBoolean("bottomLeftCorner", this.bottomLeftCorner);
        nbt.setBoolean("topLeftCorner", this.topLeftCorner);
    }
}
