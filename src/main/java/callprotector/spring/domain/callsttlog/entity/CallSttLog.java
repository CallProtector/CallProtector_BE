package callprotector.spring.domain.callsttlog.entity;

import java.time.LocalDateTime;

import callprotector.spring.global.common.enums.CallTrack;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "call_stt_log")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndexes({
    @CompoundIndex(
        name = "idx_call_session_id_is_abuse",
        def = "{'call_session_id': 1, 'is_abuse': 1}"
    ),
    @CompoundIndex(
        name = "idx_call_session_id_is_final_created_at",
        def = "{'call_session_id': 1, 'is_final': 1, 'created_at': 1}"
    )
})
public class CallSttLog {
    @Id
    private String id;

    @Field("call_session_id")
    private Long callSessionId;

    @Field("track")
    private CallTrack track;

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
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
}
