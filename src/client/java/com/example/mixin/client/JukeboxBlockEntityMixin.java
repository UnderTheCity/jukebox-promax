package com.example.mixin.client;

import com.example.client.sound.DiscNameCache;
import com.example.client.sound.DiscStopCache;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.20.1 服务端侧：放唱片开始播放时读取唱片自定义名入缓存；唱片被移除/方块被拆时打停止标记。
 * （单机集成服务端与客户端同进程，静态缓存即可传递。）
 */
@Mixin(JukeboxBlockEntity.class)
public abstract class JukeboxBlockEntityMixin {

	@Inject(method = "startPlaying", at = @At("HEAD"))
	private void recordJukebox$cacheName(CallbackInfo ci) {
		JukeboxBlockEntity self = (JukeboxBlockEntity) (Object) this;
		BlockPos pos = self.getBlockPos();
		DiscNameCache.clear();
		ItemStack stack = self.getItem(0);
		if (stack != null && !stack.isEmpty()) {
			// 只有确实设置过 display.Name（铁砧改名）才记录；否则视为未改名
			CompoundTag display = stack.getTagElement("display");
			if (display != null && display.contains("Name")) {
				String customName = stack.getHoverName().getString().trim();
				if (!customName.isEmpty()) {
					DiscNameCache.store(pos, customName);
				}
			}
		}
	}

	@Inject(method = "setItem", at = @At("HEAD"))
	private void recordJukebox$onSetItem(int slot, ItemStack stack, CallbackInfo ci) {
		// 唱片被取走/换成空 → 打停止标记并清名字缓存
		if (stack == null || stack.isEmpty()) {
			JukeboxBlockEntity self = (JukeboxBlockEntity) (Object) this;
			DiscStopCache.mark(self.getBlockPos());
			DiscNameCache.clear();
		}
	}

	@Inject(method = "popOutRecord", at = @At("HEAD"))
	private void recordJukebox$onPopOut(CallbackInfo ci) {
		// 空手取出唱片 → 打停止标记
		JukeboxBlockEntity self = (JukeboxBlockEntity) (Object) this;
		DiscStopCache.mark(self.getBlockPos());
		DiscNameCache.clear();
	}
}
