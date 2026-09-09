package com.example.mixin.client;

import com.example.client.sound.DiscNameCache;
import com.example.client.sound.DiscStopCache;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 唱片机方块被移除时打"立即停止"标记（此路径不经过 JukeboxSongPlayer#stop）。
 */
@Mixin(JukeboxBlockEntity.class)
public abstract class JukeboxBlockEntityMixin {

	@Inject(method = "setRemoved", at = @At("HEAD"))
	private void templateMod$markRemoved(CallbackInfo ci) {
		BlockPos pos = ((JukeboxBlockEntity) (Object) this).getBlockPos();
		DiscStopCache.mark(pos);
		DiscNameCache.clear();
	}
}
