package com.infamous.dungeons_gear.items.artifacts;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.infamous.dungeons_gear.combat.NetworkHandler;
import com.infamous.dungeons_gear.combat.PacketBreakItem;
import com.infamous.dungeons_gear.utilties.AreaOfEffectHelper;
import com.infamous.dungeons_gear.utilties.DescriptionHelper;
import com.infamous.dungeons_gear.utilties.SoundHelper;
import com.infamous.dungeons_libraries.capabilities.soulcaster.SoulCasterHelper;
import com.infamous.dungeons_libraries.items.interfaces.ISoulConsumer;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.List;
import java.util.UUID;

import static com.infamous.dungeons_libraries.attribute.AttributeRegistry.SOUL_GATHERING;

public class LightningRodItem extends ArtifactItem implements ISoulConsumer {
    public LightningRodItem(Properties properties) {
        super(properties);
    }

    public ActionResult<ItemStack> procArtifact(ItemUseContext c) {
        PlayerEntity playerIn = c.getPlayer();
        ItemStack itemstack = c.getItemInHand();

        if(SoulCasterHelper.consumeSouls(playerIn, itemstack)){
            //AbilityUtils.castLightningBoltAtBlockPos(itemUseContextPlayer, blockPos);
            AreaOfEffectHelper.electrifyNearbyEnemies(playerIn, 5, 5, Integer.MAX_VALUE);
            SoundHelper.playLightningStrikeSounds(playerIn);
            itemstack.hurtAndBreak(1, playerIn, (entity) -> NetworkHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), new PacketBreakItem(entity.getId(), itemstack)));
            ArtifactItem.putArtifactOnCooldown(playerIn, itemstack.getItem());
        }

        return new ActionResult<>(ActionResultType.SUCCESS, itemstack);
    }

    @Override
    public void appendHoverText(ItemStack stack, World world, List<ITextComponent> list, ITooltipFlag flag)
    {
        super.appendHoverText(stack, world, list, flag);
        DescriptionHelper.addFullDescription(list, stack);
    }

    @Override
    public int getCooldownInSeconds() {
        return 2;
    }

    @Override
    public int getDurationInSeconds() {
        return 0;
    }

    @Override
    public float getActivationCost(ItemStack stack) {
        return 15;
    }

    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(int slotIndex) {
        return getAttributeModifiersForSlot(getUUIDForSlot(slotIndex));
    }

    private ImmutableMultimap<Attribute, AttributeModifier> getAttributeModifiersForSlot(UUID slot_uuid) {
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(SOUL_GATHERING.get(), new AttributeModifier(slot_uuid, "Artifact modifier", 1, AttributeModifier.Operation.ADDITION));
        return builder.build();
    }
}
