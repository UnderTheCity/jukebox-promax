package com.example.mixin.client;

import com.example.client.sound.DiscNameParser;
import com.example.client.sound.DiscPlaybackManager;
import com.example.client.sound.RenamedDiscMusic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;
import java.util.Map;

/**
 * 接管唱片机播放（1.20.1 版）：
 * 原版客户端在收到 1010/1011 事件时会调 {@code LevelRenderer#playStreamingMusic(SoundEvent, BlockPos)}。
 * 这里在该方法开头注入"可选接管"分支，按唱片重命名后的名字 X 决定：
 * <ul>
 *   <li>{@code jukebox/X/} 是含 OGG 的文件夹 → 歌单播放；</li>
 *   <li>{@code jukebox/X.ogg} 存在 → 单曲播放；</li>
 *   <li>否则 → 走原版逻辑。</li>
 * </ul>
 * 停止由 {@code sound == null}（1011）触发，交给播放管理器。
 */
@Mixin(LevelRenderer.class)
public abstract class PlaybackControlMixin {

	@Shadow
	@Final
	private Minecraft minecraft;

	@Shadow
	@Final
	private Map<BlockPos, net.minecraft.client.resources.sounds.SoundInstance> playingRecords;

	@Shadow
	private void notifyNearbyEntities(net.minecraft.world.level.Level world, BlockPos pos, boolean playing) {
		throw new AssertionError("Shadowed");
	}

	@Inject(method = "playStreamingMusic", at = @At("HEAD"), cancellable = true)
	private void recordJukebox$control(SoundEvent sound, BlockPos pos, CallbackInfo ci) {
		DiscPlaybackManager mgr = DiscPlaybackManager.instance();

		// 停止(1011, sound==null)：若在播放自定义内容，交由 manager 处理（含取出即停）
		if (sound == null) {
			if (mgr.isActiveAt(pos)) {
				// 由服务端停止信号/轮询决定真正停止；这里清原版残留即可
				// （原版 map 里没有我们的声音，直接放行）
			}
			return; // 走原版清理
		}

		// 开始(1010)
		String rawName = RenamedDiscMusic.customNameAt(pos);
		if (rawName == null) {
			return; // 没改名 → 原版
		}
		DiscNameParser.Parsed parsed = DiscNameParser.parse(rawName);
		if (parsed == null || parsed.baseName().isBlank()) {
			return;
		}

		// 1) 歌单文件夹优先
		Path folder = RenamedDiscMusic.playlistFolder(parsed.baseName());
		if (folder != null) {
			if (mgr.startPlaylist(pos, folder, parsed.shuffle(), parsed.repeat(), parsed.timerTicks(), parsed.volumeScale())) {
				notifyNearbyEntities(this.minecraft.level, pos, true);
				ci.cancel();
			}
			return;
		}
		// 2) 单曲
		Path file = RenamedDiscMusic.singleFile(parsed.baseName());
		if (file != null) {
			if (mgr.startSingle(pos, file, parsed.volumeScale())) {
				notifyNearbyEntities(this.minecraft.level, pos, true);
				ci.cancel();
			}
			return;
		}
		// 3) 都没有 → 原版
	}
}
