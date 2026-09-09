package com.example;

import com.example.client.sound.DiscPlaybackManager;
import com.example.client.sound.RenameHintSender;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TemplateModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// 客户端启动后，确保自定义音乐目录存在（<gameDirectory>/jukebox）
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> ensureMusicFolder(client));

		// 拦截聊天输入 td（关闭/开启提示）
		ClientSendMessageEvents.ALLOW_CHAT.register(message ->
				!RenameHintSender.handleTdCommand(message));
		ClientSendMessageEvents.ALLOW_COMMAND.register(command ->
				!RenameHintSender.handleTdCommand("/" + command));

		// 进入世界 / 断线：重置播放器与跨会话残留缓存，避免"退出再进后自定义音乐失效"
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
				DiscPlaybackManager.instance().reset());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
				DiscPlaybackManager.instance().reset());
	}

	private static void ensureMusicFolder(Minecraft client) {
		Path dir = client.gameDirectory.toPath().resolve("jukebox");
		try {
			Files.createDirectories(dir);
			TemplateMod.LOGGER.info("[jukebox] 已确保自定义音乐目录存在: {}", dir);
		} catch (IOException e) {
			TemplateMod.LOGGER.error("[jukebox] 无法创建自定义音乐目录 {}", dir, e);
		}
	}
}
