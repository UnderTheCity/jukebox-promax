package com.example.mixin.client;

import com.example.client.sound.HintConfig;
import com.example.client.sound.RenameHintSender;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 检测"铁砧改名成功"：当输出槽(RESULT_SLOT)出现一张带自定义名的唱片时，
 * 在聊天栏打印一次模组格式说明（去重：同一名字只提示一次，关屏重置）。
 */
@Mixin(AnvilScreen.class)
public abstract class AnvilRenameHintMixin {

	private String templateMod$lastHinted = null;

	@Inject(method = "slotChanged", at = @At("HEAD"))
	private void templateMod$onSlotChanged(AbstractContainerMenu container, int slot, ItemStack stack, CallbackInfo ci) {
		if (!HintConfig.showHints()) {
			return;
		}
		if (slot != AnvilMenu.RESULT_SLOT) {
			return;
		}
		if (!RenameHintSender.isDisc(stack)) {
			this.templateMod$lastHinted = null; // 输出槽不再是改名唱片，重置
			return;
		}
		Component name = stack.get(DataComponents.CUSTOM_NAME);
		if (name == null) {
			this.templateMod$lastHinted = null;
			return;
		}
		String customName = name.getString().trim();
		if (customName.isEmpty() || customName.equals(this.templateMod$lastHinted)) {
			return; // 未改名 或 已提示过同一名字
		}
		this.templateMod$lastHinted = customName;
		RenameHintSender.showRenameHint(customName);
	}
}
