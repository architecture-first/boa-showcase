package com.architecture.first.framework.business.vicinity.bulletinboard;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * An entry for the bulletin board
 */
@AllArgsConstructor
@Data
public class BulletinBoardEntry {
    private String status;
    private String timestamp;
    private String subject;
    private String message;
}
