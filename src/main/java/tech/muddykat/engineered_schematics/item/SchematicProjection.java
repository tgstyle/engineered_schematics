package tech.muddykat.engineered_schematics.item;

import tech.muddykat.engineered_schematics.util.ESMultiblocks;

import blusunrize.immersiveengineering.api.MultiblockHandler;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class SchematicProjection {
    final MultiblockHandler.IMultiblock multiblock;
    final PlacementSettings settings = new PlacementSettings();
    final Int2ObjectMap<List<Template.BlockInfo>> layers = new Int2ObjectArrayMap<>();
    final BlockPos.MutableBlockPos offset = new BlockPos.MutableBlockPos();
    final Vec3i size;
    private final boolean reversedLength;
    final int blockcount;
    boolean isDirty = true;

    public SchematicProjection(@Nonnull MultiblockHandler.IMultiblock multiblock) {
        Objects.requireNonNull(multiblock, "Multiblock cannot be null!");
        this.multiblock = multiblock;
        this.size = getSize(multiblock);
        this.reversedLength = ESMultiblocks.hasReversedLength(multiblock);
        ItemStack[][][] structure = multiblock.getStructureManual();
        int count = 0;
        if (structure != null) {
            for (int h = 0; h < structure.length; h++) {
                for (int l = 0; l < structure[h].length; l++) {
                    for (int w = 0; w < structure[h][l].length; w++) {
                        ItemStack stack = structure[h][l][w];
                        if (stack == null || stack.isEmpty()) { continue; }
                        IBlockState state = multiblock.getBlockstateFromStack(h * (this.size.getZ() * this.size.getX()) + l * this.size.getX() + w, stack);
                        if (state == null) { continue; }
                        List<Template.BlockInfo> list = this.layers.get(h);
                        if (list == null) {
                            list = new ArrayList<>();
                            this.layers.put(h, list);
                        }
                        list.add(new Template.BlockInfo(mirrored(w, h, l), state, null));
                        count++;
                    }
                }
            }
        }
        this.blockcount = count;
    }

    public ItemStack stackFor(Info info) {
        ItemStack[][][] structure = this.multiblock.getStructureManual();
        if (structure == null) { return ItemStack.EMPTY; }
        BlockPos raw = info.tBlockInfo.pos;
        int l = this.reversedLength ? this.size.getZ() - 1 - raw.getZ() : raw.getZ();
        if (raw.getY() < 0 || raw.getY() >= structure.length || l < 0 || l >= structure[raw.getY()].length) { return ItemStack.EMPTY; }
        ItemStack[] row = structure[raw.getY()][l];
        return raw.getX() < 0 || raw.getX() >= row.length ? ItemStack.EMPTY : row[raw.getX()];
    }

    private BlockPos mirrored(int w, int h, int l) { return new BlockPos(w, h, this.reversedLength ? this.size.getZ() - 1 - l : l); }

    public static Vec3i getSize(@Nonnull MultiblockHandler.IMultiblock multiblock) {
        ItemStack[][][] structure = multiblock.getStructureManual();
        if (structure == null) { return Vec3i.NULL_VECTOR; }
        int length = 0;
        int width = 0;
        for (ItemStack[][] layer : structure) {
            if (layer.length > length) { length = layer.length; }
            for (ItemStack[] row : layer) {
                if (row.length > width) { width = row.length; }
            }
        }
        return new Vec3i(width, structure.length, length);
    }

    public void setRotation(Rotation rotation) {
        if (this.settings.getRotation() != rotation) {
            this.settings.setRotation(rotation);
            this.isDirty = true;
        }
    }

    public void setFlip(boolean mirror) {
        Mirror m = mirror ? Mirror.FRONT_BACK : Mirror.NONE;
        if (this.settings.getMirror() != m) {
            this.settings.setMirror(m);
            this.isDirty = true;
        }
    }

    public Vec3i getSize() { return this.size; }

    public int getBlockCount() { return this.blockcount; }

    public int getLayerCount() { return this.layers.size(); }

    public MultiblockHandler.IMultiblock getMultiblock() { return this.multiblock; }

    @Override public boolean equals(Object obj) {
        if (this == obj) { return true; }
        if (obj instanceof SchematicProjection) {
            SchematicProjection other = (SchematicProjection) obj;
            return this.multiblock.getUniqueName().equals(other.multiblock.getUniqueName()) && this.settings.getMirror() == other.settings.getMirror() && this.settings.getRotation() == other.settings.getRotation();
        }
        return false;
    }

    @Override public int hashCode() { return Objects.hash(this.multiblock.getUniqueName(), this.settings.getMirror(), this.settings.getRotation()); }

    public boolean process(int layer, Predicate<Info> predicate) {
        updateData();
        List<Template.BlockInfo> blocks = this.layers.get(layer);
        if (blocks == null) { return false; }
        for (Template.BlockInfo info : blocks) {
            if (predicate.test(new Info(this, info))) { return true; }
        }
        return false;
    }

    public void processAll(BiPredicate<Integer, Info> predicate) {
        updateData();
        for (int layer = 0; layer < getLayerCount(); layer++) {
            List<Template.BlockInfo> blocks = this.layers.get(layer);
            if (blocks == null) { continue; }
            for (Template.BlockInfo info : blocks) {
                if (predicate.test(layer, new Info(this, info))) { return; }
            }
        }
    }

    public BlockPos toLocal(BlockPos raw) {
        updateData();
        return Template.transformedBlockPos(this.settings, raw).subtract(this.offset);
    }

    private void updateData() {
        if (!this.isDirty) { return; }
        this.isDirty = false;
        boolean mirrored = this.settings.getMirror() == Mirror.FRONT_BACK;
        Rotation rotation = this.settings.getRotation();
        if (!mirrored) {
            switch (rotation) {
                case CLOCKWISE_90: this.offset.setPos(1 - this.size.getZ(), 0, 0); break;
                case CLOCKWISE_180: this.offset.setPos(1 - this.size.getX(), 0, 1 - this.size.getZ()); break;
                case COUNTERCLOCKWISE_90: this.offset.setPos(0, 0, 1 - this.size.getX()); break;
                default: this.offset.setPos(0, 0, 0); break;
            }
        }
        else {
            switch (rotation) {
                case NONE: this.offset.setPos(1 - this.size.getX(), 0, 0); break;
                case CLOCKWISE_90: this.offset.setPos(1 - this.size.getZ(), 0, 1 - this.size.getX()); break;
                case CLOCKWISE_180: this.offset.setPos(0, 0, 1 - this.size.getZ()); break;
                default: this.offset.setPos(0, 0, 0); break;
            }
        }
        int x = ((rotation.ordinal() % 2 == 0) ? this.size.getX() : this.size.getZ()) / 2;
        int z = ((rotation.ordinal() % 2 == 0) ? this.size.getZ() : this.size.getX()) / 2;
        this.offset.setPos(this.offset.getX() + x, this.offset.getY(), this.offset.getZ() + z);
    }

    public static final class Info {
        public final PlacementSettings settings;
        public final MultiblockHandler.IMultiblock multiblock;
        public final BlockPos tPos;
        public final Template.BlockInfo tBlockInfo;

        public Info(SchematicProjection projection, Template.BlockInfo templateBlockInfo) {
            this.multiblock = projection.multiblock;
            this.settings = projection.settings;
            this.tBlockInfo = templateBlockInfo;
            this.tPos = Template.transformedBlockPos(this.settings, templateBlockInfo.pos).subtract(projection.offset);
        }

        public IBlockState getModifiedState() { return this.tBlockInfo.blockState.withMirror(this.settings.getMirror()).withRotation(this.settings.getRotation()); }

        public IBlockState getRawState() { return this.tBlockInfo.blockState; }
    }
}
