package com.ims.backend.model;

import com.ims.backend.enums.ComponentType;
import com.ims.backend.enums.Severity;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "signals")
public class Signal {

    @Id
    private String id;

    @Indexed
    private String componentId;
    private ComponentType componentType;

    private String errorCode;
    private String errorMessage;
    private String stackTrace;
    private Severity severity;
    private String sourceHost;
    private String sourceIp;
    private Map<String, Object> metadata;

    @Indexed
    private String workItemId;

    private Instant receivedAt;
    private Instant occurredAt;
}
