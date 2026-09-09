package com.example.mixin.client;

import com.example.client.sound.DiscNameParser;
import com.example.client.sound.DiscPlaybackManager;
import com.example.client.sound.RenamedDiscMusic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelEventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.JukeboxSong;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;

/**
 * 自定义唱片/歌单的编排 + 红石暂停 + 停止处理。
 *
 * 开始播放(level event 1010 → playJukeboxSong)：
 * 解析自定义名 X → {@link DiscNameParser}；
 * 优先歌单文件夹，其次单曲文件，都没有则走原版。
 *
 * 停止相关(stopJukeboxSong，由 1011 事件/唱片取出触发)：
 * 只释放我们自己的播放会话；原版"按时长自动停"的 1011（唱片仍在机内）被忽略，
 * 好让歌单/长单曲能继续。
 */
@Mixin(LevelEventHandler.class)
public abstract class CustomDiscJukeboxMixin {

	@Shadow
	@Final
	private Minecraft minecraft;

	@Shadow
	@Final
	private ClientLevel level;

	@Shadow
	private void stopJukeboxSong(BlockPos pos) {
		throw new AssertionError("Shadowed");
	}

	@Shadow
	private void notifyNearbyEntities(net.minecraft.world.level.Level world, BlockPos pos, boolean playing) {
		throw new AssertionError("Shadowed");
	}

	@Inject(method = "playJukeboxSong", at = @At("HEAD"), cancellable = true)
	private void templateMod$onPlayJukeboxSong(net.minecraft.core.Holder<JukeboxSong> song, BlockPos pos, CallbackInfo ci) {
		String rawName = RenamedDiscMusic.customNameAt(pos);
		if (rawName == null) {
			return; // 没改名 → 原版
		}
		DiscNameParser.Parsed parsed = DiscNameParser.parse(rawName);
		if (parsed == null || parsed.baseName().isBlank()) {
			return;
		}

		DiscPlaybackManager mgr = DiscPlaybackManager.instance();

		// 1) 歌单文件夹优先
		Path folder = RenamedDiscMusic.playlistFolder(parsed.baseName());
		if (folder != null) {
			this.stopJukeboxSong(pos); // 清原版残留（不影响 manager）
			boolean ok = mgr.startPlaylist(pos, folder,
					parsed.shuffle(), parsed.repeat(), parsed.timerTicks(), parsed.volumeScale());
			if (ok) {
				this.notifyNearbyEntities(this.level, pos, true);
				ci.cancel();
			}
			return;
		}

		// 2) 单曲文件
		Path file = RenamedDiscMusic.singleFile(parsed.baseName());
		if (file != null) {
			this.stopJukeboxSong(pos);
			boolean ok = mgr.startSingle(pos, file, parsed.volumeScale());
			if (ok) {
				this.notifyNearbyEntities(this.level, pos, true);
				ci.cancel();
			}
			return;
		}
		// 3) 都没有 → 原版（不 cancel）
	}

}
