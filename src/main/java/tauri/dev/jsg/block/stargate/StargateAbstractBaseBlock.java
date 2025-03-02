package tauri.dev.jsg.block.stargate;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import tauri.dev.jsg.JSG;
import tauri.dev.jsg.block.JSGBlock;
import tauri.dev.jsg.creativetabs.JSGCreativeTabsHandler;
import tauri.dev.jsg.stargate.EnumMemberVariant;
import tauri.dev.jsg.stargate.merging.StargateAbstractMergeHelper;
import tauri.dev.jsg.tileentity.stargate.StargateAbstractBaseTile;
import tauri.dev.jsg.util.main.JSGProps;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public abstract class StargateAbstractBaseBlock extends JSGBlock {

    public StargateAbstractBaseBlock(String blockName) {
        super(Material.IRON);

        setRegistryName(JSG.MOD_ID + ":" + blockName);
        setUnlocalizedName(JSG.MOD_ID + "." + blockName);

        setSoundType(SoundType.METAL);
        setCreativeTab(JSGCreativeTabsHandler.JSG_GATES_CREATIVE_TAB);

        setDefaultState(blockState.getBaseState().withProperty(JSGProps.FACING_HORIZONTAL, EnumFacing.NORTH).withProperty(JSGProps.RENDER_BLOCK, true));

        setLightOpacity(0);
        setHardness(3.0f);
        setResistance(60.0f);
        setHarvestLevel("pickaxe", 3);
    }

    // -----------------------------------
    // Explosions

    @Override
    public void onBlockDestroyedByExplosion(World worldIn, BlockPos pos, @Nonnull Explosion explosionIn) {
        worldIn.newExplosion(null, pos.getX(), pos.getY(), pos.getZ(), 30, true, true).doExplosionA();
    }


    // --------------------------------------------------------------------------------------
    // Block states

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, JSGProps.FACING_HORIZONTAL, JSGProps.RENDER_BLOCK);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return (state.getValue(JSGProps.RENDER_BLOCK) ? 0x04 : 0) | state.getValue(JSGProps.FACING_HORIZONTAL).getHorizontalIndex();
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(JSGProps.RENDER_BLOCK, (meta & 0x04) != 0).withProperty(JSGProps.FACING_HORIZONTAL, EnumFacing.getHorizontal(meta & 0x03));
    }


    // ------------------------------------------------------------------------
    // Block behavior

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            if (!player.isSneaking() && !tryAutobuild(player, world, pos, hand)) {
                showGateInfo(player, hand, world, pos);
            }
        }

        return !player.isSneaking();
    }

    protected abstract void showGateInfo(EntityPlayer player, EnumHand hand, World world, BlockPos pos);

    protected boolean tryAutobuild(EntityPlayer player, World world, BlockPos basePos, EnumHand hand) {
        final StargateAbstractBaseTile gateTile = (StargateAbstractBaseTile) world.getTileEntity(basePos);
        final EnumFacing facing = gateTile.getFacing();

        StargateAbstractMergeHelper mergeHelper = gateTile.getMergeHelper();
        ItemStack stack = player.getHeldItem(hand);

        if (!gateTile.isMerged()) {

            // This check ensures that stack represents matching member block.
            EnumMemberVariant variant = mergeHelper.getMemberVariantFromItemStack(stack);

            if (variant != null) {
                List<BlockPos> posList = mergeHelper.getAbsentBlockPositions(world, basePos, facing, variant);

                if (!posList.isEmpty()) {
                    BlockPos pos = posList.get(0);

                    if (world.getBlockState(pos).getBlock().isReplaceable(world, pos)) {
                        IBlockState memberState = mergeHelper.getMemberBlock().getDefaultState();
                        world.setBlockState(pos, createMemberState(memberState, facing, stack.getMetadata()));

                        SoundType soundtype = memberState.getBlock().getSoundType(memberState, world, pos, player);
                        world.playSound(null, pos, soundtype.getBreakSound(), SoundCategory.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);

                        if (!player.capabilities.isCreativeMode) stack.shrink(1);

                        // If it was the last chevron/ring
                        if (posList.size() == 1)
                            gateTile.updateMergeState(gateTile.getMergeHelper().checkBlocks(world, basePos, facing), facing);

                        return true;
                    }
                }
            } // variant == null, wrong block held
        }

        return false;
    }

    protected abstract IBlockState createMemberState(IBlockState memberState, EnumFacing facing, int meta);

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote) {
            StargateAbstractBaseTile gateTile = (StargateAbstractBaseTile) world.getTileEntity(pos);
            gateTile.updateMergeState(false, state.getValue(JSGProps.FACING_HORIZONTAL));
            gateTile.onBlockBroken();
        }
    }

    @Override
    public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest) {
        if (willHarvest) return true; //If it will harvest, delay deletion of the block until after getDrops
        return super.removedByPlayer(state, world, pos, player, willHarvest);
    }

    @Override
    public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state, @Nullable TileEntity te, ItemStack tool) {
        super.harvestBlock(world, player, pos, state, te, tool);
        world.setBlockToAir(pos);
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        StargateAbstractBaseTile gateTile = (StargateAbstractBaseTile) world.getTileEntity(pos);
        gateTile.refresh();
    }

    // --------------------------------------------------------------------------------------
    // TileEntity

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public abstract TileEntity createTileEntity(World world, IBlockState state);


    // --------------------------------------------------------------------------------------
    // Rendering

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        if (state.getValue(JSGProps.RENDER_BLOCK)) return EnumBlockRenderType.MODEL;
        else return EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullBlock(IBlockState state) {
        return false;
    }

    @Override
    public boolean renderHighlight(IBlockState state) {
        return state.getValue(JSGProps.RENDER_BLOCK);
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }
}
