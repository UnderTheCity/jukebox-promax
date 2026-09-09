package com.example;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateMod implements ModInitializer {
	public static final String MOD_ID = "template-mod";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/**
	 * 占位 SoundEvent：对应 assets/template-mod/sounds.json 里的 custom_file 条目
	 * （指向 fabric-sound-api-v1 的空流事件）。实际播放"存档目录下的外部 OGG"时，
	 * 声音引擎靠该事件完成解析，真正的音频数据由自定义 AudioStream 提供。
	 */
	public static final SoundEvent CUSTOM_FILE_SOUND = Registry.register(
			BuiltInRegistries.SOUND_EVENT,
			Identifier.fromNamespaceAndPath(MOD_ID, "custom_file"),
			SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(MOD_ID, "custom_file"))
	);

	@Override
	public void onInitialize() {
		LOGGER.info("Jukebox Global Sound mod loaded.");
	}
}
