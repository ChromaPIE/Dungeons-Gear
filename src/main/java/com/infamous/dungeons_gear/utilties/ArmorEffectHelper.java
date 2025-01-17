package com.infamous.dungeons_gear.utilties;

import com.infamous.dungeons_gear.capabilities.combo.ICombo;
import com.infamous.dungeons_gear.capabilities.summoning.ISummonable;
import com.infamous.dungeons_gear.capabilities.summoning.ISummoner;
import com.infamous.dungeons_gear.enchantments.lists.ArmorEnchantmentList;
import com.infamous.dungeons_gear.enchantments.lists.MeleeRangedEnchantmentList;
import com.infamous.dungeons_gear.goals.*;
import com.infamous.dungeons_gear.items.interfaces.IArmor;
import com.infamous.dungeons_gear.items.interfaces.IMeleeWeapon;
import com.infamous.dungeons_gear.items.interfaces.IRangedWeapon;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.NearestAttackableTargetGoal;
import net.minecraft.entity.monster.CreeperEntity;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

public class ArmorEffectHelper {
    public static void summonOrTeleportBat(PlayerEntity playerEntity, World world) {
        ISummoner summonerCap = CapabilityHelper.getSummonerCapability(playerEntity);
        if(summonerCap == null) return;
        if(summonerCap.getSummonedBat() == null){
            BatEntity batEntity = EntityType.BAT.create(world);
            if (batEntity!= null) {
                ISummonable summonable = CapabilityHelper.getSummonableCapability(batEntity);
                if(summonable != null){

                    summonable.setSummoner(playerEntity.getUUID());
                    summonerCap.setSummonedBat(batEntity.getUUID());

                    createBat(playerEntity, world, batEntity);
                }
            }
        } else{
            if(world instanceof ServerWorld){
                Entity entity = ((ServerWorld)world).getEntity(summonerCap.getSummonedBat());
                if(entity instanceof BatEntity){
                    BatEntity batEntity = (BatEntity) entity;
                    batEntity.teleportToWithTicket(playerEntity.getX() + playerEntity.getEyeHeight(), playerEntity.getY() + playerEntity.getEyeHeight(), playerEntity.getZ() + playerEntity.getEyeHeight());
                }
            }
        }
    }

    private static void createBat(PlayerEntity playerEntity, World world, BatEntity batEntity) {
        batEntity.moveTo((double)playerEntity.getX() + playerEntity.getEyeHeight(), (double)playerEntity.getY() + playerEntity.getEyeHeight(), (double)playerEntity.getZ() + playerEntity.getEyeHeight(), 0.0F, 0.0F);

        batEntity.goalSelector.addGoal(1, new BatMeleeAttackGoal(batEntity, 1.0D, true));
        batEntity.goalSelector.addGoal(2, new BatFollowOwnerGoal(batEntity, 2.1D, 10.0F, 2.0F, false));


        batEntity.targetSelector.addGoal(1, new BatOwnerHurtByTargetGoal(batEntity));
        batEntity.targetSelector.addGoal(2, new BatOwnerHurtTargetGoal(batEntity));
        batEntity.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(batEntity, LivingEntity.class, 5, false, false,
                (entityIterator) -> entityIterator instanceof IMob && !(entityIterator instanceof CreeperEntity)));

