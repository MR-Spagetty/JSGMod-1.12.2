package tauri.dev.jsg.config;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.Config.*;
import tauri.dev.jsg.JSG;
import tauri.dev.jsg.block.JSGBlocks;
import tauri.dev.jsg.config.parsers.BiomeParser;
import tauri.dev.jsg.config.parsers.BlockMetaParser;
import tauri.dev.jsg.config.parsers.ItemMetaParser;
import tauri.dev.jsg.config.stargate.StargateSizeEnum;
import tauri.dev.jsg.config.stargate.StargateTimeLimitModeEnum;
import tauri.dev.jsg.renderer.biomes.BiomeOverlayEnum;
import tauri.dev.jsg.util.ItemMetaPair;
import tauri.dev.jsg.util.JSGAxisAlignedBB;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Config(modid = JSG.MOD_ID, name = "jsg/jsgConfig_" + JSG.CONFIG_GENERAL_VERSION)
public class JSGConfig {

    @Name("Stargate size")
    @RequiresWorldRestart
    public static StargateSizeEnum stargateSize = StargateSizeEnum.MEDIUM;

    @Name("Check for updates")
    public static boolean enableAutoUpdater = true;

    @Name("Stargate config options")
    public static StargateConfig stargateConfig = new StargateConfig();

    @Name("Event horizon config options")
    public static HorizonConfig horizonConfig = new HorizonConfig();

    @Name("Dialing/Incoming options")
    public static DialingConfig dialingConfig = new DialingConfig();

    @Name("Iris/shield config options")
    public static IrisConfig irisConfig = new IrisConfig();

    @Name("DHD config options")
    public static DHDConfig dhdConfig = new DHDConfig();

    @Name("Transport rings options")
    public static RingsConfig ringsConfig = new RingsConfig();

    @Name("Power draw options")
    public static PowerConfig powerConfig = new PowerConfig();

    @Name("Debug options")
    public static DebugConfig debugConfig = new DebugConfig();

    @Name("Mysterious Page options")
    public static MysteriousConfig mysteriousConfig = new MysteriousConfig();

    @Name("Notebook Pages options")
    public static NoteBookOptions notebookOptions = new NoteBookOptions();

    @Name("AutoClose options")
    public static AutoCloseConfig autoCloseConfig = new AutoCloseConfig();

    @Name("Open time limit config")
    public static OpenLimitConfig openLimitConfig = new OpenLimitConfig();

    @Name("Beamer options")
    public static BeamerConfig beamerConfig = new BeamerConfig();

    @Name("Audio/Video")
    public static AudioVideoConfig avConfig = new AudioVideoConfig();

    @Name("Point Of Origin options")
    public static PointOfOriginConfig originsConfig = new PointOfOriginConfig();

    @Name("Ore Generator config")
    public static WorldOreGenerator oreGeneratorConfig = new WorldOreGenerator();

    @Name("Stargate Generator config")
    public static WorldStargateGenerator stargateGeneratorConfig = new WorldStargateGenerator();

    @Name("MainMenu config")
    public static MainMenuConfig mainMenuConfig = new MainMenuConfig();

    @Name("Integrations config")
    public static IntegrationsConfig integrationsConfig = new IntegrationsConfig();

    @Name("Random incoming config")
    public static RandomIncomingConfig randomIncoming = new RandomIncomingConfig();

    @Name("Advancements config")
    public static AdvancementsConfig advancementsConfig = new AdvancementsConfig();

    @Name("Countdown config")
    public static CountDownConfig countdownConfig = new CountDownConfig();

    @Name("Development config")
    public static DevConfig devConfig = new DevConfig();

    public static class StargateConfig {
        @Name("Enable burried state for gates")
        @Comment({
                "THIS OPTION CAN BE OVERRIDE BY SETTING IT IN STARGATE GUI"
        })
        public boolean enableBurriedState = true;

        @Name("Orlin's gate max open count")
        @RangeInt(min = 0)
        public int stargateOrlinMaxOpenCount = 2;

