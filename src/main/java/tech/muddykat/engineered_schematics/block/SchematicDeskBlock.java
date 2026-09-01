package tech.muddykat.engineered_schematics.block;

import tech.muddykat.engineered_schematics.EngineeredSchematics;
import tech.muddykat.engineered_schematics.block.entity.SchematicTableBlockEntity;

import blusunrize.immersiveengineering.api.IEProperties;
import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.common.property.Properties;
import net.minecraftforge.client.model.obj.OBJModel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SchematicDeskBlock extends Block {
    public static final PropertyDirection FACING = IEProperties.FACING_HORIZONTAL;
    public static final PropertyBool DUMMY = PropertyBool.create("dummy");

    public SchematicDeskBlock() {
        super(Material.WOOD);
        setRegistryName(EngineeredSchematics.MODID, "schematic_table_block");
        setTranslationKey(EngineeredSchematics.MODID + ".schematic_table_block");
        setCreativeTab(EngineeredSchematics.CREATIVE_TAB);
        setSoundType(SoundType.WOOD);
        setHardness(2.0F);
        setResistance(5.0F);
        setHarvestLevel("axe", 0);
        setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(DUMMY, false));
    }

    @Override @Nonnull protected BlockStateContainer createBlockState() { return new ExtendedBlockState(this, new IProperty<?>[]{FACING, DUMMY}, new IUnlistedProperty<?>[]{Properties.AnimationProperty}); }

    @SuppressWarnings("deprecation")
    @Override @Nonnull public IBlockState getStateFromMeta(int meta) { return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3)).withProperty(DUMMY, (meta & 4) != 0); }

    @Override public int getMetaFromState(IBlockState state) { return state.getValue(FACING).getHorizontalIndex() | (state.getValue(DUMMY) ? 4 : 0); }

    @SuppressWarnings("deprecation")
    @Override @Nonnull public IBlockState getExtendedState(@Nonnull IBlockState state, @Nonnull IBlockAccess world, @Nonnull BlockPos pos) {
        if (!(state instanceof IExtendedBlockState)) { return state; }
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof SchematicTableBlockEntity)) { return state; }
        IModelState modelState = new OBJModel.OBJState(((SchematicTableBlockEntity)te).compileDisplayList(), true);
        return ((IExtendedBlockState)state).withProperty(Properties.AnimationProperty, modelState);
    }

    @Override public boolean hasTileEntity(@Nonnull IBlockState state) { return true; }

    @Nullable
    @Override public TileEntity createTileEntity(@Nonnull World world, @Nonnull IBlockState state) { return new SchematicTableBlockEntity(); }

    @SuppressWarnings("deprecation")
    @Override public boolean isOpaqueCube(@Nonnull IBlockState state) { return false; }

    @SuppressWarnings("deprecation")
    @Override public boolean isFullCube(@Nonnull IBlockState state) { return false; }

    @Override @Nonnull public IBlockState getStateForPlacement(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull EnumFacing facing, float hitX, float hitY, float hitZ, int meta, @Nonnull EntityLivingBase placer, @Nonnull EnumHand hand) { return getDefaultState().withProperty(FACING, EnumFacing.fromAngle(placer.rotationYaw)).withProperty(DUMMY, false); }

    @Override public void onBlockPlacedBy(World world, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull EntityLivingBase placer, @Nonnull ItemStack stack) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof SchematicTableBlockEntity) { ((SchematicTableBlockEntity)te).placeDummy(); }
    }

    @Override public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof SchematicTableBlockEntity) {
            SchematicTableBlockEntity table = (SchematicTableBlockEntity)te;
            SchematicTableBlockEntity master = table.getGuiMaster();
            if (master != null) {
                for (ItemStack stack : master.getInventory()) {
                    if (!stack.isEmpty()) { Utils.dropStackAtPos(world, pos, stack); }
                }
                master.getInventory().clear();
            }
            table.breakDummy();
        }
        super.breakBlock(world, pos, state);
    }

    @Override public boolean onBlockActivated(World world, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull EntityPlayer player, @Nonnull EnumHand hand, @Nonnull EnumFacing facing, float hitX, float hitY, float hitZ) {
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof SchematicTableBlockEntity)) { return false; }
        SchematicTableBlockEntity master = ((SchematicTableBlockEntity)te).getGuiMaster();
        if (master == null) { return false; }
        if (!world.isRemote) { player.openGui(EngineeredSchematics.instance, SchematicTableBlockEntity.GUI_ID, world, master.getPos().getX(), master.getPos().getY(), master.getPos().getZ()); }
        return true;
    }
}
