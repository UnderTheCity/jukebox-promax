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
 * 1.16.4 服务端侧：放唱片({@code setRecord})时读取唱片自定义名入缓存；
 * 唱片被取出/方块被拆（{@code clearContent}）时打停止标记。
 * （单机集成服务端与客户端同进程，静态缓存即可传递。）
 *
 * 1.16.4 的方块实体没有 startPlaying/setItem，放/取的逻辑落在
 * {@code JukeboxBlockEntity.setRecord} 与 {@code clearContent}。
 */
@Mixin(JukeboxBlockEntity.class)
public abstract class JukeboxBlockEntityMixin {

	@Inject(method = "setRecord", at = @At("HEAD"))
	private void recordJukebox$onSetRecord(ItemStack stack, CallbackInfo ci) {
		JukeboxBlockEntity self = (JukeboxBlockEntity) (Object) this;
		BlockPos pos = self.getBlockPos();
		DiscNameCache.clear();
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

	@Inject(method = "clearContent", at = @At("HEAD"))
	private void recordJukebox$onClearContent(CallbackInfo ci) {
		// 唱片被取走/方块被拆（dropRecording/onRemove 都会走 clearContent）→ 打停止标记并清名字
		JukeboxBlockEntity self = (JukeboxBlockEntity) (Object) this;
		DiscStopCache.mark(self.getBlockPos());
		DiscNameCache.clear();
	}
}
