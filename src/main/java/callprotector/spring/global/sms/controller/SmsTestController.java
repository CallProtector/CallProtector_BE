package callprotector.spring.global.sms.controller;

import callprotector.spring.global.sms.service.SmsService;
import com.solapi.sdk.message.dto.response.MultipleDetailMessageSentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/dev/sms")
@RequiredArgsConstructor
@Validated
@Tag(name = "SMS", description = "SMS 발송 테스트 API")
public class SmsTestController {

    private final SmsService smsService;

    @Operation(
            summary = "통화 강제 종료 사유 문자 발송 테스트 API",
            description = "수신번호로 폭언 유형이 포함된 안내 SMS를 발송합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TerminationSmsRequest.class)
                    )
            )
    )

    @PostMapping(value = "/termination", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public MultipleDetailMessageSentResponse sendTermination(@Valid @RequestBody TerminationSmsRequest req) {
        Set<String> labels = new LinkedHashSet<>();
        if (req.getTypes() != null) {
            for (String t : req.getTypes()) {
                if (t == null) continue;
                String trimmed = t.trim();
                if (!trimmed.isEmpty()) labels.add(trimmed);
            }
        }
        return smsService.sendTerminationNotice(req.getTo(), labels);
    }

    @Data
    public static class TerminationSmsRequest {
        @Schema(description = "수신번호(하이픈 X)", example = "01012345678", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(min = 10, max = 20)
        private String to;

        @ArraySchema(schema = @Schema(description = "폭언 유형(욕설, 성희롱, 협박)", example = "욕설"))
        private List<String> types;
    }

}