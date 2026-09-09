package com.example.mixin.client;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 需求 1：让"唱片机播放的唱片音乐"变成全局声。
 *
 * 原理：只对原版唱片机声音（{@link SoundSource#RECORDS} 类别）做只读 getter 覆盖——
 * 不重定向任何工厂方法，也不触碰 {@code LevelEventHandler}，因此可避开
 * Mixinextras 对 @Redirect 工厂调用的已知 bug。
 *
 * 改造效果（当且仅当声音属于 RECORDS 类别）：
 * <ul>
 *   <li>{@code isRelative()} → true：声音位置被当作相对听者的偏移；</li>
 *   <li>{@code getX()/getY()/getZ()} → 0：偏移为零，声音固定在听者正中；</li>
 *   <li>{@code getAttenuation()} → NONE：不再随距离衰减。</li>
 * </ul>
 * 结果：左右耳音量相等（不受立体音效/HRTF 方向性影响），且只要区块加载即等响可听。
 */
@Mixin(AbstractSoundInstance.class)
public abstract class RecordSoundGlobalizerMixin {

	private boolean templateMod$isRecordMusic() {
		SoundInstance self = (SoundInstance) (Object) this;
		return self.getSource() == SoundSource.RECORDS;
	}

	@Inject(method = "isRelative", at = @At("HEAD"), cancellable = true)
	private void templateMod$forceRelative(CallbackInfoReturnable<Boolean> cir) {
		if (templateMod$isRecordMusic()) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "getX", at = @At("HEAD"), cancellable = true)
	private void templateMod$centerX(CallbackInfoReturnable<Double> cir) {
		if (templateMod$isRecordMusic()) {
			cir.setReturnValue(0.0);
		}
	}

	@Inject(method = "getY", at = @At("HEAD"), cancellable = true)
	private void templateMod$centerY(CallbackInfoReturnable<Double> cir) {
		if (templateMod$isRecordMusic()) {
			cir.setReturnValue(0.0);
		}
	}

	@Inject(method = "getZ", at = @At("HEAD"), cancellable = true)
	private void templateMod$centerZ(CallbackInfoReturnable<Double> cir) {
		if (templateMod$isRecordMusic()) {
			cir.setReturnValue(0.0);
		}
	}

	@Inject(method = "getAttenuation", at = @At("HEAD"), cancellable = true)
	private void templateMod$noAttenuation(CallbackInfoReturnable<SoundInstance.Attenuation> cir) {
		if (templateMod$isRecordMusic()) {
			cir.setReturnValue(SoundInstance.Attenuation.NONE);
		}
	}
}
