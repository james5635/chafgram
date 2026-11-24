package io.github.james5635.chafgram.messaging.dto;

import java.util.List;

public record CreateGroupRequest(String title, List<Long> memberIds) {
}
