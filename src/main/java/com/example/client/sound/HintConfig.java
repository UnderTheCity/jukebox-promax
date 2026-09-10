package com.example.client.sound;

import com.example.TemplateMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * 简易配置：控制"重命名提示"是否显示。
 * 玩家在聊天框输入 {@code td} 可永久关闭（写入配置文件，重启仍生效）。
 */
public final class HintConfig {

	private static final String FILE = "record-jukebox.properties";
	private static final String KEY_SHOW_HINTS = "show-rename-hints";

	private static boolean showHints = true;
	private static boolean loaded = false;

	private HintConfig() {
	}

	public static boolean showHints() {
		ensureLoaded();
		return showHints;
	}

	public static void setShowHints(boolean value) {
		showHints = value;
		save();
	}

	private static void ensureLoaded() {
		if (loaded) {
			return;
		}
		loaded = true;
		Path file = configFile();
		if (file == null || !Files.isRegularFile(file)) {
			return;
		}
		Properties p = new Properties();
		try (InputStream in = Files.newInputStream(file)) {
			p.load(in);
			String v = p.getProperty(KEY_SHOW_HINTS);
			if (v != null) {
				showHints = Boolean.parseBoolean(v);
			}
		} catch (IOException e) {
			TemplateMod.LOGGER.warn("[jukebox] 读取配置失败", e);
		}
	}

	private static void save() {
		Path file = configFile();
		if (file == null) {
			return;
		}
		try {
			Files.createDirectories(file.getParent());
			Properties p = new Properties();
			p.setProperty(KEY_SHOW_HINTS, Boolean.toString(showHints));
			try (OutputStream out = Files.newOutputStream(file)) {
				p.store(out, "Record Jukebox config");
			}
		} catch (IOException e) {
			TemplateMod.LOGGER.warn("[jukebox] 保存配置失败", e);
		}
	}

	private static Path configFile() {
		try {
			return FabricLoader.getInstance().getConfigDir().resolve(FILE);
		} catch (Throwable t) {
			return null;
		}
	}
}
