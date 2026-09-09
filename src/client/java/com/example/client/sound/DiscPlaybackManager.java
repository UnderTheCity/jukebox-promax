package com.example.client.sound;

import com.example.TemplateMod;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.stream.Stream;

/**
 * 统一的非原版唱片播放管理器（全局单实例）。
 *
 * 特性：
 * <ul>
 *   <li>同一时间只允许一个"播放会话"（单曲或歌单）；新的播放顶掉旧的（多机 = 最后触发的生效）。</li>
 *   <li>歌单支持 顺序/随机 × 单次/循环（默认顺序+循环）。</li>
 *   <li>支持定时（到点播完当前曲后停止）与整体音量缩放 {@code [xx%]}。</li>
 *   <li>唱片被取出/方块被拆（服务端即时信号）→ 立即停止。</li>
 *   <li>"正在播放"标题显示实际曲目名。</li>
 * </ul>
 */
public final class DiscPlaybackManager {

	private static DiscPlaybackManager INSTANCE;

	/** 模式 */
	public enum Mode { SEQUENTIAL, SHUFFLE }

	private final Minecraft minecraft;
	private final Random random = new Random();

	// 当前激活会话
	private BlockPos activePos;
	private boolean playlist;              // true=歌单, false=单曲
	private List<Path> tracks = new ArrayList<>();
	private int index;
	private boolean shuffle;               // 随机模式
	private boolean repeat = true;         // 是否循环（false = 单次）
	private boolean timerEnabled;
	private long timerTicksRemaining;      // 剩余 tick
	private float volumeScale = 1.0f;      // 整体音量倍率（0~1，来自 [xx%]）

	// 红石开关：充能(信号>0)暂停，失能(0)恢复（不参与音量）
	private boolean pausedByRedstone;
	private int redstonePollTicks;

	private SimpleSoundInstance current;
	private boolean firstStarted;          // 当前曲是否已真正开始过（避免启动异步误判切曲）

	private DiscPlaybackManager(Minecraft minecraft) {
		this.minecraft = minecraft;
	}

	public static DiscPlaybackManager instance() {
		if (INSTANCE == null) {
			INSTANCE = new DiscPlaybackManager(Minecraft.getInstance());
			ClientTickEvents.END_CLIENT_TICK.register(INSTANCE::tick);
		}
		return INSTANCE;
	}

	/** 是否正为某位置激活播放会话。 */
	public boolean isActiveAt(BlockPos pos) {
		return activePos != null && activePos.equals(pos) && current != null;
	}

	/** 停止指定位置的会话（唱片取出/被顶替）。 */
	public void stop(BlockPos pos) {
		if (isActiveAt(pos)) {
			TemplateMod.LOGGER.info("[jukebox-mgr] 停止会话 at {}", pos);
			clear();
		}
	}

	/** 进入新世界/断线时重置：清空播放会话与跨会话残留缓存。 */
	public void reset() {
		clear();
		DiscStopCache.clear();
		DiscNameCache.clear();
	}

	/**
	 * 开始播放单曲文件。
	 * @param volumeScale 整体音量倍率 0~1（1=原声）
	 */
	public boolean startSingle(BlockPos pos, Path file, float volumeScale) {
		if (file == null || !Files.isRegularFile(file)) {
			return false;
		}
		clear();
		DiscStopCache.clear(); // 清除可能残留的"已取出"标记，防止新播放被立即误停
		this.activePos = pos;
		this.playlist = false;
		this.tracks = new ArrayList<>();
		this.tracks.add(file);
		this.index = 0;
		this.shuffle = false;
		this.repeat = false;   // 单曲播完即停
		this.timerEnabled = false;
		this.volumeScale = clampVolume(volumeScale);
		playCurrent();
		return true;
	}

	/**
	 * 开始播放歌单文件夹。
	 * @param shuffle  随机模式
	 * @param repeat   是否循环（false = 单次）
	 * @param timerTicks 定时 tick（0 = 不限时）
	 * @param volumeScale 整体音量倍率 0~1（1=原声）
	 */
	public boolean startPlaylist(BlockPos pos, Path folder, boolean shuffle, boolean repeat, long timerTicks, float volumeScale) {
		List<Path> files = listOggs(folder);
		if (files.isEmpty()) {
			return false;
		}
		clear();
		DiscStopCache.clear(); // 清除可能残留的"已取出"标记，防止新播放被立即误停
		this.activePos = pos;
		this.playlist = true;
		this.tracks = files;
		this.index = 0;
		this.shuffle = shuffle;
		this.repeat = repeat;
		this.timerEnabled = timerTicks > 0;
		this.timerTicksRemaining = timerTicks;
		this.volumeScale = clampVolume(volumeScale);
		if (shuffle) {
			Collections.shuffle(this.tracks, this.random);
		}
		TemplateMod.LOGGER.info("[jukebox-mgr] 开始歌单 {} ({} 首, shuffle={}, repeat={}, timer={}, vol={}) at {}",
				folder, files.size(), shuffle, repeat, timerTicks, this.volumeScale, pos);
		playCurrent();
		return true;
	}

	private static float clampVolume(float v) {
		if (Float.isNaN(v)) {
			return 1.0f;
		}
		if (v <= 0f) {
			return 0.001f; // 允许接近静音但不完全 0，避免引擎跳过
		}
		return Math.min(v, 1.0f);
	}

