package com.example.client.sound;

import com.example.TemplateMod;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 音乐目录与文件匹配辅助。
 *
 * 目录：{@code <gameDirectory>/jukebox}（不存在自动创建）。
 * 匹配名 X 对应：
 * <ul>
 *   <li>{@code jukebox/X/} 目录（含 .ogg）→ 歌单；</li>
 *   <li>{@code jukebox/X.ogg} → 单曲。</li>
 * </ul>
 */
public final class RenamedDiscMusic {

	/** 音乐根目录名（相对 Minecraft gameDirectory）。 */
	public static final String MUSIC_FOLDER = "jukebox";

	private RenamedDiscMusic() {
	}

	/** 取当前自定义名（坐标匹配来自服务端缓存），无则 null。 */
	public static String customNameAt(net.minecraft.core.BlockPos pos) {
		String name = DiscNameCache.take(pos);
		return (name == null || name.isBlank()) ? null : name;
	}

	/** 返回歌单文件夹路径；不存在/不含 ogg 返回 null。 */
	public static Path playlistFolder(String name) {
		if (!isSafeName(name)) {
			return null;
		}
		Path folder = resolveMusicDirectory().resolve(name);
		if (!Files.isDirectory(folder)) {
			return null;
		}
		return DiscPlaybackManager.listOggs(folder).isEmpty() ? null : folder;
	}

	/** 返回单曲文件路径；不存在返回 null。 */
	public static Path singleFile(String name) {
		if (!isSafeName(name)) {
			return null;
		}
		Path file = resolveMusicDirectory().resolve(name + ".ogg");
		if (!Files.isRegularFile(file)) {
			return null;
		}
		return file;
	}

	private static boolean isSafeName(String name) {
		return name != null && !name.isBlank()
				&& !name.contains("/") && !name.contains("\\")
				&& !name.equals(".") && !name.equals("..");
	}

	/** 定位/创建音乐根目录。 */
	public static Path resolveMusicDirectory() {
		Minecraft mc = Minecraft.getInstance();
		Path dir = mc.gameDirectory.toPath().resolve(MUSIC_FOLDER);
		try {
			Files.createDirectories(dir);
			return dir;
		} catch (IOException e) {
			TemplateMod.LOGGER.error("[jukebox] 创建目录失败 {}", dir, e);
			return mc.gameDirectory.toPath();
		}
	}
}