        @Name("Universe dialer max horizontal reach radius")
        @RangeInt(min = 0, max = 64)
        public int universeDialerReach = 10;

        @Name("Universe dialer nearby radius")
        public int universeGateNearbyReach = 1024;

        @Name("Temperature threshold for frosty overlay")
        @Comment({
                "Below this biome temperature the gate will receive frosty texture.",
                "Set to negative value to disable."
        })
        public float frostyTemperatureThreshold = 0.1f;

        // ---------------------------------------------------------------------------------------
        // Kawoosh blocks

        @Name("Kawoosh invincible blocks")
        @Comment({
                "Format: \"modid:blockid[:meta/*]\", for example: ",
                "\"minecraft:wool:7\"",
                "\"minecraft:stone\"",
                "\"minecraft:concrete:*\""
        })
        public String[] kawooshInvincibleBlocks = {
                "minecraft:snow_layer:*",
                "minecraft:rail:*",
                "minecraft:golden_rail:*",
                "minecraft:detector_rail:*",
                "minecraft:activator_rail:*",
                "minecraft:carpet:*",
                "minecraft:stone_pressure_plate:*",
                "minecraft:wooden_pressure_plate:*",
                "minecraft:light_weighted_pressure_plate:*",
                "minecraft:heavy_weighted_pressure_plate:*"
        };

        private Map<IBlockState, Boolean> cachedInvincibleBlocks = null;

        public boolean canKawooshDestroyBlock(IBlockState state) {
            if (state.getBlock() == JSGBlocks.IRIS_BLOCK) return false;
            if (state.getBlock() == JSGBlocks.INVISIBLE_BLOCK) return false;

            if (cachedInvincibleBlocks == null) {
                cachedInvincibleBlocks = BlockMetaParser.parseConfig(kawooshInvincibleBlocks);
            }
            if (cachedInvincibleBlocks.get(state.getBlock().getDefaultState()) != null && cachedInvincibleBlocks.get(state.getBlock().getDefaultState())) {
                return false;
            }
            return cachedInvincibleBlocks.get(state) == null;
        }


        // ---------------------------------------------------------------------------------------
        // Jungle biomes
        @Name("Biome overlay biome matches")
        @Comment({
                "This check comes last (after block is directly under sky (except Nether) and temperature is high enough).",
                "You can disable the temperature check by setting it to a negative value.",
                "Format: \"modid:biomename\", for example: ",
                "\"minecraft:dark_forest\"",
                "\"minecraft:forest\""
        })
        public Map<String, String[]> biomeMatches = new HashMap<String, String[]>() {
            {
                put(BiomeOverlayEnum.NORMAL.toString(), new String[]{});
                put(BiomeOverlayEnum.FROST.toString(), new String[]{});
                put(BiomeOverlayEnum.MOSSY.toString(), new String[]{"minecraft:jungle", "minecraft:jungle_hills", "minecraft:jungle_edge", "minecraft:mutated_jungle", "minecraft:mutated_jungle_edge"});
                put(BiomeOverlayEnum.AGED.toString(), new String[]{});
                put(BiomeOverlayEnum.SOOTY.toString(), new String[]{"minecraft:hell"});
            }
        };

        private Map<Biome, BiomeOverlayEnum> cachedBiomeMatchesReverse = null;

        public Map<Biome, BiomeOverlayEnum> getBiomeOverrideBiomes() {
            if (cachedBiomeMatchesReverse == null) {
                cachedBiomeMatchesReverse = new HashMap<>();

                for (Map.Entry<String, String[]> entry : biomeMatches.entrySet()) {
                    List<Biome> parsedList = BiomeParser.parseConfig(entry.getValue());
                    BiomeOverlayEnum biomeOverlay = BiomeOverlayEnum.fromString(entry.getKey());

                    for (Biome biome : parsedList) {
                        cachedBiomeMatchesReverse.put(biome, biomeOverlay);
                    }
                }
            }

            return cachedBiomeMatchesReverse;
        }


