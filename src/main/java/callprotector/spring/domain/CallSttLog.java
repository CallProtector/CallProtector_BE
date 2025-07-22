package callprotector.spring.domain;

import java.time.LocalDateTime;

import callprotector.spring.domain.enums.CallTrack;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "call_stt_log")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallSttLog {
    @Id
    private String id;

    @Field("call_session_id")
    private Long callSessionId;     // RDB CallSession 연동용

    @Field("track")
    private CallTrack track; // inbound, outbound

    @Field("script")
    private String script;

    @Field("is_final")
    private Boolean isFinal;

    @Field("is_abuse")
    private Boolean isAbuse;

    @Field("abuse_type")
    private String abuseType;

    @Field("abuse_cnt")
    private Integer abuseCnt;

    @CreatedDate
    @Field("created_at")
    private LocalDateTime timestamp;
}
