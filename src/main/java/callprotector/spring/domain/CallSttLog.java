package callprotector.spring.domain;

import callprotector.spring.domain.enums.CallTrack;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "call_stt_log")
@Getter
@NoArgsConstructor
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

    @Builder
    public CallSttLog(Long callSessionId, CallTrack track, String script, Boolean isFinal, Boolean isAbuse, String abuseType, Integer abuseCnt ) {
        this.callSessionId = callSessionId;
        this.track = track;
        this.script = script;
        this.isFinal = isFinal;
        this.isAbuse = isAbuse;
        this.abuseType = abuseType;
        this.abuseCnt = abuseCnt;
    }
}
