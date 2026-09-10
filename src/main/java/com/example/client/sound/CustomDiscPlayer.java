package com.example.client.sound;

import com.example.TemplateMod;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 外部 OGG 播放载体（1.16.4 版）。
 *
 * 无 fabric-sound API，改为：声音实例指向占位 sound id {@code record_jukebox:custom}
 * （sounds.json 里配了 stream:true 的空占位），引擎会走 {@code SoundBufferLibrary#getStream}
 * 加载它；我们的 Mixin 拦截该 id，改为从磁盘文件返回 {@code OggAudioStream}。
 *
 * 实例做成相对听者、正中、无衰减的全局声（RECORDS 类别），与全局声 getter 一致。
 */
public final class CustomDiscPlayer {

	private CustomDiscPlayer() {
	}

	/** 占位声音 id（sounds.json 中注册为 stream）。 */
	public static final ResourceLocation CUSTOM_SOUND = new ResourceLocation(TemplateMod.MOD_ID, "custom");

	/** 当前要播放的磁盘文件（由播放管理器设置，同一时刻一首）。 */
	private static volatile Path currentFile;

	public static Path currentFile() {
		return currentFile;
	}

	static void setCurrentFile(Path file) {
		currentFile = file;
	}

	/**
	 * 若文件存在，返回一个播放该 OGG 的全局化声音实例；否则返回 null。
	 * 1.16.4 的 SimpleSoundInstance 构造函数无需 Random。
	 *
	 * <p>音量直接取 volumeScale(0~1)，不再乘 4.0：旧版引擎
	 * {@code calculateVolume = clamp(vol * categoryVol, 0, 1)}，若用 4.0 基准会饱和，
	 * 既让设置里的音量条失去线性，也让 {@code [xx%]} 标签在高百分比时失效。
	 */
	public static SimpleSoundInstance playExternal(Path file, float volumeScale) {
		if (file == null || !Files.isRegularFile(file)) {
			return null;
		}
		setCurrentFile(file);
		// 参数: location, source, volume, pitch, looping, delay, attenuation, x, y, z, relative
		return new SimpleSoundInstance(CUSTOM_SOUND, SoundSource.RECORDS,
				Math.max(0.001f, volumeScale), 1.0f,
				false, 0, SoundInstance.Attenuation.NONE,
				0.0, 0.0, 0.0, true);
	}

	static void clearCurrent() {
		currentFile = null;
	}
}
