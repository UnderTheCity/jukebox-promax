package com.example.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.Map;

/**
 * 区块卸载即停（原版唱片机音乐，1.16.4 版）：
 * 全局化后的唱片机声音不再随距离衰减，若其"锚点"所在区块被卸载却仍继续响，会显得异常。
 * 这里在渲染器每 tick 检查：若某正在播放的唱片机方块所在区块已不在客户端加载范围，就停掉它。
 * （自定义音乐的同类检查在 DiscPlaybackManager.tick 内完成。）
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererStopMixin {

	@Shadow
	@Final
	private Minecraft minecraft;

	@Shadow
	@Final
	private Map<BlockPos, SoundInstance> playingRecords;

	@Inject(method = "tick", at = @At("HEAD"))
	private void recordJukebox$stopUnloadedChunkRecords(CallbackInfo ci) {
		try {
			ClientLevel level = this.minecraft.level;
			if (level == null || playingRecords == null || playingRecords.isEmpty()) {
				return;
			}
			Iterator<Map.Entry<BlockPos, SoundInstance>> it = playingRecords.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<BlockPos, SoundInstance> e = it.next();
				BlockPos pos = e.getKey();
				// 区块已不在加载范围 → 停掉该声音
				if (!level.isLoaded(pos)) {
					this.minecraft.getSoundManager().stop(e.getValue());
					it.remove();
				}
			}
		} catch (Throwable t) {
			// 仅防御，不影响主流程
		}
	}
}
