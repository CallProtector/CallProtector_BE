package callprotector.spring.global.multimodal;

// u-law 데이터를 PCM으로 변환
public class ULawDecoder {
	private static final int[] ULAW_MAP = new int[256];

	static {
		for (int i = 0; i < 256; i++) {
			ULAW_MAP[i] = decode(i);
		}
	}

	private static int decode(int ulaw) {
		ulaw = ~ulaw;
		int sign = (ulaw & 0x80);
		int exponent = (ulaw >> 4) & 0x07;
		int mantissa = ulaw & 0x0F;
		int sample = (mantissa << (exponent + 3)) | (1 << (exponent + 3)) - 1;
		if (exponent > 0) {
			sample += (1 << exponent) - 1;
		}
		if (sign != 0) {
			sample = -sample;
		}
		return sample << 2;
	}

	public static int uLawToPcm(int ulawByte) {
		return ULAW_MAP[ulawByte & 0xFF];
	}

}
