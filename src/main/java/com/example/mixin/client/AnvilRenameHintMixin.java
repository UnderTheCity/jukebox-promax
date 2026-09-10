package com.example.mixin.client;

import com.example.client.sound.HintConfig;
import com.example.client.sound.RenameHintSender;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 检测"铁砧改名成功"（1.16.4 版）：当输出槽(索引 2)出现一张带自定义名的唱片时，
 * 在聊天栏打印一次模组格式说明（去重：同一名字只提示一次，关屏重置）。
 *
 * 1.16.4 的 AnvilMenu 没有 RESULT_SLOT 常量，结果槽固定在索引 2；
 * 改名信息存在物品 NBT 的 {@code display.Name}。
 */
@Mixin(AnvilScreen.class)
public abstract class AnvilRenameHintMixin {

	private static final int RESULT_SLOT = 2;

	private String recordJukebox$lastHinted = null;

	@Inject(method = "slotChanged", at = @At("HEAD"))
	private void recordJukebox$onSlotChanged(AbstractContainerMenu container, int slot, ItemStack stack, CallbackInfo ci) {
		if (!HintConfig.showHints()) {
			return;
		}
		if (slot != RESULT_SLOT) {
			return;
		}
		if (!RenameHintSender.isDisc(stack)) {
			this.recordJukebox$lastHinted = null; // 输出槽不再是改名唱片，重置
			return;
		}
		String customName = null;
		CompoundTag display = stack.getTagElement("display");
		if (display != null && display.contains("Name")) {
			customName = stack.getHoverName().getString().trim();
		}
		if (customName == null || customName.isEmpty()) {
			this.recordJukebox$lastHinted = null;
			return;
		}
		if (customName.equals(this.recordJukebox$lastHinted)) {
			return; // 已提示过同一名字
		}
		this.recordJukebox$lastHinted = customName;
		RenameHintSender.showRenameHint(customName);
	}
}
