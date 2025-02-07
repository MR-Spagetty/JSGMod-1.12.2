package tauri.dev.jsg.beamer;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntityBeaconRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import tauri.dev.jsg.block.JSGBlocks;
import tauri.dev.jsg.config.JSGConfig;
import tauri.dev.jsg.tileentity.BeamerTile;
import tauri.dev.jsg.util.FacingToRotation;
import tauri.dev.jsg.util.FluidColors;
import tauri.dev.jsg.util.JSGAxisAlignedBB;
import tauri.dev.jsg.util.JSGTextureLightningHelper;
import tauri.dev.jsg.util.main.JSGDamageSources;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * BeamerBeam is only half of the whole beam - only one part
 * Needed 2 beams to make connection through a gate
 *
 * @author MineDragonCZ
 */
public class BeamerBeam {
    public float angleX;
    public float angleY;

    public double beamLength;
    public float beamRadius;
    public float beamMaxRadius;

    public BeamerBeam(float angleX, float angleY, double beamLength, float beamRadius) {
        this(angleX, angleY, beamLength, beamRadius, beamRadius);
    }
    public BeamerBeam(float angleX, float angleY, double beamLength, float beamRadius, float beamMaxRadius) {
        this.angleX = angleX;
        this.angleY = angleY;
        this.beamLength = beamLength;
        this.beamRadius = beamRadius;
        this.beamMaxRadius = beamMaxRadius;
    }

    @Nonnull
    public static BeamerBeam getBeam(int offsetZFromTarget, int offsetZTargetFromGate, int offsetXFromTarget, int offsetYFromTarget, float beamRadius, float beamMaxRadius, @Nonnull EnumFacing facing) {

        double t1 = ((double) offsetXFromTarget) / ((double) offsetZFromTarget);
        double angY = Math.toDegrees(Math.atan(t1));

        double beamerLengthTemp = (Math.sqrt(Math.pow(offsetZFromTarget, 2) + Math.pow(offsetXFromTarget, 2)));

        double t2 = ((double) offsetYFromTarget) / (beamerLengthTemp);
        double angX = Math.toDegrees(Math.atan(t2));

        double beamerLength = ((Math.sqrt(Math.pow(beamerLengthTemp, 2) + Math.pow(offsetYFromTarget, 2))) - offsetZTargetFromGate);

        if (beamerLength < 0) beamerLength = -beamerLength;

        beamerLength += 0.2D;

        return new BeamerBeam((float) angX * -1, (float) angY * ((facing == EnumFacing.SOUTH || facing == EnumFacing.WEST) ? -1 : 1), beamerLength, beamRadius, beamMaxRadius);
    }