        // ---------------------------------------------------------------------------------------
        // Biome overlay override blocks

        @Name("Biome overlay override blocks")
        @Comment({
                "Format: \"modid:blockid[:meta]\", for example: ",
                "\"minecraft:wool:7\"",
                "\"minecraft:stone\""
        })
        public Map<String, String[]> biomeOverrideBlocks = new HashMap<String, String[]>() {
            {
                put(BiomeOverlayEnum.NORMAL.toString(), new String[]{"minecraft:stone"});
                put(BiomeOverlayEnum.FROST.toString(), new String[]{"minecraft:ice"});
                put(BiomeOverlayEnum.MOSSY.toString(), new String[]{"minecraft:vine"});
                put(BiomeOverlayEnum.AGED.toString(), new String[]{"minecraft:cobblestone"});
                put(BiomeOverlayEnum.SOOTY.toString(), new String[]{"minecraft:coal_block"});
            }
        };

        private Map<BiomeOverlayEnum, List<ItemMetaPair>> cachedBiomeOverrideBlocks = null;
        private Map<ItemMetaPair, BiomeOverlayEnum> cachedBiomeOverrideBlocksReverse = null;

        private void genBiomeOverrideCache() {
            cachedBiomeOverrideBlocks = new HashMap<>();
            cachedBiomeOverrideBlocksReverse = new HashMap<>();

            for (Map.Entry<String, String[]> entry : biomeOverrideBlocks.entrySet()) {
                List<ItemMetaPair> parsedList = ItemMetaParser.parseConfig(entry.getValue());
                BiomeOverlayEnum biomeOverlay = BiomeOverlayEnum.fromString(entry.getKey());

                cachedBiomeOverrideBlocks.put(biomeOverlay, parsedList);

                for (ItemMetaPair stack : parsedList) {
                    cachedBiomeOverrideBlocksReverse.put(stack, biomeOverlay);
                }
            }
        }

        public Map<BiomeOverlayEnum, List<ItemMetaPair>> getBiomeOverrideBlocks() {
            if (cachedBiomeOverrideBlocks == null) {
                genBiomeOverrideCache();
            }

            return cachedBiomeOverrideBlocks;
        }

        public Map<ItemMetaPair, BiomeOverlayEnum> getBiomeOverrideItemMetaPairs() {
            if (cachedBiomeOverrideBlocksReverse == null) {
                genBiomeOverrideCache();
            }

            return cachedBiomeOverrideBlocksReverse;
        }

        @Name("Enable gate overheat with explosion")
        @Comment({
                "Should gate explode when its overheated?",
                "This method is not implemented yet!"
        })
        public boolean enableGateOverHeatExplosion = false;

        @RequiresMcRestart
        @Name("Max stargate heat")
        public double gateMaxHeat = 83400;

        @Name("Chance of lighting strike that charge a gate")
        public float lightingBoldChance = 0.0005f;

    }

    public static class HorizonConfig {
        @Name("Disable animated Event Horizon")
        @RequiresMcRestart
        @Comment({
                "Changing this option will require you to reload resources manually.",
                "Just restart your game"
        })
        public boolean disableAnimatedEventHorizon = false;

        @Name("Enable wrong side killing")
        public boolean wrongSideKilling = true;

        @Name("Unstable Event Horizon chance of death")
        public float ehDeathChance = 0.07f;

        @Name("Disable new kawoosh model (from 4.11.0.0)")
        @RequiresMcRestart
        public boolean disableNewKawoosh = false;
    }

    public static class DialingConfig {

        @Name("Allow incoming animations")
        @Comment({
                "If the incoming animations of gates generate issues, set it to false",
                "THIS OPTION CAN BE OVERRIDE BY SETTING IT IN STARGATE GUI"
        })
        public boolean allowIncomingAnimations = true;

