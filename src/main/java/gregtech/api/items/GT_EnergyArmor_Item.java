package gregtech.api.items;

import static gregtech.api.enums.Mods.GregTech;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.ISpecialArmor;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingFallEvent;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.GregTech_API;
import gregtech.api.util.GT_LanguageManager;
import gregtech.api.util.GT_ModHandler;
import gregtech.api.util.GT_Utility;

public class GT_EnergyArmor_Item extends ItemArmor implements ISpecialArmor {

    public static Map<EntityPlayer, Float> jumpChargeMap = new ConcurrentHashMap<>();
    public int mCharge, mTransfer, mTier, mDamageEnergyCost, mSpecials;
    public boolean mChargeProvider;
    public double mArmorAbsorbtionPercentage;

    public GT_EnergyArmor_Item(int aID, String aUnlocalized, String aEnglish, int aCharge, int aTransfer, int aTier,
        int aDamageEnergyCost, int aSpecials, double aArmorAbsorbtionPercentage, boolean aChargeProvider, int aType,
        int aArmorIndex) {
        super(ArmorMaterial.DIAMOND, aArmorIndex, aType);
        setMaxStackSize(1);
        setMaxDamage(100);
        setNoRepair();
        setUnlocalizedName(aUnlocalized);
        GT_LanguageManager.addStringLocalization(getUnlocalizedName() + ".name", aEnglish);
        mCharge = Math.max(1, aCharge);
        mTransfer = Math.max(1, aTransfer);
        mTier = Math.max(1, aTier);
        mSpecials = aSpecials;
        mChargeProvider = aChargeProvider;
        mDamageEnergyCost = Math.max(0, aDamageEnergyCost);
        mArmorAbsorbtionPercentage = aArmorAbsorbtionPercentage;

        setCreativeTab(GregTech_API.TAB_GREGTECH);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private static void setCharge(ItemStack aStack) {
        NBTTagCompound tNBT = aStack.getTagCompound();
        if (tNBT == null) tNBT = new NBTTagCompound();
        tNBT.setInteger("charge", 1000000000);
        aStack.setTagCompound(tNBT);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack aStack, World aWorld, EntityPlayer aPlayer) {
        ItemStack tStack = aPlayer.inventory.armorInventory[3 - armorType];
        if (tStack != null) {
            for (int i = 0; i < 9; i++) {
                if (aPlayer.inventory.mainInventory[i] == aStack) {
                    aPlayer.inventory.armorInventory[3 - armorType] = aPlayer.inventory.mainInventory[i];
                    aPlayer.inventory.mainInventory[i] = tStack;
                    return tStack;
                }
            }
        }
        return super.onItemRightClick(aStack, aWorld, aPlayer);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister aIconRegister) {
        this.itemIcon = aIconRegister.registerIcon(GregTech.getResourcePath(getUnlocalizedName()));
    }

    @Override
    public void addInformation(ItemStack aStack, EntityPlayer aPlayer, List<String> aList, boolean aF3_H) {
        aList.add("Tier: " + mTier);
        if ((mSpecials & 1) != 0) aList.add("Rebreather");
        if ((mSpecials & 2) != 0) aList.add("Inertia Damper");
        if ((mSpecials & 4) != 0) aList.add("Food Replicator");
        if ((mSpecials & 8) != 0) aList.add("Medicine Module");
        if ((mSpecials & 16) != 0) aList.add("Lamp");
        if ((mSpecials & 32) != 0) aList.add("Solarpanel");
        if ((mSpecials & 64) != 0) aList.add("Extinguisher Module");
        if ((mSpecials & 128) != 0) aList.add("Jump Booster");
        if ((mSpecials & 256) != 0) aList.add("Speed Booster");
        if ((mSpecials & 512) != 0) aList.add("Invisibility Field");
        if ((mSpecials & 1024) != 0) aList.add("Infinite Charge");
    }

    @Override
    public void onArmorTick(World aWorld, EntityPlayer aPlayer, ItemStack aStack) {
        if (mSpecials == 0) return;

        if (!aPlayer.worldObj.isRemote && (mSpecials & 1) != 0) {
            int airSupply = aPlayer.getAir();
            if (GT_ModHandler.canUseElectricItem(aStack, 1000) && airSupply < 50) {
                aPlayer.setAir(airSupply + 250);
                GT_ModHandler.useElectricItem(aStack, 1000, aPlayer);
            }
        }

        if (!aPlayer.worldObj.isRemote && (mSpecials & 4) != 0) {
            if (GT_ModHandler.canUseElectricItem(aStack, 50000) && aPlayer.getFoodStats()
                .needFood()) {
                aPlayer.getFoodStats()
                    .addStats(1, 0.0F);
                GT_ModHandler.useElectricItem(aStack, 50000, aPlayer);
            }
        }

        if ((mSpecials & 8) != 0) {
            if (GT_ModHandler.canUseElectricItem(aStack, 10000) && aPlayer.isPotionActive(Potion.poison)) {
                GT_Utility.removePotion(aPlayer, Potion.poison.id);
                GT_ModHandler.useElectricItem(aStack, 10000, aPlayer);
            }
            if (GT_ModHandler.canUseElectricItem(aStack, 100000) && aPlayer.isPotionActive(Potion.wither)) {
                GT_Utility.removePotion(aPlayer, Potion.wither.id);
                GT_ModHandler.useElectricItem(aStack, 100000, aPlayer);
            }
        }

        if ((mSpecials & 64) != 0) {
            aPlayer.setFire(0);
        }

        if (!aPlayer.worldObj.isRemote && (mSpecials & 128) != 0) {
            float jumpCharge = jumpChargeMap.containsKey(aPlayer) ? (Float) jumpChargeMap.get(aPlayer) : 1.0F;

            if (GT_ModHandler.canUseElectricItem(aStack, 1000) && aPlayer.onGround && jumpCharge < 1.0F) {
                jumpCharge = 1.0F;
                GT_ModHandler.useElectricItem(aStack, 1000, aPlayer);
            }

            if (aPlayer.motionY >= 0.0D && jumpCharge > 0.0F && !aPlayer.isInWater()) {
                if (GT_ModHandler.getJumpKeyDown(aPlayer) && GT_ModHandler.getBoostKeyDown(aPlayer)) {
                    if (jumpCharge == 1.0F) {
                        aPlayer.motionX *= 3.5D;
                        aPlayer.motionZ *= 3.5D;
                    }

                    aPlayer.motionY += (jumpCharge * 0.3F);
                    jumpCharge = (float) (jumpCharge * 0.75D);
                } else if (jumpCharge < 1.0F) {
                    jumpCharge = 0.0F;
                }
            }

            jumpChargeMap.put(aPlayer, jumpCharge);
        }

        if ((mSpecials & 256) != 0) {
            if (GT_ModHandler.canUseElectricItem(aStack, 100) && aPlayer.isSprinting()
                && (aPlayer.onGround && Math.abs(aPlayer.motionX) + Math.abs(aPlayer.motionZ) > 0.10000000149011612D
                    || aPlayer.isInWater())) {
                GT_ModHandler.useElectricItem(aStack, 100, aPlayer);
                float bonus = 0.22F;

                if (aPlayer.isInWater()) {
                    GT_ModHandler.useElectricItem(aStack, 100, aPlayer);
                    bonus = 0.1F;

                    if (aPlayer.motionY > 0) {
                        aPlayer.motionY += 0.10000000149011612D;
                    }
                }

                if (bonus > 0.0F) {
                    aPlayer.moveFlying(0.0F, 1.0F, bonus);
                }
            }
        }

        if ((mSpecials & 512) != 0) {
            if (GT_ModHandler.canUseElectricItem(aStack, 10000)) {
                GT_ModHandler.useElectricItem(aStack, 10000, aPlayer);
                aPlayer.addPotionEffect(new PotionEffect(Potion.invisibility.getId(), 25, 1, true));
            }
        }

        if (!aPlayer.worldObj.isRemote && (mSpecials & (16 | 32)) != 0) {
            // if (GregTech_API.sWorldTickCounter%20==0) {
            ItemStack tTargetChargeItem = aStack, tTargetDechargeItem = aStack;

            if (GT_ModHandler.chargeElectricItem(tTargetChargeItem, 1, Integer.MAX_VALUE, true, true) < 1) {
                tTargetChargeItem = aPlayer.inventory.armorInventory[2];
            }
            if (GT_ModHandler.dischargeElectricItem(tTargetDechargeItem, 10, Integer.MAX_VALUE, true, true, true)
                < 10) {
                tTargetDechargeItem = aPlayer.inventory.armorInventory[2];
            }

            if (tTargetChargeItem == null || !GT_ModHandler.isElectricItem(tTargetChargeItem)) {
                tTargetChargeItem = null;
            }

            if (aPlayer.worldObj.isDaytime() && aPlayer.worldObj.canBlockSeeTheSky(
                MathHelper.floor_double(aPlayer.posX),
                MathHelper.floor_double(aPlayer.posY + 1),
                MathHelper.floor_double(aPlayer.posZ))) {
                if ((mSpecials & 32) != 0 && tTargetChargeItem != null) {
                    GT_ModHandler.chargeElectricItem(tTargetChargeItem, 20, Integer.MAX_VALUE, true, false);
                }
            } else {
                /*
                 * TODO: if ((mSpecials & 16) != 0 && tTargetDechargeItem != null &&
                 * GT_ModHandler.canUseElectricItem(tTargetDechargeItem, 10)) { if (aPlayer.worldObj.getBlock
                 * ((int)aPlayer.posX, (int)aPlayer.posY+1, (int)aPlayer.posZ) == Blocks.air) aPlayer.worldObj.setBlock
                 * ((int)aPlayer.posX, (int)aPlayer.posY+1, (int)aPlayer.posZ, GregTech_API.sBlockList[3]);
                 * GT_ModHandler.useElectricItem(tTargetDechargeItem, 10, aPlayer); }
                 */
                // }
            }
        }
    }

    @Override
    public boolean getShareTag() {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item aItem, CreativeTabs creativeTab, List<ItemStack> outputSubItems) {
        ItemStack tCharged = new ItemStack(this, 1), tUncharged = new ItemStack(this, 1, getMaxDamage());
        GT_ModHandler.chargeElectricItem(tCharged, Integer.MAX_VALUE, Integer.MAX_VALUE, true, false);
        outputSubItems.add(tCharged);
        outputSubItems.add(tUncharged);
    }

    public boolean canProvideEnergy(ItemStack aStack) {
        if ((mSpecials & 1024) != 0) setCharge(aStack);
        return mChargeProvider;
    }

    public Item getChargedItem(ItemStack aStack) {
        if ((mSpecials & 1024) != 0) setCharge(aStack);
        return this;
    }

    public Item getEmptyItem(ItemStack aStack) {
        if ((mSpecials & 1024) != 0) setCharge(aStack);
        return this;
    }

    public int getMaxCharge(ItemStack aStack) {
        if ((mSpecials & 1024) != 0) setCharge(aStack);
        return mCharge;
    }

    public int getTier(ItemStack aStack) {
        if ((mSpecials & 1024) != 0) setCharge(aStack);
        return mTier;
    }

    public int getTransferLimit(ItemStack aStack) {
        if ((mSpecials & 1024) != 0) setCharge(aStack);
        return mTransfer;
    }

    @Override
    public int getItemEnchantability() {
        return 0;
    }

    @Override
    public boolean isBookEnchantable(ItemStack ingredient, ItemStack bookEnchant) {
        return false;
    }

    @Override
    public boolean getIsRepairable(ItemStack toBeRepaired, ItemStack repairWith) {
        return false;
    }

    // TODO: @ForgeSubscribe
    public void onEntityLivingFallEvent(LivingFallEvent event) {
        if (!event.entity.worldObj.isRemote && event.entity instanceof EntityPlayer player) {
            for (int i = 0; i < 4; i++) {
                ItemStack armor = player.inventory.armorInventory[i];
                if (armor != null && armor.getItem() == this && (mSpecials & 2) != 0) {
                    int distanceFactor = (int) event.distance - 3;
                    int energyCost = (this.mDamageEnergyCost * distanceFactor) / 4;
                    if (energyCost <= GT_ModHandler
                        .dischargeElectricItem(armor, Integer.MAX_VALUE, Integer.MAX_VALUE, true, true, true)) {
                        GT_ModHandler.dischargeElectricItem(armor, energyCost, Integer.MAX_VALUE, true, false, true);
                        event.setCanceled(true);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public ISpecialArmor.ArmorProperties getProperties(EntityLivingBase player, ItemStack armor, DamageSource source,
        double damage, int slotIndex) {
        return new ISpecialArmor.ArmorProperties(
            (source == DamageSource.fall && (mSpecials & 2) != 0) ? 10 : 0,
            getBaseAbsorptionRatio() * mArmorAbsorbtionPercentage,
            mDamageEnergyCost > 0 ? 25
                * GT_ModHandler.dischargeElectricItem(armor, Integer.MAX_VALUE, Integer.MAX_VALUE, true, true, true)
                / mDamageEnergyCost : 0);
    }

    @Override
    public int getArmorDisplay(EntityPlayer player, ItemStack armor, int slotIndex) {
        return (int) Math.round(20.0D * getBaseAbsorptionRatio() * mArmorAbsorbtionPercentage);
    }

    @Override
    public void damageArmor(EntityLivingBase entity, ItemStack itemStack, DamageSource source, int damage,
        int slotIndex) {
        GT_ModHandler
            .dischargeElectricItem(itemStack, damage * mDamageEnergyCost, Integer.MAX_VALUE, true, false, true);
    }

    private double getBaseAbsorptionRatio() {
        if (mArmorAbsorbtionPercentage <= 0) return 0.00;
        return switch (this.armorType) {
            case 0 -> 0.15;
            case 1 -> 0.40;
            case 2 -> 0.30;
            case 3 -> 0.15;
            default -> 0.00;
        };
    }
}
