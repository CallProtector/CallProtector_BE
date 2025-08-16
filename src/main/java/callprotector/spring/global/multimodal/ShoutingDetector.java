package callprotector.spring.global.multimodal;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchProcessor;

import callprotector.spring.global.handler.SttContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@RequiredArgsConstructor
public class ShoutingDetector {
	private final List<Double> basePitches = new ArrayList<>();
	private final List<Double> baseVolumes = new ArrayList<>();

	private double shoutingPitchThreshold;
	private double shoutingVolumeThreshold;

	private final AtomicBoolean isBaselineSet = new AtomicBoolean(false);

	private volatile double accumulatedBaselineDuration = 0;

	private volatile boolean isVoiceDetected = false;

	private AudioDispatcher dispatcher;
	private PitchProcessor pitchProcessor;
	private PipedOutputStream pipedOutputStream;
	private PipedInputStream pipedInputStream;
	private boolean isHighPitchUser;
	private float lastKnownPitch = -1.0f;

	private static final double BASELINE_PERIOD_SECONDS = 1.5;
	private static final double PITCH_INCREASE_FACTOR_HIGH = 1.15;
	private static final double PITCH_INCREASE_FACTOR_LOW = 1.69;
	private static final double PITCH_BOUNDARY = 165.0; // 피치 높낮이 구분 기준
	private static final double DB_BOUNDARY = 20.0; // 데시벨 증가 경계값

	@Setter
    private SttContext sttContext;

	public void initializeTarsosDSP(int sampleRate) throws IOException {
		if (dispatcher == null) {
			// PipedInputStream과 PipedOutputStream을 연결하여 오디오 파이프라인 구축
			pipedOutputStream = new PipedOutputStream();
			pipedInputStream = new PipedInputStream(pipedOutputStream);

			AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sampleRate, 16, 1, 2, sampleRate, false);

			// AudioInputStream 생성
			AudioInputStream audioInputStream = new AudioInputStream(pipedInputStream, format, -1);

			// JVMAudioInputStream 생성
			JVMAudioInputStream audioStream = new JVMAudioInputStream(audioInputStream);

			dispatcher = new AudioDispatcher(audioStream, 1024, 0);
			// 피치 분석 핸들러
			PitchDetectionHandler pitchHandler = (pitchDetectionResult, audioEvent) -> {
				float pitchInHz = pitchDetectionResult.getPitch();
				float probability = pitchDetectionResult.getProbability();

				// 피치값이 0보다 클 때를 음성으로 간주
				log.info("pitchInHz = {}", pitchInHz);

				if (pitchInHz > 80 && probability > 0.7f) {
					if (pitchInHz > 350f) {
						pitchInHz *= 0.5f;
					}
					this.isVoiceDetected = true;
					this.lastKnownPitch = pitchInHz;
				} else {
					this.isVoiceDetected = false;
					this.lastKnownPitch = -1.0f;
				}

				if (!isBaselineSet.get() && this.isVoiceDetected) {
					// 유효한 피치 데이터가 있을 때만 누적 시간과 데이터를 추가
					basePitches.add((double) this.lastKnownPitch);
					accumulatedBaselineDuration += audioEvent.getBufferSize() / audioEvent.getSampleRate();
					log.info("⭐ 기준 피치 수집 중: {}Hz (누적 시간: {}s)", pitchInHz, accumulatedBaselineDuration);

					// 누적 시간이 3초를 초과하면 기준값 설정
					if (accumulatedBaselineDuration >= BASELINE_PERIOD_SECONDS && !isBaselineSet.get()) {
						calculateBaselineAndSetThreshold();
						isBaselineSet.set(true);
						log.info("✅ 기준값 수집 완료.");
					}
				}


			};

			// 세기 분석 핸들러 (직접 RMS를 계산하여 데시벨로 변환)
			AudioProcessor volumeHandler = new AudioProcessor() {
				@Override
				public boolean process(AudioEvent audioEvent) {
					float[] buffer = audioEvent.getFloatBuffer();
					double sum = IntStream.range(0, buffer.length)
						.mapToDouble(i -> buffer[i] * buffer[i])
						.sum();
					double rms = Math.sqrt(sum / buffer.length);
					double currentVolume = rms > 0 ? 20 * Math.log10(rms / 0.001f) : -100.0;

					// 볼륨을 기준으로 음성 감지 여부를 판단
					log.info("currentVolume = {}dB, isVoiceDetected = {}", currentVolume, isVoiceDetected);

					// 기준 데이터 수집 단계: 피치가 감지된 경우에만 볼륨 데이터 수집
					if (!isBaselineSet.get() && isVoiceDetected) {
						if (currentVolume > -100) {
							baseVolumes.add(currentVolume);
							log.info("⭐ 기준 볼륨 수집 중: {}dB", currentVolume);
						}
					}

					// 고함 감지 단계: 기준 설정이 완료되고 피치가 감지된 경우
					if (isBaselineSet.get() && isVoiceDetected) {
						float currentPitch = ShoutingDetector.this.lastKnownPitch;
						if (currentPitch > shoutingPitchThreshold && currentVolume > shoutingVolumeThreshold) {
							log.info("🚨🚨🚨 고함 감지! 현재 피치: {}Hz, 볼륨: {}dB", currentPitch, currentVolume);
							sttContext.triggerBeep(); // 삐처리
						} else {
							log.info("✅ 정상 대화: 현재 피치 {}Hz, 볼륨 {}dB", currentPitch, currentVolume);
						}
					}


					return true;
				}
				@Override
				public void processingFinished() {}
			};

			pitchProcessor = new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.YIN, sampleRate, 1024, pitchHandler);
			dispatcher.addAudioProcessor(pitchProcessor);

