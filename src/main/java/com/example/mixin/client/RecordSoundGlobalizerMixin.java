package com.example.mixin.client;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让唱片机播放的唱片音乐变成"全局声"：
 * <ul>
 *   <li>{@code isRelative()} → true：声音位置当作相对听者偏移；</li>
 *   <li>{@code getX()/getY()/getZ()} → 0：固定在听者正中；</li>
 *   <li>{@code getAttenuation()} → NONE：不随距离衰减；</li>
 *   <li>{@code getVolume()} → 不超过 1.0（见下）。</li>
 * </ul>
 * 仅对 RECORDS 类别生效。结果：左右耳等响（不受立体音效方向性影响），区块加载内即可听。
 *
 * <p><b>音量线性化</b>：旧版引擎 {@code calculateVolume = clamp(instVolume * categoryVol, 0, 1)}。
 * 原版唱片机声音 volume=4.0，导致"唱片机/音符盒"音量条只有在拖到约 25% 以下时才真正变化
 * （4.0 * 0.25 = 1.0 已饱和）。这里把 RECORDS 声音的实例音量压到 ≤1.0，使音量条全程线性生效，
 * 同时因衰减已是 NONE，压低基准不会损失响度上限。
 */
@Mixin(AbstractSoundInstance.class)
public abstract class RecordSoundGlobalizerMixin {

	@Shadow
	protected float volume;

	@Shadow
	protected net.minecraft.client.resources.sounds.Sound sound;

	private boolean recordJukebox$isRecordMusic() {
		SoundInstance self = (SoundInstance) (Object) this;
		return self.getSource() == SoundSource.RECORDS;
	}

	/** 计算真实 getVolume() 值：{@code volume * sound.getVolume()}。 */
	private float recordJukebox$realVolume() {
		if (this.sound != null) {
			return this.volume * this.sound.getVolume();
		}
		return this.volume;
	}

	@Inject(method = "isRelative", at = @At("HEAD"), cancellable = true)
	private void recordJukebox$forceRelative(CallbackInfoReturnable<Boolean> cir) {
		if (recordJukebox$isRecordMusic()) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "getX", at = @At("HEAD"), cancellable = true)
	private void recordJukebox$centerX(CallbackInfoReturnable<Double> cir) {
		if (recordJukebox$isRecordMusic()) {
			cir.setReturnValue(Double.valueOf(0.0D));
		}
	}

	@Inject(method = "getY", at = @At("HEAD"), cancellable = true)
	private void recordJukebox$centerY(CallbackInfoReturnable<Double> cir) {
		if (recordJukebox$isRecordMusic()) {
			cir.setReturnValue(Double.valueOf(0.0D));
		}
	}

	@Inject(method = "getZ", at = @At("HEAD"), cancellable = true)
	private void recordJukebox$centerZ(CallbackInfoReturnable<Double> cir) {
		if (recordJukebox$isRecordMusic()) {
			cir.setReturnValue(Double.valueOf(0.0D));
		}
	}

	@Inject(method = "getAttenuation", at = @At("HEAD"), cancellable = true)
	private void recordJukebox$noAttenuation(CallbackInfoReturnable<SoundInstance.Attenuation> cir) {
		if (recordJukebox$isRecordMusic()) {
			cir.setReturnValue(SoundInstance.Attenuation.NONE);
		}
	}

	@Inject(method = "getVolume", at = @At("HEAD"), cancellable = true)
	private void recordJukebox$linearVolume(CallbackInfoReturnable<Float> cir) {
		if (recordJukebox$isRecordMusic()) {
			float real = recordJukebox$realVolume();
			if (real > 1.0f) {
				cir.setReturnValue(Float.valueOf(1.0f)); // 消除 4.0 基准造成的音量条饱和
			}
		}
	}
}
