package mrjake.aunis.item.tools;

import mrjake.aunis.Aunis;
import mrjake.aunis.capability.ItemCapabilityProvider;
import mrjake.aunis.entity.AunisEnergyProjectile;
import mrjake.aunis.item.renderer.CustomModel;
import mrjake.aunis.item.renderer.CustomModelItemInterface;
import mrjake.aunis.stargate.power.StargateItemEnergyStorage;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public abstract class EnergyWeapon extends Item implements CustomModelItemInterface {

    private final int maxEnergyStored;
    private final int energyPerShot;
    public String itemName;
    private CustomModel customModel;

    public EnergyWeapon(String itemName, int maxEnergyStored, int energyPerShot) {
        this.itemName = itemName;
        this.maxEnergyStored = maxEnergyStored;
        this.energyPerShot = energyPerShot;

        setRegistryName(new ResourceLocation(Aunis.ModID, this.itemName));
        setUnlocalizedName(Aunis.ModID + "." + this.itemName);
        setMaxStackSize(1);
        setCreativeTab(Aunis.aunisToolsCreativeTab);
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (isInCreativeTab(tab)) {
            items.add(new ItemStack(this));

            ItemStack stack = new ItemStack(this);
            StargateItemEnergyStorage energyStorage = (StargateItemEnergyStorage) stack.getCapability(CapabilityEnergy.ENERGY, null);
            energyStorage.setEnergyStored(energyStorage.getMaxEnergyStored());
            items.add(stack);
        }
    }

    @Nonnull
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, @Nonnull EntityPlayer player, @Nonnull EnumHand hand) {
        if (!world.isRemote) {
            StargateItemEnergyStorage energyStorage = (StargateItemEnergyStorage) player.getHeldItem(hand).getCapability(CapabilityEnergy.ENERGY, null);
            if (energyStorage != null && energyStorage.extractEnergy(energyPerShot, true) >= energyPerShot) {
                playShootSound(world, player);
                world.spawnEntity(AunisEnergyProjectile.createEnergyBall(world, player, this));
                energyStorage.extractEnergy(energyPerShot, false);
            }
        }
        return super.onItemRightClick(world, player, hand);
    }

    public abstract void playShootSound(World world, EntityPlayer player);

    public abstract DamageSource getDamageSource();

    public void setEnergyBallParams(AunisEnergyProjectile projectile) {
        projectile.maxAliveTime = 5;
        projectile.damage = 5.0F;
        projectile.igniteGround = true;
        projectile.paralyze = false;
        projectile.explode = false;
        projectile.invisible = true;
        projectile.damageSource = getDamageSource();
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int itemSlot, boolean isSelected) {
        if (!world.isRemote) {

        }
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(Aunis.getInProgress());
        tooltip.add("");
        IEnergyStorage energyStorage = stack.getCapability(CapabilityEnergy.ENERGY, null);
        if (energyStorage == null) {
            tooltip.add("NBTs are broken! This is a bug!");
            return;
        }

        String energy = String.format("%,d", energyStorage.getEnergyStored());
        String capacity = String.format("%,d", energyStorage.getMaxEnergyStored());

        tooltip.add(energy + " / " + capacity + " RF");

        String energyPercent = String.format("%.2f", energyStorage.getEnergyStored() / (float) energyStorage.getMaxEnergyStored() * 100) + " %";
        tooltip.add(energyPercent);
        tooltip.add("");
        tooltip.add(Aunis.proxy.localize("item.aunis.energyWeapon.available_shots") + (int) Math.floor((float) energyStorage.getEnergyStored() / energyPerShot) + "/" + (int) Math.floor((float) energyStorage.getMaxEnergyStored() / energyPerShot));
    }

    @Override
    public void setCustomModel(CustomModel customModel) {
        this.customModel = customModel;
    }

    public ItemCameraTransforms.TransformType getLastTransform() {
        return customModel.lastTransform;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return oldStack.getItem() != newStack.getItem();
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NBTTagCompound nbt) {
        return new ItemCapabilityProvider(stack, nbt, maxEnergyStored);
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return true;
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        IEnergyStorage energyStorage = stack.getCapability(CapabilityEnergy.ENERGY, null);
        if (energyStorage == null) return 1;
        return 1 - (energyStorage.getEnergyStored() / (double) energyStorage.getMaxEnergyStored());
    }
}