			dispatcher.addAudioProcessor(volumeHandler);
			new Thread(dispatcher, "Audio Dispatcher").start();
		}
	}

	public void transferAudio(byte[] audioData) throws IOException {
		if (pipedOutputStream == null) {
			log.info("🗑️초기화 전 오디오 데이터는 버림");
			return;
		}
		pipedOutputStream.write(audioData);
	}

	private void calculateBaselineAndSetThreshold() {
		if (basePitches.isEmpty() || baseVolumes.isEmpty()) {
			shoutingPitchThreshold = 500;
			shoutingVolumeThreshold = 1.0;
			return;
		}

		// 정렬
		Collections.sort(basePitches);
		Collections.sort(baseVolumes);

		// 중앙값 추출
		double medianBasePitch = basePitches.get(basePitches.size() / 2);
		double medianBaseVolume = baseVolumes.get(baseVolumes.size() / 2);

		this.isHighPitchUser = (medianBasePitch > PITCH_BOUNDARY);
		double pitchIncreaseFactor = this.isHighPitchUser ? PITCH_INCREASE_FACTOR_HIGH : PITCH_INCREASE_FACTOR_LOW;

		shoutingPitchThreshold = medianBasePitch * (1 + pitchIncreaseFactor);
		shoutingVolumeThreshold = medianBaseVolume + DB_BOUNDARY;


		log.info("✅ 기준 피치 설정 완료: {}Hz, 고함 임계값: {}Hz", medianBasePitch, shoutingPitchThreshold);
		log.info("✅ 기준 볼륨 설정 완료: {}dB, 고함 임계값: {}dB", medianBaseVolume, shoutingVolumeThreshold);
	}


	public void close() {
		if (dispatcher != null && !dispatcher.isStopped()) {
			dispatcher.stop();
		}
		try {
			if (pipedOutputStream != null) {
				pipedOutputStream.close();
			}
			if (pipedInputStream != null) {
				pipedInputStream.close();
			}
		} catch (IOException e) {
			log.error("Piped 스트림 종료 실패.", e);
		}
	}

	// 바이트 배열 -> Short 배열로 변환
	private short[] convertBytesToShorts(byte[] bytes) {
		// PCM_SIGNED 16-bit은 2바이트 단위이므로, 홀수 길이는 오류
		if (bytes.length % 2 != 0) {
			throw new IllegalArgumentException("Byte array length must be even.");
		}
		short[] shorts = new short[bytes.length / 2];
		// ByteBuffer를 사용하여 바이트 순서를 고려하여 변환
		ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
		return shorts;
	}

}
