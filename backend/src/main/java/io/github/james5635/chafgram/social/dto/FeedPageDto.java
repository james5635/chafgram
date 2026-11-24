package io.github.james5635.chafgram.social.dto;

import java.util.List;

public record FeedPageDto(List<PostDto> posts, String nextCursor) {
}