	/**
	 * 红石开关：充能(有信号)暂停，失能恢复。只作暂停开关，不影响音量（音量由 [xx%] 控制）。
	 */
	private void updateRedstoneSwitch(BlockPos pos, boolean powered) {
		if (!isActiveAt(pos)) {
			return;
		}
		if (powered && !pausedByRedstone) {
			pausedByRedstone = true;
			TemplateMod.LOGGER.info("[jukebox-mgr] 红石充能 → 暂停 at {}", pos);
			if (current != null) {
				minecraft.getSoundManager().stop(current);
			}
		} else if (!powered && pausedByRedstone) {
			pausedByRedstone = false;
			TemplateMod.LOGGER.info("[jukebox-mgr] 红石失能 → 恢复 at {}", pos);
			// 恢复：从当前曲开头重播（声音流无法续播）
			if (current != null) {
				minecraft.getSoundManager().stop(current);
				current = null;
			}
			playCurrent();
		}
	}

	private void tick(Minecraft client) {
		if (current == null) {
			return;
		}
		if (activePos != null) {
			// 唱片被取出/移除：服务端已精确标记（单机同进程），每 tick 消费，命中立即停止
			if (DiscStopCache.consume(activePos)) {
				TemplateMod.LOGGER.info("[jukebox-mgr] 唱片被取出/方块被拆，立即停止 at {}", activePos);
				stop(activePos);
				return;
			}
			// 红石开关：充能(>0)暂停、失能(0)恢复。降频每 5 tick 检测。
			if (++redstonePollTicks >= 5) {
				redstonePollTicks = 0;
				if (client.level != null) {
					boolean powered = client.level.getBestNeighborSignal(activePos) > 0;
					updateRedstoneSwitch(activePos, powered);
				}
			}
			// 红石暂停：不推进任何逻辑（定时也不计），等待失能恢复
			if (pausedByRedstone) {
				return;
			}
		}
		SoundManager sm = minecraft.getSoundManager();
		// 定时：每 tick 递减，到 0 标记"曲终即停"
		boolean stopAfterCurrent = false;
		if (timerEnabled) {
			if (timerTicksRemaining > 0) {
				timerTicksRemaining--;
			}
			if (timerTicksRemaining <= 0) {
				stopAfterCurrent = true;
			}
		}
		if (current != null && sm.isActive(current)) {
			firstStarted = true;
			return; // 当前曲仍在播
		}
		// 当前曲已结束
		if (!firstStarted) {
			// 可能仍在异步启动：给一帧缓冲，避免秒切
			firstStarted = true;
			return;
		}
		// 定时到点且当前已播完 → 结束
		if (stopAfterCurrent) {
			TemplateMod.LOGGER.info("[jukebox-mgr] 定时结束 at {}", activePos);
			clear();
			return;
		}
		// 切下一首
		boolean hasNext = advance();
		if (!hasNext) {
			TemplateMod.LOGGER.info("[jukebox-mgr] 播放结束（无下一首） at {}", activePos);
			clear();
		}
	}

	/** 切到下一首；返回是否有下一首可播。 */
	private boolean advance() {
		if (tracks.isEmpty()) {
			return false;
		}
		if (playlist) {
			if (repeat) {
				if (shuffle) {
					index = random.nextInt(tracks.size());
				} else {
					index = (index + 1) % tracks.size();
				}
			} else {
				// 单次
				if (index + 1 >= tracks.size()) {
					return false;
				}
				index++;
			}
		} else {
			// 单曲没有下一首
			return false;
		}
		playCurrent();
		return true;
	}

	private void playCurrent() {
		if (tracks.isEmpty() || index < 0 || index >= tracks.size()) {
			return;
		}
		Path file = tracks.get(index);
		SimpleSoundInstance inst = CustomDiscPlayer.playExternal(file, volumeScale);
		if (inst == null) {
			return;
		}
		current = inst;
		firstStarted = false;
		minecraft.getSoundManager().play(inst);
		// 在"正在播放"标题显示实际曲目名（而不是原版唱片名）
		showNowPlaying(file);
		TemplateMod.LOGGER.info("[jukebox-mgr] 播放 {}/{} : {} (vol x{})",
				index + 1, tracks.size(), file.getFileName(), volumeScale);
	}

	/** 显示"正在播放: <文件名去扩展名>"。 */
	private void showNowPlaying(Path file) {
		try {
			String name = file.getFileName().toString();
			if (name.toLowerCase(Locale.ROOT).endsWith(".ogg")) {
				name = name.substring(0, name.length() - 4);
			}
			minecraft.gui.setNowPlaying(Component.literal(name));
		} catch (Throwable t) {
			// 忽略：仅显示用途，失败不影响播放
		}
	}

	private void clear() {
		if (current != null) {
			minecraft.getSoundManager().stop(current);
			current = null;
		}
		activePos = null;
		playlist = false;
		tracks.clear();
		index = 0;
		shuffle = false;
		repeat = true;
		timerEnabled = false;
		timerTicksRemaining = 0;
		volumeScale = 1.0f;
		pausedByRedstone = false;
		redstonePollTicks = 0;
		firstStarted = false;
	}

	/** 列出文件夹内全部 .ogg（字典序）。 */
	public static List<Path> listOggs(Path folderDir) {
		List<Path> files = new ArrayList<>();
		if (folderDir == null || !Files.isDirectory(folderDir)) {
			return files;
		}
		try (Stream<Path> s = Files.list(folderDir)) {
			s.filter(Files::isRegularFile)
					.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".ogg"))
					.sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT)))
					.forEach(files::add);
		} catch (IOException e) {
			TemplateMod.LOGGER.error("[jukebox-mgr] 读取歌单目录失败 {}", folderDir, e);
		}
		return files;
	}
}
