package com.example.client.sound;

import net.minecraft.core.BlockPos;

/**
 * 单机(集成服务端与客户端同 JVM)下，把"服务端读取到的唱片自定义名"暂存，
 * 供客户端在收到唱片开始播放事件时取用。
 *
 * 之所以要绕这一道：客户端侧方块实体不同步唱片物品内容，而直接在渲染线程
 * 读服务端方块实体又存在跨线程并发风险；因此在服务端线程(播放逻辑)读取后缓存。
 */
public final class DiscNameCache {

	private static volatile BlockPos cachedPos = null;
	private static volatile String cachedCustomName = null;

	private DiscNameCache() {
	}

	/** 由服务端播放逻辑写入（服务端线程）。 */
	public static void store(BlockPos pos, String customName) {
		cachedPos = pos;
		cachedCustomName = customName;
	}

	/** 由客户端播放事件读取（渲染线程）；命中同坐标才返回，否则 null。 */
	public static String take(BlockPos pos) {
		if (cachedPos != null && cachedPos.equals(pos)) {
			return cachedCustomName;
		}
		return null;
	}

	public static void clear() {
		cachedPos = null;
		cachedCustomName = null;
	}
}
