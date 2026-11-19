package io.github.eeeeeeruki.ponderererer.mixin;

import io.github.eeeeeeruki.ponderererer.world.LiveModelData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Lets marked positions in a Ponder scene render with their block entity's model data. */
@Mixin(targets = "net.createmod.catnip.impl.client.render.model.BakedModelBuffererImpl", remap = false)
public abstract class BakedModelBuffererMixin {
    @Redirect(method = {"bufferModel", "bufferBlocks"},
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/BlockAndTintGetter;getModelData(Lnet/minecraft/core/BlockPos;)Lnet/neoforged/neoforge/client/model/data/ModelData;"),
            require = 0)
    private static ModelData ponderererer$liveModelData(BlockAndTintGetter level, BlockPos pos) {
        ModelData live = LiveModelData.get(level, pos);
        return live != null ? live : level.getModelData(pos);
    }
}