        @Name("Connect to dialing gate")
        @Comment({
                "If target gate is dialing and this option is set to true,",
                "the target gate stop dialing and open incoming wormhole.",
                "If this is set to false and the dialed gate dialing address,",
                "the connection will not established.",
                "If it cause issues, set it to false.",
        })
        public boolean allowConnectToDialing = true;

        @Name("Use 8 chevrons between MW and PG gates")
        @Comment({
                "Change this to true, if you want to use 8 chevrons between pegasus and milkyway gates"
        })
        public boolean pegAndMilkUseEightChevrons = true;

        @Name("Need only 7 symbols between Uni gates")
        @Comment({
                "If u want to dial UNI-UNI only with seven symbols (interdimensional for example), set this to true"
        })
        public boolean useStrictSevenSymbolsUniGate = false;

        @Name("Faster MilkyWay and Universe gates computer dial")
        @Comment({
                "Speed up dialing with computer on MW and UNI gates"
        })
        public boolean fasterMWGateDial = false;

        @Name("Enable fast dialing of gates")
        @Comment({
                "THIS OPTION CAN BE OVERRIDE BY SETTING IT IN STARGATE GUI"
        })
        public boolean enableFastDialing = false;

        @Name("Enable opening last chevron while dialing with dhd")
        @Comment({
                "Enable opening last chevron while dialing milkyway gate with dhd",
                "THIS OPTION CAN BE OVERRIDE BY SETTING IT IN STARGATE GUI"
        })
        public boolean dhdLastOpen = true;

    }

    public static class IrisConfig {
        @Name("Iris kills at destination")
        @Comment("If set to 'false' player get killed by iris on entering event horizon")
        public boolean killAtDestination = true;

        @Name("Titanium iris durability")
        @Comment({
                "Durability of Titanium iris",
                "set it to 0, if u want to make it unbreakable"
        })
        @RangeInt(min = 0)
        public int titaniumIrisDurability = 500;

        @Name("Trinium iris durability")
        @Comment({
                "Durability of Trinium iris",
                "set it to 0, if u want to make it unbreakable"
        })
        @RangeInt(min = 0)
        public int triniumIrisDurability = 1000;

        @Name("Shield power draw")
        @Comment({
                "Energy/tick used for make shield closed"
        })
        @RangeInt(min = 0)
        public int shieldPowerDraw = 500;

        @Name("Allow creative bypass")
        @Comment({
                "Set it to true, if u want to bypass",
                "shield/iris damage by creative gamemode"
        })
        public boolean allowCreative = false;

        @Name("Maximum iris code length")
        @RangeInt(min = 0, max = 32)
        public int irisCodeLength = 9;

        @Name("Can iris destroy blocks")
        public boolean irisDestroysBlocks = false;

        @Name("Unbreaking chance per level")
        @Comment({"0 - disables unbreaking on iris", "100 - unbreaking makes iris unbreakable"})
        @RangeInt(min = 0, max = 100)
        public int unbreakingChance = 10;

        @Name("Enable iris overheat collapse")
        @Comment({
                "Should iris break when its overheated?"
        })
        public boolean enableIrisOverHeatCollapse = true;

        @RequiresMcRestart
        @Name("Max titanium iris heat")
        public double irisTitaniumMaxHeat = 1668;

        @RequiresMcRestart
        @Name("Max trinium iris heat")
        public double irisTriniumMaxHeat = 3336;
    }

    public static class PowerConfig {
        @Name("Stargate's internal buffer size")
        @RangeInt(min = 0)
        public int stargateEnergyStorage = 71280000;

        @Name("Stargate's max power throughput")
        @RangeInt(min = 1)
        public int stargateMaxEnergyTransfer = 26360;

        @Name("Stargate wormhole open power draw")
        @RangeInt(min = 0)
        public int openingBlockToEnergyRatio = 4608;

        @Name("Stargate wormhole sustain power draw")
        @RangeInt(min = 0)
        public int keepAliveBlockToEnergyRatioPerTick = 2;

