package dev.eeeeeeruki.layeredarmor.mixin.compat;

import dev.eeeeeeruki.layeredarmor.compat.ColdSweatCompat;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.Map;

/** Cold Sweat（2.x）：装備の防寒材・温度アイテム・温度系の修飾子に重ね着の防具を含める */
@Pseudo
@Mixin(targets = "com.momosoftworks.coldsweat.common.capability.handler.EntityTempManager", remap = false)
public abstract class ColdSweatEntityTempManagerMixin {

    @Redirect(method = {
            "getInsulatorsOnEntity(Lnet/minecraft/world/entity/LivingEntity;)Ljava/util/Map;",
            "getItemTemperaturesOnEntity(Lnet/minecraft/world/entity/LivingEntity;)Ljava/util/Map;",
            "getAllEquipmentAttributeModifiers(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/ai/attributes/AttributeInstance;Lnet/minecraft/world/entity/ai/attributes/AttributeModifier$Operation;)Ljava/util/Collection;"
    }, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;m_6844_(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;", remap = false),
            require = 0, remap = false)
    private static ItemStack layeredarmor$getItemBySlot(LivingEntity entity, EquipmentSlot slot) {
        return ColdSweatCompat.getItemBySlot(entity, slot);
    }

    @Inject(method = "getInsulatorsOnEntity(Lnet/minecraft/world/entity/LivingEntity;)Ljava/util/Map;", at = @At("RETURN"), require = 0, remap = false)
    private static void layeredarmor$insulators(LivingEntity entity, CallbackInfoReturnable<Map> cir) {
        ColdSweatCompat.mergeMap(entity, cir.getReturnValue(), () -> (Map) ColdSweatCompat.call("getInsulatorsOnEntity", entity));
    }

    @Inject(method = "getItemTemperaturesOnEntity(Lnet/minecraft/world/entity/LivingEntity;)Ljava/util/Map;", at = @At("RETURN"), require = 0, remap = false)
    private static void layeredarmor$temperatures(LivingEntity entity, CallbackInfoReturnable<Map> cir) {
        ColdSweatCompat.mergeMap(entity, cir.getReturnValue(), () -> (Map) ColdSweatCompat.call("getItemTemperaturesOnEntity", entity));
    }

    @Inject(method = "getAllEquipmentAttributeModifiers(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/ai/attributes/AttributeInstance;Lnet/minecraft/world/entity/ai/attributes/AttributeModifier$Operation;)Ljava/util/Collection;",
            at = @At("RETURN"), require = 0, remap = false)
    private static void layeredarmor$modifiers(LivingEntity entity, AttributeInstance instance, AttributeModifier.Operation operation,
                                               CallbackInfoReturnable<Collection> cir) {
        ColdSweatCompat.mergeCollection(entity, cir.getReturnValue(), () -> (Collection) ColdSweatCompat.call("getAllEquipmentAttributeModifiers", entity, instance, operation));
    }
}
