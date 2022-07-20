package com.architecture.first.framework.business.vicinity.messages;

import com.architecture.first.framework.technical.util.SimpleModel;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;

/**
 * The header for a Vicinity message
 */
@Data
@RequiredArgsConstructor
public class VicinityHeader implements Serializable {
    private String to;
    private String from;
    private String subject;
    private String eventType;
    private String token;
    private SimpleModel attributes;
}