        @Name("Stargate instability threshold")
        @Comment({
                "Seconds of energy left before gate becomes unstable",
        })
        @RangeInt(min = 1)
        public int instabilitySeconds = 20;

        @Name("Transport Rings active power draw")
        @Comment({
                "Energy extracted from rings every tick when they are active (calculated by distance from these rings)"
        })
        public int ringsKeepAliveBlockToEnergyRatioPerTick = 2;

        @Name("Transport Rings teleport power draw")
        @Comment({
                "Energy extracted from rings when they teleport LIVING entity (not drop)"
        })
        @RangeInt(min = 0)
        public int ringsTeleportPowerDraw = 640;

        @Name("Orlin's gate energy multiplier")
        @RangeDouble(min = 0)
        public double stargateOrlinEnergyMul = 2.0;

        @Name("Universe gate energy multiplier")
        @RangeDouble(min = 0)
        public double stargateUniverseEnergyMul = 1.5;

        @Name("Capacitors supported by Universe gates")
        @Comment({
                "THIS OPTION CAN BE OVERRIDE BY SETTING IT IN STARGATE GUI"
        })
        public int universeCapacitors = 0;

        @Name("ZPM capacity (RF)")
        public int zpmCapacity = 2_000_000_000;

        @Name("ZPMHub's max power throughput")
        @RangeInt(min = 1)
        public int zpmHubMaxEnergyTransfer = 104360;

        @Name("Stargate eight symbols address power mul")
        @Comment({
                "Specifies the multiplier of power needed to keep the gate alive",
                "when 8-symbols address is dialed"
        })
        @RangeDouble(min = 0)
        public float eightSymbolAddressMul = 1.3f;

        @Name("Stargate nine symbols address power mul")
        @Comment({
                "Specifies the multiplier of power needed to keep the gate alive",
                "when 9-symbols address is dialed"
        })
        @RangeDouble(min = 0)
        public float nineSymbolAddressMul = 1.7f;
    }

    public static class RingsConfig {
        @Name("Rings range's radius horizontal")
        @RangeInt(min = 1, max = 256)
        public int rangeFlat = 25;

        @Name("Rings vertical reach")
        @RangeInt(min = 1, max = 256)
        public int rangeVertical = 256;

        @Name("Ignore rings check for blocks to replace")
        public boolean ignoreObstructionCheck = false;
    }

    public static class DHDConfig {
        @Name("DHD range's radius horizontal")
        @RangeInt(min = 1)
        public int rangeFlat = 25;

        @Name("DHD range's radius vertical")
        @RangeInt(min = 1)
        public int rangeVertical = 15;

        @Name("Pegasus DHD do dial animation")
        @Comment({
                "Disable this, to disable animation when dial gate with DHD (pegasus)",
                "THIS OPTION CAN BE OVERRIDE BY SETTING IT IN STARGATE GUI"
        })
        public boolean animatePegDHDDial = true;

        @Name("Enable press sound when dialing with computer")
        @Comment({
                "THIS OPTION CAN BE OVERRIDE BY SETTING IT IN STARGATE GUI"
        })
        public boolean computerDialSound = false;

        @Name("DHD's max fluid capacity")
        @RangeInt(min = 1)
        public int fluidCapacity = 16000;

        @Name("Capacity upgrade multiplier")
        @RangeDouble(min = 1.0, max = 5.0)
        @Comment({
                "When capacity upgrade is placed in the DHD,",
                "then multiply internal capacity by this number"
        })
        public float capacityUpgradeMultiplier = 2f;

        @Name("Energy per 1mB Naquadah")
        @RangeInt(min = 1)
        public int energyPerNaquadah = 10240;

        @Name("Generation multiplier")
        @RangeInt(min = 1)
        @Comment({
                "Energy per 1mB is multiplied by this",
                "Consumed mB/t is equal to this"
        })
        public int powerGenerationMultiplier = 1;

