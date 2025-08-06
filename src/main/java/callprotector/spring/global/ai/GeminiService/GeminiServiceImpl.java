package callprotector.spring.global.ai.GeminiService;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.GenerationConfig;
import com.google.cloud.vertexai.api.Part;
import com.google.cloud.vertexai.generativeai.GenerativeModel;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiServiceImpl implements  GeminiService {
	@Value("${gcp.project.id}")
	private String projectId;

	@Value("${gcp.location.id}")
	private String locationId;

	private static final String MODEL_ID = "gemini-2.5-flash-lite";

	private VertexAI vertexAi;
	private GenerativeModel generativeModel;

	@PostConstruct
	public void init() throws IOException {
		String credentialsPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
		log.info("GeminiService 초기화: GOOGLE_APPLICATION_CREDENTIALS 경로 = {}", credentialsPath != null ? credentialsPath : "설정되지 않음");


		vertexAi = new VertexAI(projectId, locationId);
		generativeModel = new GenerativeModel(MODEL_ID, vertexAi);

		log.info("GeminiService 초기화 완료 === Endpoint: projects/{}/locations/{}/publishers/google/models/{}", projectId, locationId, MODEL_ID);

	}

	@PreDestroy
	public void destroy() {
		if (vertexAi != null) {
			try {
				vertexAi.close();
				log.info("VertexAI client 종료");
			} catch (Exception e) {
				log.error("VertexAI client 종료 중 오류", e);
			}
		}
	}

	@Override
	public String summarizeCallScript(String conversationText) {
		String prompt = buildSummaryPrompt(conversationText);

		Content content = Content.newBuilder()
			.setRole("user")
			.addParts(Part.newBuilder().setText(prompt))
			.build();

		// 모델 파라미터 구성
		GenerationConfig generationConfig = GenerationConfig.newBuilder()
			.setTemperature(0.2f)
			.setMaxOutputTokens(4096)
			.setTopP(0.9f)
			.setTopK(40)
			.build();

		try {
			GenerateContentResponse response = generativeModel.generateContent(content, generationConfig);

			if (response.getCandidatesCount() > 0 && response.getCandidates(0).hasContent()) {
				return response.getCandidates(0).getContent().getParts(0).getText();
			}
			return "요약 생성 실패: Gemini API 응답에서 유효한 내용을 찾을 수 없습니다.";

		} catch (Exception e) {
			log.error("Gemini API 호출 중 오류 발생: {}", e.getMessage(), e);
			throw new RuntimeException("Gemini API 호출 중 오류 발생", e);
		}
	}

	private String buildSummaryPrompt(String conversationText) {
		return String.format("""
        당신은 고객 상담 내용을 전문적으로 요약하는 고성능 AI 비서입니다.

        아래는 고객(INBOUND)과 상담원(OUTBOUND)의 실제 대화 내용입니다.

        이 대화에서 다음 사항을 중심으로 정확하고 구체적으로 요약해주세요:
        - 고객이 문의한 핵심 질문 또는 요청 사항
        - 상담원이 제공한 안내, 처리 절차 또는 조치 내용

        응답은 **명사형 종결 어미**로 작성하되, **핵심 내용을 빠짐없이 구체적으로** 기술해주세요.
        단순 요약이 아닌 **실제 대화 흐름에 기반한 명확한 정보 전달**을 목표로 합니다.

        다음 형식에 맞춰 작성해주세요:

        문의사항
        <고객의 요청·질문 내용을 요약한 문장>

        처리 결과
        <상담원이 제공한 정보 또는 조치 내용을 요약한 문장>

        ---

        대화 내용:
        %s
        """, conversationText);
	}

}
