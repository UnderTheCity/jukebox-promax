package com.example.mixin.client;

import com.example.client.sound.DiscPlaybackManager;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.16.4 不依赖 fabric 事件，直接用 Minecraft 主循环 tick 驱动播放管理器：
 * <ul>
 *   <li>不在世界内（标题/退出存档后）：重置管理器与跨会话残留缓存，
 *       保证"退出再进后自定义音乐仍正常"；</li>
 *   <li>在世界内：每 tick 推进播放逻辑（曲目切换/定时/红石/取出即停）。</li>
 * </ul>
 */
@Mixin(Minecraft.class)
public abstract class ClientTickMixin {

	@Inject(method = "tick", at = @At("HEAD"))
	private void recordJukebox$onTick(CallbackInfo ci) {
		Minecraft mc = Minecraft.getInstance();
		DiscPlaybackManager mgr = DiscPlaybackManager.instance();
		if (mc.level == null) {
			// 不在世界内：退出存档/回到标题时清掉残留会话，避免重进后异常
			mgr.reset();
		} else {
			mgr.tick();
		}
	}
}