        @Name("Efficiency upgrade multiplier")
        @RangeDouble(min = 1.0, max = 5.0)
        @Comment({
                "Energy per 1mB is multiplied by this",
                "when efficiency upgrade is placed in the DHD"
        })
        public float efficiencyUpgradeMultiplier = 1.4f;

        @Name("Cold fusion reactor activation energy level")
        @RangeDouble(min = 0, max = 1)
        public double activationLevel = 0.4;

        @Name("Cold fusion reactor deactivation energy level")
        @RangeDouble(min = 0, max = 1)
        public double deactivationLevel = 0.98;
    }

    public static class DebugConfig {
        @Name("Check gate merge")
        public boolean checkGateMerge = true;

        @Name("Render bounding boxes")
        public boolean renderBoundingBoxes = false;

        @Name("Render whole kawoosh bounding box")
        public boolean renderWholeKawooshBoundingBox = false;

        @Name("Render invisible blocks")
        public boolean renderInvisibleBlocks = false;

        @Name("Show loading textures in log")
        public boolean logTexturesLoading = false;
    }

    public static class MysteriousConfig {
        @Name("Max overworld XZ-coords generation")
        @RangeInt(min = 1, max = 30000000)
        public int maxOverworldCoords = 30000;

        @Name("Min overworld XZ-coords generation")
        @RangeInt(min = 1, max = 30000000)
        public int minOverworldCoords = 15000;

        @Name("Chance of despawning DHD")
        @RangeDouble(min = 0, max = 1)
        public double despawnDhdChance = 0.05;

        @Name("Mysterious page cooldown")
        @RangeInt(min = 0)
        public int pageCooldown = 40;
    }

    public static class NoteBookOptions {

        @Name("Notebook page Glyph transparency")
        @RangeDouble(min = 0, max = 1)
        public double glyphTransparency = 0.75;

        @Name("Enable hint when dialing on DHDs with notebook page")
        public boolean enablePageHint = true;

        @Name("Notebook Page offset")
        @Comment({
                "Greater values render the Page more to the center of the screen, smaller render it closer to the borders.",
                "0 - for standard 16:9 (default),",
                "0.2 - for 4:3.",
        })
        public float pageNarrowing = 0;
    }

    public static class AutoCloseConfig {
        @Name("Autoclose enabled")
        public boolean autocloseEnabled = true;

        @Name("Seconds to autoclose with no players nearby")
        @RangeInt(min = 1, max = 300)
        public int secondsToAutoclose = 5;
    }

    public static class OpenLimitConfig {
        @Name("Maximum seconds of gate should be open")
        @Comment({
                "(in seconds (2280 = 38 minutes))"
        })
        @RangeInt(min = 5, max = 3000)
        public int maxOpenedSeconds = 240;

        @Name("Gate open time limit mode")
        @Comment({
                "What happens after gate's open time reaches limit?"
        })
        public StargateTimeLimitModeEnum maxOpenedWhat = StargateTimeLimitModeEnum.DRAW_MORE_POWER;

        @Name("Power draw after opened time limit")
        @RangeInt(min = 0, max = 50000)
        public int maxOpenedPowerDrawAfterLimit = 10000;
    }

    public static class BeamerConfig {
        @Name("Fluid buffer capacity")
        @RangeInt(min = 1)
        public int fluidCapacity = 60000;

        @Name("Energy buffer capacity")
        @RangeInt(min = 1)
        public int energyCapacity = 17820000;

        @Name("Energy buffer max transfer")
        @RangeInt(min = 1)
        public int energyTransfer = 26360;

        @Name("Fluid max transfer")
        @RangeInt(min = 1)
        public int fluidTransfer = 100;

        @Name("Item max transfer")
        @RangeInt(min = 1)
        public int itemTransfer = 4;

        @Name("Max gate-beamer distance")
        @RangeInt(min = 3, max = 50)
        public int reach = 10;

        @Name("Should the beam be responsive to fluid color")
        public boolean enableFluidBeamColorization = true;

