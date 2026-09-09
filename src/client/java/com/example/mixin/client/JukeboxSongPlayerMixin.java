package com.example.mixin.client;

import com.example.client.sound.DiscNameCache;
import com.example.client.sound.DiscStopCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.JukeboxSongPlayer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在唱片机开始播放(服务端线程)时，读取该唱片机内唱片的自定义名并缓存，
 * 供客户端播放事件读取；在"唱片被取出/移除"时打即时停止标记。
 */
@Mixin(JukeboxSongPlayer.class)
public abstract class JukeboxSongPlayerMixin {

	@Shadow
	@Final
	private BlockPos blockPos;

	@Inject(method = "play", at = @At("HEAD"))
	private void templateMod$cacheDiscName(LevelAccessor level, Holder<JukeboxSong> song, CallbackInfo ci) {
		DiscNameCache.clear();
		if (level instanceof ServerLevel serverLevel
				&& serverLevel.getBlockEntity(this.blockPos) instanceof JukeboxBlockEntity jukebox) {
			ItemStack stack = jukebox.getTheItem();
			Component name = stack == null ? null : stack.get(DataComponents.CUSTOM_NAME);
			String customName = name == null ? null : name.getString().trim();
			if (customName != null && !customName.isBlank()) {
				DiscNameCache.store(this.blockPos, customName);
			}
		}
	}

	@Inject(method = "stop", at = @At("HEAD"))
	private void templateMod$markStopIfRemoved(LevelAccessor level, BlockState state, CallbackInfo ci) {
		// stop 触发来源可能是"唱片被取出/方块被拆"或"自然播放结束"。
		// 仅当该位置唱片物品已清空(真被取出)时才打"立即停止"标记，
		// 这样客户端播放管理器可立刻停止，不等方块状态同步。
		if (level instanceof ServerLevel serverLevel
				&& serverLevel.getBlockEntity(this.blockPos) instanceof JukeboxBlockEntity jukebox) {
			ItemStack stack = jukebox.getTheItem();
			if (stack == null || stack.isEmpty()) {
				DiscStopCache.mark(this.blockPos);
				DiscNameCache.clear();
			}
		}
	}
}
