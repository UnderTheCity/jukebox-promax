package com.example.client.sound;

import com.example.TemplateMod;
import net.fabricmc.fabric.api.client.sound.v1.FabricSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.JOrbisAudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * 需求 B 的播放载体：
 * 把"存档目录下的外部 OGG 文件"作为自定义音频流交给 Minecraft 声音引擎播放。
 *
 * 原理：fabric-sound-api 允许一个 {@link SoundInstance} 额外实现
 * {@link FabricSoundInstance} 并覆写 {@code getAudioStream}，从而提供来自任意来源
 * （这里是磁盘文件）的 {@link AudioStream}，无需把 OGG 打进资源包。
 *
 * 该声音同样做成"相对听者、正中、无衰减"的全局声，与需求 A 一致（双耳等响、无视距离）。
 */
public final class CustomDiscPlayer {

	private CustomDiscPlayer() {
	}

	/** 基础音量（唱片机原生音量）。 */
	public static final float BASE_VOLUME = 4.0f;

	/**
	 * 若给定文件存在，返回一个播放该 OGG 的全局化声音实例；否则返回 {@code null}。
	 */
	public static SimpleSoundInstance playExternal(Path file) {
		return playExternal(file, 1.0f);
	}

	/**
	 * 若给定文件存在，返回一个播放该 OGG 的全局化声音实例；否则返回 {@code null}。
	 *
	 * @param volumeScale 音量倍率（0~1，1=原声）
	 */
	public static SimpleSoundInstance playExternal(Path file, float volumeScale) {
		if (file == null || !Files.isRegularFile(file)) {
			return null;
		}
		return new FileDiscSoundInstance(TemplateMod.CUSTOM_FILE_SOUND, file, volumeScale);
	}

	/**
	 * 真正的文件声音实例：用一个指向 sounds.json 空事件的占位 SoundEvent 让引擎
	 * 能解析到它，实际音频数据由 {@code getAudioStream} 从磁盘文件提供。
	 */
	static class FileDiscSoundInstance extends SimpleSoundInstance implements FabricSoundInstance {
		private final Path file;

		FileDiscSoundInstance(SoundEvent placeholder, Path file, float volumeScale) {
			super(placeholder, SoundSource.RECORDS,
					BASE_VOLUME * Math.max(0.001f, volumeScale), 1.0f,
					SoundInstance.createUnseededRandom(), 0.0, 0.0, 0.0);
			this.file = file;
			// 需求 A：相对听者、位于正中(双耳等响)、无衰减
			this.relative = true;
			this.attenuation = SoundInstance.Attenuation.NONE;
			this.looping = false;
		}

		@Override
		public CompletableFuture<AudioStream> getAudioStream(
				SoundBufferLibrary loader, Identifier id, boolean repeatInstantly) {
			return CompletableFuture.supplyAsync(() -> {
				try {
					InputStream in = new BufferedInputStream(Files.newInputStream(file));
					return new JOrbisAudioStream(in);
				} catch (IOException e) {
					TemplateMod.LOGGER.error("无法打开自定义唱片音频 {}", file, e);
					return null;
				}
			});
		}
	}
}