        @Name("Interval of signals being send to OC about transfers (in ticks)")
        @RangeInt(min = 1)
        public int signalIntervalTicks = 20;

        @Name("Energy/tick needed to keep laser alive")
        @RangeInt(min = 1)
        public int laserEnergy = 16384;

        @Name("Damage entities in a beam")
        public boolean damageEntities = true;

        @Name("Destroy blocks in a beam")
        public boolean destroyBlocks = true;
    }

    public static class AudioVideoConfig {
        @Name("JSG volume")
        @RangeDouble(min = 0, max = 1)
        public float volume = 1;

        @Name("Render not placed blocks of s stargate")
        public boolean renderStargateNotPlaced = true;

        @Name("Render EHs even if they are not rendering")
        public boolean renderEHifTheyNot = true;

        @Name("Render emissive textures")
        @Comment({
                "Render light of some textures.",
                "Disable this if it causes lags."
        })
        public boolean renderEmissive = true;

        @Name("Enable custom sounds category")
        @Comment({"Disable this if it causes crash."})
        public boolean enableCustomSoundCategory = true;
    }

    public static class PointOfOriginConfig {
        @Name("Enable different Point Of Origins for MW gate")
        public boolean enableDiffOrigins = true;

        @RequiresMcRestart
        @Name("Custom added points of origin")
        @Comment({
                "Specifies Point Of Origins that were added by any resource pack.",
                "This options is required to load all models of added origins!",
                "Format: \"id:name\", for example: ",
                "\"6:Tollan\"",
                "\"7:P4X-256\"",
                "!DO NOT CHANGE ANYTHING IF YOU DON'T KNOW WHAT ARE YOU DOING!"
        })
        public String[] additionalOrigins = {};
    }

    public static class WorldOreGenerator {
        @Name("Enable Naquadah ore generation")
        @Comment({
                "Do you want to spawn naquadah ores in the Nether?",
        })
        public boolean naquadahEnable = true;

        @Name("Naquadah vein size")
        public int naquadahVeinSize = 8;

        @Name("Naquadah max veins in chunk")
        public int naquadahMaxVeinInChunk = 16;

        @Name("Enable Trinium ore generation")
        @Comment({
                "Do you want to spawn trinium ores in the End?",
        })
        public boolean triniumEnabled = true;

        @Name("Trinium vein size")
        public int triniumVeinSize = 2;

        @Name("Trinium max veins in chunk")
        public int triniumMaxVeinInChunk = 4;

        @Name("Enable Titanium ore generation")
        @Comment({
                "Do you want to spawn titanium ores in the Overworld?",
        })
        public boolean titaniumEnable = true;

        @Name("Titanium vein size")
        public int titaniumVeinSize = 4;

        @Name("Titanium max veins in chunk")
        public int titaniumMaxVeinInChunk = 8;
    }

    public static class WorldStargateGenerator {
        @Name("Enable random stargate generator")
        @RequiresMcRestart
        @Comment({
                "Generate stargate in world random.",
        })
        public boolean stargateRandomGeneratorEnabled = true;

        @Name("Enable random structures generator")
        @RequiresMcRestart
        @Comment({
                "Enable generation of structures in the world.",
                "This will not disable the stargate generation!",
        })
        public boolean structuresRandomGeneratorEnabled = true;


        @Name("Chance of generating stargates in Overworld")
        @RequiresMcRestart
        @RangeDouble(min = 0, max = 1f)
        public float stargateRGChanceOverworld = 0.0001f;

        @Name("Chance of generating stargates in End")
        @RequiresMcRestart
        @RangeDouble(min = 0, max = 1f)
        public float stargateRGChanceTheEnd = 0.00007f;

    }

    public static class MainMenuConfig {
        @RequiresMcRestart
        @Name("Disable JSG main menu")
        @Comment({
                "Disables showing custom main menu",
                "WARNING! - Requires reloading!"
        })
        public boolean disableJSGMainMenu = false;

        @Name("Enable debug mode")
        public boolean debugMode = false;

