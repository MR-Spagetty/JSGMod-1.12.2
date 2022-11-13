package tauri.dev.jsg.renderer.stargate;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import tauri.dev.jsg.JSG;
import tauri.dev.jsg.config.JSGConfig;
import tauri.dev.jsg.loader.ElementEnum;
import tauri.dev.jsg.loader.model.ModelLoader;
import tauri.dev.jsg.loader.texture.TextureLoader;
import tauri.dev.jsg.stargate.EnumIrisType;
import tauri.dev.jsg.stargate.network.SymbolUniverseEnum;
import tauri.dev.jsg.util.JSGTextureLightningHelper;

public class StargateUniverseRenderer extends StargateClassicRenderer<StargateUniverseRendererState> {

    private static final float GATE_DIAMETER = 8.67415f;

    @Override
    protected void applyTransformations(StargateUniverseRendererState rendererState) {
        float scale = 0.90f;
        GlStateManager.scale(scale, scale, scale);

        GlStateManager.translate(0.5, GATE_DIAMETER / 2 + 0.20, 0.55);
        GlStateManager.rotate(90, 0, 0, 1);
    }

    @Override
    protected void renderGate(StargateUniverseRendererState rendererState, double partialTicks) {
        setGateHeatColor(rendererState);
        GlStateManager.rotate(-rendererState.horizontalRotation, 0, 1, 0);
        float angularRotation = rendererState.spinHelper.getCurrentSymbol().getAngle();

        if (rendererState.spinHelper.getIsSpinning())
            angularRotation += rendererState.spinHelper.apply(getWorld().getTotalWorldTime() + partialTicks);

        GlStateManager.rotate(rendererState.horizontalRotation - 90, 1, 0, 0);
        GlStateManager.rotate((float) angularRotation + 0.6f, 0, 1, 0);

        GlStateManager.disableLighting();
        ElementEnum.UNIVERSE_CHEVRON.bindTexture(rendererState.getBiomeOverlay());

        for (SymbolUniverseEnum symbol : SymbolUniverseEnum.values()) {
            if (symbol.modelResource != null) {
                float color = rendererState.getSymbolColor(symbol) + 0.25f;
                GlStateManager.pushMatrix();
                JSGTextureLightningHelper.lightUpTexture(rendererState.getSymbolColor(symbol) / 0.6f);

                GlStateManager.color(color, color, color);
                ModelLoader.getModel(symbol.modelResource).render();
                JSGTextureLightningHelper.resetLight(getWorld(), rendererState.pos);
                GlStateManager.popMatrix();
            }
        }
        GlStateManager.enableLighting();

        renderChevrons(rendererState, partialTicks);

        ElementEnum.UNIVERSE_GATE.bindTextureAndRender(rendererState.getBiomeOverlay());

        rendererState.iterate(getWorld(), partialTicks);
    }

    @Override
    protected ResourceLocation getEventHorizonTextureResource(StargateAbstractRendererState rendererState, boolean kawoosh) {
        String texture = (kawoosh ? EV_HORIZON_DESATURATED_KAWOOSH_TEXTURE_ANIMATED : EV_HORIZON_DESATURATED_TEXTURE_ANIMATED);
        if (JSGConfig.horizonConfig.disableAnimatedEventHorizon || !isEhAnimatedLoaded())
            texture = EV_HORIZON_DESATURATED_TEXTURE;

        return new ResourceLocation(JSG.MOD_ID, texture);
    }

    @Override
    protected void renderKawoosh(StargateAbstractRendererState rendererState, double partialTicks) {

        GlStateManager.translate(0, -0.05f, 0);
        GlStateManager.rotate(90, 1, 0, 0);
        GlStateManager.scale(0.9, 0.9, 0.9);

        super.renderKawoosh(rendererState, partialTicks);

    }

    private static final float DARKNESS = 0.6f;

    @Override
    public void renderIris(double partialTicks, World world, StargateUniverseRendererState rendererState, boolean backOnly) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(0, -0.05f, 0);
        GlStateManager.rotate(90, 1, 0, 0);
        GlStateManager.scale(0.887f, 0.887f, 0.887f);
        if (rendererState.irisType == EnumIrisType.SHIELD) GlStateManager.color(1, 1, 1);
        else GlStateManager.color(DARKNESS, DARKNESS, DARKNESS);
        super.renderIris(partialTicks, world, rendererState, backOnly);
        GlStateManager.popMatrix();
    }

    // ----------------------------------------------------------------------------------------
    // Chevrons

    @Override
    protected void renderChevron(StargateUniverseRendererState rendererState, double partialTicks, ChevronEnum chevron, boolean onlyLight) {
        GlStateManager.pushMatrix();
        setGateHeatColor(rendererState);

        GlStateManager.rotate(-chevron.rotation, 0, 1, 0);

        TextureLoader.getTexture(rendererState.chevronTextureList.get(rendererState.getBiomeOverlay(), chevron, onlyLight)).bindTexture();
        ElementEnum.UNIVERSE_CHEVRON.render();

        GlStateManager.popMatrix();
    }
}
