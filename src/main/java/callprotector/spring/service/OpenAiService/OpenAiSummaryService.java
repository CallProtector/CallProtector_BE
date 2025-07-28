package callprotector.spring.service.OpenAiService;

public interface OpenAiSummaryService {
    String summarize(String inboundScript, String outboundScript);
}