        @Name("Play music in main menu")
        public boolean playMusic = true;
    }

    public static class IntegrationsConfig {
        @RequiresMcRestart
        @Name("Enable Tinkers' Construct integration")
        @Comment({
                "WARNING! - Requires reloading!"
        })
        public boolean tConstructIntegration = true;

        @RequiresMcRestart
        @Name("Enable Open Computers integration")
        @Comment({
                "WARNING! - Requires reloading!"
        })
        public boolean ocIntegration = true;

        @Name("OC wireless network range (in blocks)")
        public int ocIntegrationWirelessRange = 20;

        @RequiresMcRestart
        @Name("Enable Thermal Expansion integration")
        @Comment({
                "WARNING! - Requires reloading!"
        })
        public boolean tExpansionIntegration = true;
    }

    public static class RandomIncomingConfig {

        @Name("Enable random incoming wormholes")
        @Comment({
                "Enable random incoming wormholes generator",
                "THIS OPTION CAN BE OVERRIDE BY SETTING IT IN STARGATE GUI"
        })
        public boolean enableRandomIncoming = true;

        @Name("Chance of spawning")
        @Comment({
                "10 = 1%"
        })
        @RangeInt(min = 1, max = 100)
        public int chance = 1;

        @Name("Entities to spawn")
        @Comment({
                "Format: \"modid:entityid\", for example: ",
                "\"minecraft:zombie\"",
                "\"minecraft:creeper\""
        })
        public String[] entitiesToSpawn = {
                "minecraft:zombie",
                "minecraft:skeleton"
        };
    }

    public static class AdvancementsConfig {
        @Name("Ranged Advancements radius")
        @Comment({
                "Players in this radius around triggered pos will get Advancement."
        })
        public int radius = 25;
    }

    public static class CountDownConfig {
        @Name("Delay after zero-time (seconds)")
        public int zeroDelay = 5;

        @Name("Delay to start dialing after countdown start (seconds)")
        public int dialStartDelay = 5;
    }

    public static class DevConfig {
        @Name("Dev mode")
        public boolean enableDevMode = false;
        @Name("x")
        public float x = 0f;
        @Name("y")
        public float y = 0f;
        @Name("z")
        public float z = 0f;
        @Name("x2")
        public float x2 = 0f;
        @Name("y2")
        public float y2 = 0f;
        @Name("z2")
        public float z2 = 0f;
        @Name("s")
        public float s = 1f;
        @Name("sz")
        public float sz = 0f;
        @Name("yz")
        public float yz = 0f;
        @Name("tz")
        public float tz = 0f;
    }

    @SuppressWarnings("unused")
    public static void rescaleToConfig() {
        GlStateManager.translate(JSGConfig.devConfig.x, JSGConfig.devConfig.y, JSGConfig.devConfig.z);
        GlStateManager.scale(JSGConfig.devConfig.s, JSGConfig.devConfig.s, JSGConfig.devConfig.s);
    }

    @SuppressWarnings("unused")
    public static void rotateToConfig() {
        GlStateManager.rotate(JSGConfig.devConfig.x2, 1, 0, 0);
        GlStateManager.rotate(JSGConfig.devConfig.y2, 0, 1, 0);
        GlStateManager.rotate(JSGConfig.devConfig.z2, 0, 0, 1);
    }

    @SuppressWarnings("unused")
    public static JSGAxisAlignedBB createHitbox() {
        return new JSGAxisAlignedBB(JSGConfig.devConfig.x, JSGConfig.devConfig.y, JSGConfig.devConfig.z, JSGConfig.devConfig.x2, JSGConfig.devConfig.y2, JSGConfig.devConfig.z2);
    }

    public static void resetCache() {
        stargateConfig.cachedInvincibleBlocks = null;
        stargateConfig.cachedBiomeMatchesReverse = null;
        stargateConfig.cachedBiomeOverrideBlocks = null;
        stargateConfig.cachedBiomeOverrideBlocksReverse = null;
    }
}