    public static boolean isSomethingInBeam(BeamerTile beamer, boolean destroyBlocks, boolean hitEntities) {
        if (!JSGConfig.beamerConfig.damageEntities) hitEntities = false;
        if (!JSGConfig.beamerConfig.destroyBlocks) destroyBlocks = false;

        boolean debug = false;

        BlockPos pos = beamer.getPos();
        World world = beamer.getWorld();
        EnumFacing facing = beamer.getFacing();
        int beamXOffset = beamer.beamOffsetFromTargetX;
        int beamYOffset = beamer.beamOffsetFromTargetY;
        int beamZOffset = beamer.beamOffsetFromTargetZ;
        int targetOffset = beamer.beamOffsetFromGateTarget;

        int currentOffsetFromGate = beamZOffset - targetOffset;
        if (currentOffsetFromGate < 0)
            currentOffsetFromGate *= -1;

        for (int off = 1; off < currentOffsetFromGate; off++) {
            BeamerBeam beam = getBeam(beamZOffset, targetOffset, beamXOffset, beamYOffset, -1, -1, beamer.getFacing());
            double lengthXZ = (off / Math.cos(Math.toRadians(beam.angleY)));

            final double offX = (Math.tan(Math.toRadians(beam.angleY)) * off);
            final double offY = (Math.tan(Math.toRadians(beam.angleX)) * lengthXZ);

            for (int i = 0; i < 9; i++) {

                double x;
                double y;

                if (i < 3) y = Math.ceil(offY);
                else if (i < 6) y = Math.round(offY);
                else y = Math.floor(offY);

                if (i % 3 == 0) x = Math.floor(offX);
                else if ((i - 1) % 3 == 0) x = Math.round(offX);
                else x = Math.ceil(offX);

                BlockPos stepPos = new BlockPos(x, y, off).rotate(FacingToRotation.get(facing));

                stepPos = stepPos.add(pos);

                if (stepPos.equals(pos)) continue;

                Block targetBlock = world.getBlockState(stepPos).getBlock();
                IBlockState targetBlockState = world.getBlockState(stepPos);
                if (!debug) {
                    if (destroyBlocks && !JSGBlocks.isInBlocksArray(targetBlock, JSGBlocks.BEAMER_BREAK_BLACKLIST) && targetBlockState.getBlockHardness(world, stepPos) >= 0.0f)
                        world.setBlockToAir(stepPos);
                    else if (!destroyBlocks) {
                        if ((!targetBlock.isAir(targetBlockState, world, pos) && !targetBlock.isReplaceable(world, pos) && targetBlockState.isOpaqueCube()) || targetBlock == JSGBlocks.IRIS_BLOCK)
                            return true;
                    }
                } else {
                    world.setBlockState(stepPos, Blocks.STONE.getDefaultState());
                }
                if (hitEntities && world.getTotalWorldTime() % 20 == 0) {
                    List<Entity> entities = world.getEntitiesWithinAABB(Entity.class, new JSGAxisAlignedBB(stepPos.add(-1, -1, -1), stepPos.add(1, 1, 1)));
                    for (Entity entity : entities)
                        entity.attackEntityFrom(JSGDamageSources.DAMAGE_BEAMER, ((beamer.getMode() == BeamerModeEnum.LASER) ? 5 : 2));
                }
            }

        }
        return false;
    }


    // -------------------------------------------
    // RENDERING
    public void render(float partialTicks, long tick, BeamerRoleEnum teRole, float[] colors, boolean transferringFluid, Fluid lastFluidTransferred) {
        if (transferringFluid && tauri.dev.jsg.config.JSGConfig.beamerConfig.enableFluidBeamColorization) {
            if (lastFluidTransferred != null) {
                FluidColors.FloatColors fluidColors = FluidColors.getAverageColor(lastFluidTransferred);
                if (fluidColors != null) {
                    colors = fluidColors.colors;
                }
            }
        }
        GlStateManager.rotate(this.angleY, 0, 1, 0);
        GlStateManager.rotate(-90 + this.angleX, 1, 0, 0);

        GlStateManager.alphaFunc(516, 0.1F);
        JSGTextureLightningHelper.lightUpTexture(1f);
        Minecraft.getMinecraft().getTextureManager().bindTexture(TileEntityBeaconRenderer.TEXTURE_BEACON_BEAM);
        float lengthAnimCoefficient = (this.beamRadius/this.beamMaxRadius);
        renderBeamSegment(partialTicks, (teRole == BeamerRoleEnum.TRANSMIT ? 1 : -1), tick, (this.beamLength * lengthAnimCoefficient), colors, this.beamRadius, this.beamRadius + 0.05f);
    }

