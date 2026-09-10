package com.example;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TemplateModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// 客户端初始化时确保自定义音乐目录存在（<gameDirectory>/jukebox）。
		// 若此时 Minecraft 实例尚未就绪，则由播放匹配逻辑(RenamedDiscMusic)在首次使用时自动创建。
		try {
			Minecraft mc = Minecraft.getInstance();
			if (mc != null && mc.gameDirectory != null) {
				Path dir = mc.gameDirectory.toPath().resolve("jukebox");
				Files.createDirectories(dir);
				TemplateMod.LOGGER.info("[jukebox] 已确保自定义音乐目录存在: {}", dir);
			}
		} catch (Throwable t) {
			TemplateMod.LOGGER.info("[jukebox] 音乐目录延后创建（首次使用时自动创建）");
		}
	}
}
