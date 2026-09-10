package com.example.mixin.client;

import com.example.client.sound.RenameHintSender;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 拦截聊天发送（1.20.1 版）：玩家输入 {@code td} 或 {@code /template-mod hints on|off} 时
 * 不真正发送，而是处理开关（RenameHintSender 内完成）。其余消息照常发送。
 */
@Mixin(ChatScreen.class)
public abstract class ChatTdMixin {

	@Inject(method = "handleChatInput", at = @At("HEAD"), cancellable = true)
	private void recordJukebox$onChat(String text, boolean addToHistory, CallbackInfoReturnable<Boolean> cir) {
		if (RenameHintSender.handleTdCommand(text)) {
			cir.setReturnValue(true); // 已消费，不再发送
		}
	}
}