    /**
     * This method is copy of Minecraft Beam Render method
     * <p>
     * - edited some ints to doubles
     */
    private static void renderBeamSegment(double partialTicks, double textureScale, double totalWorldTime, double height, float[] colors, double beamRadius, double glowRadius) {
        GlStateManager.pushMatrix();
        JSGTextureLightningHelper.lightUpTexture(1f);
        double i = (double) 0 + height;
        GlStateManager.glTexParameteri(3553, 10242, 10497);
        GlStateManager.glTexParameteri(3553, 10243, 10497);
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.disableBlend();
        GlStateManager.depthMask(true);
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        double d0 = totalWorldTime + partialTicks;
        double d1 = height < 0 ? d0 : -d0;
        double d2 = MathHelper.frac(d1 * 0.2D - (double) MathHelper.floor(d1 * 0.1D));
        float f = colors[0];
        float f1 = colors[1];
        float f2 = colors[2];
        double d3 = d0 * 0.025D * -1.5D;
        double d4 = 0.5D + Math.cos(d3 + 2.356194490192345D) * beamRadius;
        double d5 = 0.5D + Math.sin(d3 + 2.356194490192345D) * beamRadius;
        double d6 = 0.5D + Math.cos(d3 + (Math.PI / 4D)) * beamRadius;
        double d7 = 0.5D + Math.sin(d3 + (Math.PI / 4D)) * beamRadius;
        double d8 = 0.5D + Math.cos(d3 + 3.9269908169872414D) * beamRadius;
        double d9 = 0.5D + Math.sin(d3 + 3.9269908169872414D) * beamRadius;
        double d10 = 0.5D + Math.cos(d3 + 5.497787143782138D) * beamRadius;
        double d11 = 0.5D + Math.sin(d3 + 5.497787143782138D) * beamRadius;
        double d13 = -1.0D + d2;
        double d14 = -1.0D + d2;
        double d15 = height * textureScale * (0.5D / beamRadius) + d14;
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        bufferbuilder.pos(-0.5 + d4, -0.3 + i, -0.5 + d5).tex(1.0D, d15).color(f, f1, f2, 1.0F).endVertex();
        bufferbuilder.pos(-0.5 + d4, -0.3 + (double) 0, -0.5 + d5).tex(1.0D, d14).color(f, f1, f2, 1.0F).endVertex();
        bufferbuilder.pos(-0.5 + d6, -0.3 + (double) 0, -0.5 + d7).tex(0.0D, d14).color(f, f1, f2, 1.0F).endVertex();
        bufferbuilder.pos(-0.5 + d6, -0.3 + i, -0.5 + d7).tex(0.0D, d15).color(f, f1, f2, 1.0F).endVertex();
        bufferbuilder.pos(-0.5 + d10, -0.3 + i, -0.5 + d11).tex(1.0D, d15).color(f, f1, f2, 1.0F).endVertex();
        bufferbuilder.pos(-0.5 + d10, -0.3 + (double) 0, -0.5 + d11).tex(1.0D, d14).color(f, f1, f2, 1.0F).endVertex();
        bufferbuilder.pos(-0.5 + d8, -0.3 + (double) 0, -0.5 + d9).tex(0.0D, d14).color(f, f1, f2, 1.0F).endVertex();
        bufferbuilder.pos(-0.5 + d8, -0.3 + i, -0.5 + d9).tex(0.0D, d15).color(f, f1, f2, 1.0F).endVertex();
        bufferbuilder.pos(-0.5 + d6, -0.3 + i, -0.5 + d7).tex(1.0D, d15).color(f, f1, f2, 1.0F).endVertex();
        bufferbuilder.pos(-0.5 + d6, -0.3 + (double) 0, -0.5 + d7).tex(1.0D, d14).color(f, f1, f2, 1.0F).endVertex();
        bufferbuilder.pos(-0.5 + d10, -0.3 + (double) 0, -0.5 + d11).tex(0.0D, d14).color(f, f1, f2, 1.0F).endVertex();
        bufferbuilder.pos(-0.5 + d10, -0.3 + i, -0.5 + d11).tex(0.0D, d15).color(f, f1, f2, 1.0F).endVertex();
        bufferbuilder.pos(-0.5 + d8, -0.3 + i, -0.5 + d9).tex(1.0D, d15).color(f, f1, f2, 1.0F).endVertex();
        bufferbuilder.pos(-0.5 + d8, -0.3 + (double) 0, -0.5 + d9).tex(1.0D, d14).color(f, f1, f2, 1.0F).endVertex();
        bufferbuilder.pos(-0.5 + d4, -0.3 + (double) 0, -0.5 + d5).tex(0.0D, d14).color(f, f1, f2, 1.0F).endVertex();
        bufferbuilder.pos(-0.5 + d4, -0.3 + i, -0.5 + d5).tex(0.0D, d15).color(f, f1, f2, 1.0F).endVertex();
        tessellator.draw();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.depthMask(false);
        d3 = 0.5D - glowRadius;
        d4 = 0.5D - glowRadius;
        d5 = 0.5D + glowRadius;
        d6 = 0.5D - glowRadius;
        d7 = 0.5D - glowRadius;
        d8 = 0.5D + glowRadius;
        d9 = 0.5D + glowRadius;
        d10 = 0.5D + glowRadius;
        d14 = height * textureScale + d13;
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        bufferbuilder.pos(-0.5 + d3, -0.3 + i, -0.5 + d4).tex(1.0D, d14).color(f, f1, f2, 0.25F).endVertex();
        bufferbuilder.pos(-0.5 + d3, -0.3 + (double) 0, -0.5 + d4).tex(1.0D, d13).color(f, f1, f2, 0.25F).endVertex();
        bufferbuilder.pos(-0.5 + d5, -0.3 + (double) 0, -0.5 + d6).tex(0.0D, d13).color(f, f1, f2, 0.25F).endVertex();
        bufferbuilder.pos(-0.5 + d5, -0.3 + i, -0.5 + d6).tex(0.0D, d14).color(f, f1, f2, 0.25F).endVertex();
        bufferbuilder.pos(-0.5 + d9, -0.3 + i, -0.5 + d10).tex(1.0D, d14).color(f, f1, f2, 0.25F).endVertex();
        bufferbuilder.pos(-0.5 + d9, -0.3 + (double) 0, -0.5 + d10).tex(1.0D, d13).color(f, f1, f2, 0.25F).endVertex();
        bufferbuilder.pos(-0.5 + d7, -0.3 + (double) 0, -0.5 + d8).tex(0.0D, d13).color(f, f1, f2, 0.25F).endVertex();
        bufferbuilder.pos(-0.5 + d7, -0.3 + i, -0.5 + d8).tex(0.0D, d14).color(f, f1, f2, 0.25F).endVertex();
        bufferbuilder.pos(-0.5 + d5, -0.3 + i, -0.5 + d6).tex(1.0D, d14).color(f, f1, f2, 0.25F).endVertex();
        bufferbuilder.pos(-0.5 + d5, -0.3 + (double) 0, -0.5 + d6).tex(1.0D, d13).color(f, f1, f2, 0.25F).endVertex();
        bufferbuilder.pos(-0.5 + d9, -0.3 + (double) 0, -0.5 + d10).tex(0.0D, d13).color(f, f1, f2, 0.25F).endVertex();
        bufferbuilder.pos(-0.5 + d9, -0.3 + i, -0.5 + d10).tex(0.0D, d14).color(f, f1, f2, 0.25F).endVertex();
        bufferbuilder.pos(-0.5 + d7, -0.3 + i, -0.5 + d8).tex(1.0D, d14).color(f, f1, f2, 0.25F).endVertex();
        bufferbuilder.pos(-0.5 + d7, -0.3 + (double) 0, -0.5 + d8).tex(1.0D, d13).color(f, f1, f2, 0.25F).endVertex();
        bufferbuilder.pos(-0.5 + d3, -0.3 + (double) 0, -0.5 + d4).tex(0.0D, d13).color(f, f1, f2, 0.25F).endVertex();
        bufferbuilder.pos(-0.5 + d3, -0.3 + i, -0.5 + d4).tex(0.0D, d14).color(f, f1, f2, 0.25F).endVertex();
        tessellator.draw();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.depthMask(true);
        GlStateManager.popMatrix();
    }
}
