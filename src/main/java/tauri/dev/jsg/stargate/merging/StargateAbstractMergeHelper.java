package tauri.dev.jsg.stargate.merging;

import tauri.dev.jsg.block.stargate.StargateAbstractMemberBlock;
import tauri.dev.jsg.block.stargate.StargateMilkyWayBaseBlock;
import tauri.dev.jsg.block.stargate.StargateMilkyWayMemberBlock;
import tauri.dev.jsg.config.JSGConfig;
import tauri.dev.jsg.stargate.EnumMemberVariant;
import tauri.dev.jsg.tileentity.stargate.StargateAbstractBaseTile;
import tauri.dev.jsg.tileentity.stargate.StargateMilkyWayBaseTile;
import tauri.dev.jsg.util.JSGAxisAlignedBB;
import tauri.dev.jsg.util.BlockHelpers;
import tauri.dev.jsg.util.FacingToRotation;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

//ToDo Well, idk if getRingBlocks() should be relative... Maybe return absolute positions too. But not sure
public abstract class StargateAbstractMergeHelper {

  /**
   * @return {@link List} of {@link BlockPos} pointing to ring blocks. Positions are relative
   */
  @Nonnull
  public abstract List<BlockPos> getRingBlocks();

  /**
   * @return {@link List} of {@link BlockPos} pointing to chevron blocks. Positions are relative
   */
  @Nonnull
  public abstract List<BlockPos> getChevronBlocks();

  private BlockPos topBlock = null;

  /**
   * For Classic gate returns top chevron position (relative).
   * For Orlin's gate returns top ring position (no chevrons).
   *
   * @return Gate's top block.
   */
  public BlockPos getTopBlock() {
    // null - not initialized
    if (topBlock == null) topBlock = BlockHelpers.getHighest(getChevronBlocks());

    // Still null - chevron list empty (Orlin's gate)
    if (topBlock == null) topBlock = BlockHelpers.getHighest(getRingBlocks());

    return topBlock;
  }

  /**
   * @return {@link List} of {@link BlockPos} pointing to absent blocks of variant given. Positions are absolute.
   */
  @Nonnull
  public List<BlockPos> getAbsentBlockPositions(IBlockAccess world, BlockPos basePos, EnumFacing facing, EnumMemberVariant variant) {
    List<BlockPos> blocks = null;

    switch (variant) {
      case CHEVRON:
        blocks = getChevronBlocks();
        break;

      case RING:
        blocks = getRingBlocks();
        break;
    }

    return blocks.stream().map(pos -> pos.rotate(FacingToRotation.get(facing)).add(basePos)).filter(pos -> !matchMember(world.getBlockState(pos))).collect(Collectors.toList());
  }

  @Nullable
  public abstract EnumMemberVariant getMemberVariantFromItemStack(ItemStack stack);

  /**
   * @return Max box where to search for the base.
   */
  public abstract JSGAxisAlignedBB getBaseSearchBox();

  /**
   * @param state State of the block for the check.
   *
   * @return True if the {@link IBlockState} represents the Base block, false otherwise.
   */
  public abstract boolean matchBase(IBlockState state);

  /**
   * @param state State of the block for the check.
   *
   * @return True if the {@link IBlockState} represents the Member block, false otherwise.
   */
  public abstract boolean matchMember(IBlockState state);

  /**
   * @return Member block.
   */
  public abstract StargateAbstractMemberBlock getMemberBlock();

  /**
   * Method searches for a {@link StargateMilkyWayBaseBlock}
   * and returns it's {@link TileEntity}.
   *
   * @param blockAccess Usually {@link World}.
   * @param memberPos   Starting position.
   * @param facing      Facing of the member blocks.
   *
   * @return {@link StargateMilkyWayBaseTile} if found, {@code null} otherwise.
   */
  @Nullable
  public StargateAbstractBaseTile findBaseTile(IBlockAccess blockAccess, BlockPos memberPos, EnumFacing facing) {
    JSGAxisAlignedBB globalBox = getBaseSearchBox().rotate(facing).offset(memberPos);

    for (MutableBlockPos pos : BlockPos.getAllInBoxMutable(globalBox.getMinBlockPos(), globalBox.getMaxBlockPos())) {
      if (matchBase(blockAccess.getBlockState(pos))) {
        return (StargateAbstractBaseTile) blockAccess.getTileEntity(pos.toImmutable());
      }
    }

    return null;
  }

  /**
   * Check the given {@link BlockPos} for the {@link StargateMilkyWayMemberBlock},
   * it's correct variant and facing.
   *
   * @param blockAccess Usually {@link World}.
   * @param pos         {@link BlockPos} to be checked.
   * @param facing      Expected {@link EnumFacing}.
   * @param variant     Expected {@link EnumMemberVariant}.
   *
   * @return {@code true} if the block matches given parameters, {@code false} otherwise.
   */
  protected abstract boolean checkMemberBlock(IBlockAccess blockAccess, BlockPos pos, EnumFacing facing, EnumMemberVariant variant);

  /**
   * Called on block placement. Checks the found base block
   * for other Stargate blocks and returns the result.
   *
   * @param blockAccess Usually {@link World}.
   * @param basePos     Found {@link StargateMilkyWayBaseBlock}.
   * @param baseFacing  Current base facing.
   *
   * @return {@code true} if the structure matches, {@code false} otherwise.
   */
  public boolean checkBlocks(IBlockAccess blockAccess, BlockPos basePos, EnumFacing baseFacing) {
    if (JSGConfig.debugConfig.checkGateMerge) {
      for (BlockPos pos : getRingBlocks()) {
        if (!checkMemberBlock(blockAccess, pos.rotate(FacingToRotation.get(baseFacing)).add(basePos), baseFacing, EnumMemberVariant.RING))
          return false;
      }

      for (BlockPos pos : getChevronBlocks()) {
        if (!checkMemberBlock(blockAccess, pos.rotate(FacingToRotation.get(baseFacing)).add(basePos), baseFacing, EnumMemberVariant.CHEVRON))
          return false;
      }
    }

    return true;
  }

  /**
   * Updates merge status of the given block.
   *
   * @param world          {@link World} instance.
   * @param checkPos       Position of the currently checked {@link StargateMilkyWayMemberBlock}.
   * @param basePos        Position of {@link StargateMilkyWayBaseBlock} the tiles should be linked to.
   * @param baseFacing     Facing of the base block
   * @param shouldBeMerged {@code true} if the structure is merging, false otherwise.
   */
  protected abstract void updateMemberMergeStatus(World world, BlockPos checkPos, BlockPos basePos, EnumFacing baseFacing, boolean shouldBeMerged);

  /**
   * Updates merge status of the Stargate.
   *
   * @param world          {@link World} instance.
   * @param basePos        Position of {@link StargateMilkyWayBaseBlock} the tiles should be linked to.
   * @param baseFacing     Facing of {@link StargateMilkyWayBaseBlock}.
   * @param shouldBeMerged {@code true} if the structure is merging, false otherwise.
   */
  public void updateMembersMergeStatus(World world, BlockPos basePos, EnumFacing baseFacing, boolean shouldBeMerged) {
    for (BlockPos pos : getRingBlocks())
      updateMemberMergeStatus(world, pos, basePos, baseFacing, shouldBeMerged);

    for (BlockPos pos : getChevronBlocks())
      updateMemberMergeStatus(world, pos, basePos, baseFacing, shouldBeMerged);
  }

  public void updateMembersBasePos(IBlockAccess blockAccess, BlockPos basePos, EnumFacing baseFacing) {
  }
}