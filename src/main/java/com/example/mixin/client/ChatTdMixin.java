package com.example.mixin.client;

import com.example.client.sound.RenameHintSender;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 拦截聊天发送（1.16.4 版）：聊天框敲回车走 {@code Screen#sendMessage(String)}，
 * 玩家输入 {@code td} 或 {@code /template-mod hints on|off} 时拦截处理，不再真正发送。
 * 其余消息照常发送。
 */
@Mixin(Screen.class)
public abstract class ChatTdMixin {

	@Inject(method = "sendMessage", at = @At("HEAD"), cancellable = true)
	private void recordJukebox$onSendMessage(String text, CallbackInfo ci) {
		if (RenameHintSender.handleTdCommand(text)) {
			ci.cancel(); // 已消费，不再发送
		}
	}
}
