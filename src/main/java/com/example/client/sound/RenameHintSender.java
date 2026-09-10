package com.example.client.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;

/**
 * 游戏内引导提示（1.16.4 版）。
 *
 * 在铁砧给唱片改名成功后，于聊天栏打印一段"简易格式说明"；
 * 提示顶部附带"输入 td 不再显示"提示。玩家发送 td 后写入配置永久关闭。
 */
public final class RenameHintSender {

	private RenameHintSender() {
	}

	/**
	 * 处理玩家输入的 "td" / "/template-mod hints on|off"。返回 true 表示已消费（不再发送）。
	 */
	public static boolean handleTdCommand(String raw) {
		String msg = raw == null ? "" : raw.trim();
		if (msg.equalsIgnoreCase("td")) {
			HintConfig.setShowHints(false);
			Minecraft mc = Minecraft.getInstance();
			if (mc.player != null) {
				mc.player.displayClientMessage(
						new TextComponent("§7[模组] 已关闭重命名提示。输入 §e/template-mod hints on§7 可重新开启。"), false);
			}
			return true;
		}
		if (msg.startsWith("/template-mod hints")) {
			boolean on = msg.contains("on") && !msg.contains("off");
			HintConfig.setShowHints(on);
			Minecraft mc = Minecraft.getInstance();
			if (mc.player != null) {
				mc.player.displayClientMessage(
						new TextComponent("§7[模组] 重命名提示已" + (on ? "开启" : "关闭") + "。"), false);
			}
			return true;
		}
		return false;
	}

	/** 检测一个物品是否为"可播放的唱片"。 */
	public static boolean isDisc(ItemStack stack) {
		return stack != null && !stack.isEmpty() && stack.getItem() instanceof RecordItem;
	}

	/** 输出改名成功后的提示。 */
	public static void showRenameHint(String customName) {
		if (!HintConfig.showHints()) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return;
		}
		// 顶部标题
		mc.player.displayClientMessage(new TextComponent(""), false);
		mc.player.displayClientMessage(new TextComponent(
				"§e§l【唱片机自定义音乐】§r §8——把唱片在铁砧改名为 §fxxx §8即可使用"), false);
		mc.player.displayClientMessage(new TextComponent(""), false);

		// 音乐文件放哪
		mc.player.displayClientMessage(new TextComponent(
				"§7§l文件位置§r§7：放进版本文件夹下的 §fjukebox §7文件夹"), false);
		mc.player.displayClientMessage(new TextComponent(
				"§7 · 单曲：§f jukebox/xxx.ogg"), false);
		mc.player.displayClientMessage(new TextComponent(
				"§7 · 歌单：建文件夹 §f jukebox/xxx/ §7 放 .ogg"), false);
		mc.player.displayClientMessage(new TextComponent(""), false);

		// 命名规则
		mc.player.displayClientMessage(new TextComponent(
				"§7§l命名规则§r§7（歌单名 = 唱片改名，空格隔开可组合）："), false);
		mc.player.displayClientMessage(new TextComponent(
				"§7 · §fxxx §8→ §7顺序循环"), false);
		mc.player.displayClientMessage(new TextComponent(
				"§7 · §fxxx 随机§7 / §fxxx 单次 §8→ §7随机 / 播一遍"), false);
		mc.player.displayClientMessage(new TextComponent(
				"§7 · §fxxx 随机 单次 30m §8→ §7随机单次 + 定时30分钟"), false);
		mc.player.displayClientMessage(new TextComponent(
				"§7 · §fxxx [10%] §8→ §7整体音量降到10%"), false);
		mc.player.displayClientMessage(new TextComponent(""), false);

		// 其它
		mc.player.displayClientMessage(new TextComponent(
				"§7 · §f[50%] §8= 音量 §f50% §8｜§f 红石：充能§7暂停§8/§7取消恢复 §8｜§f td§8=关闭本提示"), false);

		// 着重强调 td（醒目、独立成行、排版正确）
		mc.player.displayClientMessage(new TextComponent(""), false);
		mc.player.displayClientMessage(new TextComponent(
				"§c§l提示：在聊天栏输入 §6§ltd §c§l可永久关闭本提示"), false);
		mc.player.displayClientMessage(new TextComponent(
				"§7（需要重新开启：输入 §f/template-mod hints on§7）"), false);
		mc.player.displayClientMessage(new TextComponent(""), false);
	}
}
