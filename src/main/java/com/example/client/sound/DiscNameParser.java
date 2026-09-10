package com.example.client.sound;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 解析唱片自定义名的"标签式"格式（1.16.4 版，Java 8 兼容，无 record）。
 * <pre>
 *   X                     → 顺序 + 循环
 *   X 顺序                → 顺序 + 循环
 *   X 随机                → 随机 + 循环
 *   X 单次                → 顺序 + 不循环
 *   X 随机 单次           → 随机 + 不循环
 *   X 顺序 1h30m          → 顺序 + 循环 + 定时（1 小时 30 分，到点播完当前曲即止）
 *   X 随机 90s            → 随机 + 循环 + 定时 90 秒
 *   X 随机 单次 5m        → 随机 + 不循环 + 定时 5 分钟
 *   X 单次 1h [10%]       → 歌单/单曲 + 单次 + 定时 1 小时 + 整体音量降到 10%
 * </pre>
 * 音量标签：{@code [数字%]}（0~100），可出现在任意位置，从基础名中剥离。
 * 模式标签（可多个）：{@code 顺序/列表/list/seq}、{@code 随机/乱序/random/shuffle}、
 * {@code 单次/一次/once/single}。定时：{@code 数字[h|m|s]组合}。
 * 解析后 {@code baseName} 用于匹配 jukebox 下的文件夹/单曲文件。
 */
public final class DiscNameParser {

	/** 解析结果（Java 8 无 record，用不可变内部类）。 */
	public static final class Parsed {
		private final String baseName;
		private final boolean shuffle;
		private final boolean repeat;
		private final long timerTicks;
		private final float volumeScale;

		Parsed(String baseName, boolean shuffle, boolean repeat, long timerTicks, float volumeScale) {
			this.baseName = baseName;
			this.shuffle = shuffle;
			this.repeat = repeat;
			this.timerTicks = timerTicks;
			this.volumeScale = volumeScale;
		}

		public String baseName() {
			return baseName;
		}

		public boolean shuffle() {
			return shuffle;
		}

		public boolean repeat() {
			return repeat;
		}

		public boolean singleShot() {
			return !repeat;
		}

		public long timerTicks() {
			return timerTicks;
		}

		public float volumeScale() {
			return volumeScale;
		}
	}

	private DiscNameParser() {
	}

	public static Parsed parse(String rawName) {
		if (rawName == null) {
			return null;
		}
		String[] parts = rawName.trim().split("\\s+");
		if (parts.length == 0 || parts[0].isEmpty()) {
			return null;
		}
		List<String> words = new ArrayList<String>();
		for (String p : parts) {
			words.add(p);
		}

		boolean shuffle = false;
		boolean repeat = true;
		long timerTicks = 0;
		float volumeScale = 1.0f;

		// 1) 先扫描并剥离音量标签 [xx%]
		for (int i = words.size() - 1; i >= 0; i--) {
			float v = parseVolume(words.get(i));
			if (v > 0f) {
				volumeScale = v;
				words.remove(i);
			}
		}

		// 2) 剥离尾部标签（定时 / 模式），直到剩基础名
		boolean changed = true;
		while (changed && words.size() > 1) {
			changed = false;
			String last = words.get(words.size() - 1).toLowerCase(Locale.ROOT);

			// 定时
			long dur = parseDuration(last);
			if (dur > 0 && timerTicks == 0) {
				timerTicks = dur;
				words.remove(words.size() - 1);
				changed = true;
				continue;
			}
			// 模式
			if (last.equals("随机") || last.equals("乱序") || last.equals("random")
					|| last.equals("shuffle") || last.equals("rand")) {
				shuffle = true;
				words.remove(words.size() - 1);
				changed = true;
			} else if (last.equals("单次") || last.equals("一次") || last.equals("once")
					|| last.equals("single") || last.equals("single-shot")) {
				repeat = false;
				words.remove(words.size() - 1);
				changed = true;
			} else if (last.equals("顺序") || last.equals("列表") || last.equals("list")
					|| last.equals("sequence") || last.equals("seq") || last.equals("order")) {
				shuffle = false;
				repeat = true;
				words.remove(words.size() - 1);
				changed = true;
			}
		}

		String baseName = String.join(" ", words).trim();
		if (baseName.isEmpty()) {
			return null;
		}
		return new Parsed(baseName, shuffle, repeat, timerTicks, volumeScale);
	}

	/** 解析音量标签 {@code [xx%]}，返回 0~1；不是合法标签返回 0。 */
	public static float parseVolume(String s) {
		if (s == null) {
			return 0f;
		}
		String t = s.trim();
		if (t.length() < 4 || !t.startsWith("[") || !t.endsWith("%]")) {
			return 0f;
		}
		String inner = t.substring(1, t.length() - 2); // 去掉 [ 和 %]
		if (!inner.matches("^[0-9]{1,3}$")) {
			return 0f;
		}
		int pct;
		try {
			pct = Integer.parseInt(inner);
		} catch (NumberFormatException e) {
			return 0f;
		}
		if (pct < 0 || pct > 100) {
			return 0f;
		}
		return pct / 100.0f;
	}

	/**
	 * 解析时长到游戏 tick。支持 h/m/s 组合：{@code 1h30m}、{@code 90s}、{@code 5h}。
	 * 返回 0 表示无有效时长。
	 */
	public static long parseDuration(String s) {
		if (s == null || s.isEmpty()) {
			return 0;
		}
		String t = s.toLowerCase(Locale.ROOT);
		if (!t.matches("^[0-9]+([hms][0-9]+)*[hms]$")) {
			return 0;
		}
		long totalSeconds = 0;
		StringBuilder num = new StringBuilder();
		boolean any = false;
		for (int i = 0; i < t.length(); i++) {
			char c = t.charAt(i);
			if (Character.isDigit(c)) {
				num.append(c);
			} else {
				if (num.length() == 0) {
					return 0;
				}
				int n = Integer.parseInt(num.toString());
				if (c == 'h') {
					totalSeconds += n * 3600L;
				} else if (c == 'm') {
					totalSeconds += n * 60L;
				} else if (c == 's') {
					totalSeconds += n;
				} else {
					return 0;
				}
				num.setLength(0);
				any = true;
			}
		}
		return any ? totalSeconds * 20L : 0;
	}
}
