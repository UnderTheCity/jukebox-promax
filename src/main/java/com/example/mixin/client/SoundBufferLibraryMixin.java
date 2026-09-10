package com.example.mixin.client;

import com.example.client.sound.CustomDiscPlayer;
import com.example.TemplateMod;
import com.mojang.blaze3d.audio.OggAudioStream;
import net.minecraft.Util;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * 外部 OGG 播放（1.16.4 版）：
 *
 * 1.16.4 管线与 1.20.1 不同——SoundEngine 播放流式声音时，传给
 * {@link SoundBufferLibrary#getStream(ResourceLocation, boolean)} 的不是注册事件名
 * {@code record_jukebox:custom}，而是该 Sound 解析后的实际资源路径，且**带 .ogg 后缀**：
 * {@code record_jukebox:sounds/empty.ogg}（见 Sound.getPath()：{ns}:sounds/{file}.ogg）。
 *
 * 因此这里在引擎要流式加载 {@code record_jukebox:sounds/empty.ogg}（占位空音频）时，
 * 若当前有自定义曲目在播，就从磁盘文件返回 {@link OggAudioStream} 作为音频源。
 */
@Mixin(SoundBufferLibrary.class)
public abstract class SoundBufferLibraryMixin {

	/** 占位 Sound 解析后的实际资源路径（getPath() = {ns}:sounds/{file}.ogg）。 */
	private static final ResourceLocation PLACEHOLDER_SOUND_PATH =
			new ResourceLocation(CustomDiscPlayer.CUSTOM_SOUND.getNamespace(), "sounds/empty.ogg");

	@Inject(method = "getStream", at = @At("HEAD"), cancellable = true)
	private void recordJukebox$customStream(ResourceLocation id, boolean loop, CallbackInfoReturnable<CompletableFuture<AudioStream>> cir) {
		// 兼容两种可能传入的 id：注册事件名 record_jukebox:custom 或解析后路径 record_jukebox:sounds/empty.ogg
		if (!CustomDiscPlayer.CUSTOM_SOUND.equals(id) && !PLACEHOLDER_SOUND_PATH.equals(id)) {
			return;
		}
		Path file = CustomDiscPlayer.currentFile();
		if (file == null) {
			return; // 无自定义曲目，走原版（读不到会静默/报错，但不应发生）
		}
		// 与原版一致用背景 IO 执行器，避免公共线程池排队造成额外启动延迟
		cir.setReturnValue(CompletableFuture.supplyAsync(new java.util.function.Supplier<AudioStream>() {
			public AudioStream get() {
				try {
					InputStream in = new BufferedInputStream(Files.newInputStream(file));
					return new OggAudioStream(in);
				} catch (Exception e) {
					TemplateMod.LOGGER.error("无法打开自定义唱片音频 {}", file, e);
					return null;
				}
			}
		}, Util.backgroundExecutor()));
	}
}