        SoundHelper.playCreatureSound(playerEntity, SoundEvents.BAT_AMBIENT);
        world.addFreshEntity(batEntity);
    }

    public static void teleportOnHit(LivingEntity livingEntity){
        World world = livingEntity.getCommandSenderWorld();
        if (!world.isClientSide) {

            for(int i = 0; i < 16; ++i) {
                double teleportX = livingEntity.getX() + (livingEntity.getRandom().nextDouble() - 0.5D) * 16.0D;
                double teleportY = MathHelper.clamp(livingEntity.getY() + (double)(livingEntity.getRandom().nextInt(16) - 8), 0.0D, (double)(world.getHeight() - 1));
                double teleportZ = livingEntity.getZ() + (livingEntity.getRandom().nextDouble() - 0.5D) * 16.0D;
                if (livingEntity.isPassenger()) {
                    livingEntity.stopRiding();
                }

                if (livingEntity.randomTeleport(teleportX, teleportY, teleportZ, true)) {
                    SoundEvent soundEvent = livingEntity instanceof FoxEntity ? SoundEvents.FOX_TELEPORT : SoundEvents.CHORUS_FRUIT_TELEPORT;
                    world.playSound((PlayerEntity)null, livingEntity.blockPosition(), soundEvent, SoundCategory.PLAYERS, 1.0F, 1.0F);
                    livingEntity.playSound(soundEvent, 1.0F, 1.0F);
                    break;
                }
            }
        }
    }

    public static void handleInvulnerableJump(PlayerEntity playerEntity, ItemStack helmet, ItemStack chestplate) {
        boolean invulnerableJump = helmet.getItem() instanceof IArmor && ((IArmor) helmet.getItem()).doBriefInvulnerabilityWhenJumping();
        boolean invulnerableJump2 = chestplate.getItem() instanceof IArmor && ((IArmor) chestplate.getItem()).doBriefInvulnerabilityWhenJumping();
        boolean doInvulnerableJump = invulnerableJump || invulnerableJump2;

        if (doInvulnerableJump) {
            EffectInstance resistance = new EffectInstance(Effects.DAMAGE_RESISTANCE, 20, 4);
            playerEntity.addEffect(resistance);
        }
    }

    public static void handleJumpBoost(PlayerEntity playerEntity, ItemStack helmet, ItemStack chestplate) {
        float jumpBoost = helmet.getItem() instanceof IArmor ? (float) ((IArmor) helmet.getItem()).getLongerRolls() : 0;
        float jumpBoost2 = chestplate.getItem() instanceof IArmor ? (float) ((IArmor) chestplate.getItem()).getLongerRolls() : 0;
        float totalJumpBoost = jumpBoost * 0.002F + jumpBoost2 * 0.002F;

        if (totalJumpBoost > 0) {
            playerEntity.setDeltaMovement(playerEntity.getDeltaMovement().add(0, totalJumpBoost, 0));
        }
    }

    public static void handleJumpEnchantments(PlayerEntity playerEntity, ItemStack helmet, ItemStack chestplate) {
        if (ModEnchantmentHelper.hasEnchantment(playerEntity, ArmorEnchantmentList.ELECTRIFIED)) {
            SoundHelper.playLightningStrikeSounds(playerEntity);
            AreaOfEffectHelper.electrifyNearbyEnemies(playerEntity, 5, 5, 3);
        }

        if (ModEnchantmentHelper.hasEnchantment(playerEntity, ArmorEnchantmentList.FIRE_TRAIL)) {
            int fireTrailLevel = EnchantmentHelper.getEnchantmentLevel(ArmorEnchantmentList.FIRE_TRAIL, playerEntity);
            AreaOfEffectHelper.burnNearbyEnemies(playerEntity, 1.0F * fireTrailLevel, 1.5F);
        }

        // TODO: Beenest Armor and Buzzynest Armor
        if (ModEnchantmentHelper.hasEnchantment(playerEntity, ArmorEnchantmentList.TUMBLEBEE)) {
            int tumblebeeLevel = EnchantmentHelper.getEnchantmentLevel(ArmorEnchantmentList.TUMBLEBEE, playerEntity);

            float tumblebeeRand = playerEntity.getRandom().nextFloat();
            if (tumblebeeRand <= 0.333F * tumblebeeLevel) {
                summonTumblebeeBee(playerEntity);
            }
        }

        boolean highlandArmorFlag = hasSwiftfootedBuiltIn(chestplate) || hasSwiftfootedBuiltIn(helmet);
        if (ModEnchantmentHelper.hasEnchantment(playerEntity, ArmorEnchantmentList.SWIFTFOOTED) || highlandArmorFlag) {
            int swiftfootedLevel = EnchantmentHelper.getEnchantmentLevel(ArmorEnchantmentList.SWIFTFOOTED, playerEntity);
            if (highlandArmorFlag) swiftfootedLevel++;
            EffectInstance speedBoost = new EffectInstance(Effects.MOVEMENT_SPEED, 60, swiftfootedLevel - 1);
            playerEntity.addEffect(speedBoost);
        }

        handleDynamoEnchantment(playerEntity);
    }

    private static boolean hasSwiftfootedBuiltIn(ItemStack stack) {
        return stack.getItem() instanceof IArmor && ((IArmor) stack.getItem()).hasSwiftfootedBuiltIn(stack);
    }

    private static void handleDynamoEnchantment(PlayerEntity playerEntity) {
        ItemStack mainhand = playerEntity.getMainHandItem();
        boolean uniqueWeaponFlag = hasDynamoBuiltIn(mainhand);
        if (ModEnchantmentHelper.hasEnchantment(mainhand, MeleeRangedEnchantmentList.DYNAMO) || uniqueWeaponFlag) {
            int dynamoLevel = EnchantmentHelper.getItemEnchantmentLevel(MeleeRangedEnchantmentList.DYNAMO, mainhand);
            if (uniqueWeaponFlag) dynamoLevel++;
            ICombo comboCap = CapabilityHelper.getComboCapability(playerEntity);
            if (comboCap == null) return;
            double originalDynamoMultiplier = comboCap.getDynamoMultiplier();
            double dynamoModifier = 1.0D + (0.5D * Math.max((dynamoLevel - 1), 0));
            comboCap.setDynamoMultiplier(originalDynamoMultiplier + dynamoModifier);
        }
    }

    private static boolean hasDynamoBuiltIn(ItemStack mainhand) {
        return mainhand.getItem() instanceof IMeleeWeapon && ((IMeleeWeapon) mainhand.getItem()).hasDynamoBuiltIn(mainhand) ||
                mainhand.getItem() instanceof IRangedWeapon && ((IRangedWeapon) mainhand.getItem()).hasDynamoBuiltIn(mainhand);
    }

    private static void summonTumblebeeBee(PlayerEntity playerEntity) {
        ISummoner summonerCap = CapabilityHelper.getSummonerCapability(playerEntity);
        if (summonerCap == null) return;
        BeeEntity beeEntity = EntityType.BEE.create(playerEntity.level);
        if (beeEntity != null) {
            ISummonable summonable = CapabilityHelper.getSummonableCapability(beeEntity);
            if (summonable != null && summonerCap.addTumblebeeBee(beeEntity.getUUID())) {
                summonable.setSummoner(playerEntity.getUUID());

                createBee(playerEntity, beeEntity);
            } else {
                beeEntity.remove();
            }
        }
    }

    private static void createBee(PlayerEntity playerEntity, BeeEntity beeEntity) {
        beeEntity.moveTo((double) playerEntity.getX() + 0.5D, (double) playerEntity.getY() + 0.05D, (double) playerEntity.getZ() + 0.5D, 0.0F, 0.0F);

        beeEntity.goalSelector.addGoal(2, new BeeFollowOwnerGoal(beeEntity, 2.1D, 10.0F, 2.0F, false));

        beeEntity.targetSelector.addGoal(1, new BeeOwnerHurtByTargetGoal(beeEntity));
        beeEntity.targetSelector.addGoal(2, new BeeOwnerHurtTargetGoal(beeEntity));
        beeEntity.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(beeEntity, LivingEntity.class, 5, false, false,
                (entityIterator) -> entityIterator instanceof IMob && !(entityIterator instanceof CreeperEntity)));

        SoundHelper.playCreatureSound(playerEntity, SoundEvents.BEE_LOOP);
        playerEntity.level.addFreshEntity(beeEntity);
    }


}
