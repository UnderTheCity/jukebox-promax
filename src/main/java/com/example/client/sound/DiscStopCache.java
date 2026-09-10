package com.example.client.sound;

import net.minecraft.core.BlockPos;

/**
 * 单机(集成服务端与客户端同 JVM)下，"唱片已被取出/移除"的即时信号。
 *
 * 服务端在 JukeboxSongPlayer#stop 里判断：若唱片物品已清空（真被取出/方块被拆），
 * 就把位置写入这里；客户端播放管理器每 tick 轮询一次，命中即立刻停止，
 * 从而让"弹出唱片"立即生效，而不必等客户端方块状态同步。
 *
 * 只记录最近一次停止事件（同一时刻只可能有一个活跃会话被取出）。
 */
public final class DiscStopCache {

	private static volatile BlockPos stopPos = null;

	private DiscStopCache() {
	}

	/** 服务端标记某位置的唱片已被取出。 */
	public static void mark(BlockPos pos) {
		stopPos = pos;
	}

	/** 客户端消费：若标记的位置与 pos 匹配则清除并返回 true。 */
	public static boolean consume(BlockPos pos) {
		if (stopPos != null && stopPos.equals(pos)) {
			stopPos = null;
			return true;
		}
		return false;
	}

	/** 清除标记（会话切换/开始新播放时调用）。 */
	public static void clear() {
		stopPos = null;
	}
}
