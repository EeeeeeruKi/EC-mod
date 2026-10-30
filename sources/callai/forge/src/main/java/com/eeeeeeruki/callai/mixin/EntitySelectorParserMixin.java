package com.eeeeeeruki.callai.mixin;

import com.eeeeeeruki.callai.GeminiSelector;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Mixin(EntitySelectorParser.class)
public abstract class EntitySelectorParserMixin {

    /** 「@ai」を正しいセレクターとして読み込む */
    @Inject(method = "parse", at = @At("HEAD"), cancellable = true)
    private void callai$parseGemini(CallbackInfoReturnable<EntitySelector> cir) {
        EntitySelectorParser self = (EntitySelectorParser) (Object) this;
        if (GeminiSelector.tryConsume(self.getReader())) {
            cir.setReturnValue(GeminiSelector.build(self));
        }
    }

    /** OPが「@」を打った時の候補（@p @a … の並び）に足す */
    @Inject(method = "fillSelectorSuggestions", at = @At("TAIL"))
    private static void callai$suggestSelector(SuggestionsBuilder builder, CallbackInfo ci) {
        GeminiSelector.suggest(builder);
    }

    /** OPじゃない人にも候補を出す（プレイヤー名の候補と一緒に出ます） */
    @Inject(method = "suggestNameOrSelector", at = @At("HEAD"))
    private void callai$suggestName(SuggestionsBuilder builder, Consumer<SuggestionsBuilder> consumer,
                                        CallbackInfoReturnable<CompletableFuture<Suggestions>> cir) {
        GeminiSelector.suggest(builder);
    }
}
