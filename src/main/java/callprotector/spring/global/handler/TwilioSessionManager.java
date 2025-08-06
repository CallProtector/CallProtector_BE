package callprotector.spring.global.handler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class TwilioSessionManager {

	private final Map<String, TwilioMediaStreamProcessor> callSidToProcessor = new ConcurrentHashMap<>();

	public void registerProcessor(String callSid, TwilioMediaStreamProcessor processor) {
		callSidToProcessor.put(callSid, processor);
	}

	// CallSid로 프로세서를 찾는 메서드
	public TwilioMediaStreamProcessor getProcessorByCallSid(String callSid) {
		return callSidToProcessor.get(callSid);
	}

	public void unregisterProcessor(String callSid) {
		callSidToProcessor.remove(callSid);
	}
}
