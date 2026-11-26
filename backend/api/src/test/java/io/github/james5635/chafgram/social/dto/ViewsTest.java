package io.github.james5635.chafgram.social.dto;

import io.github.james5635.chafgram.identity.Role;
import io.github.james5635.chafgram.identity.User;
import io.github.james5635.chafgram.identity.UserStatus;
import io.github.james5635.chafgram.identity.repository.UserRepository;
import io.github.james5635.chafgram.social.Post;
import io.github.james5635.chafgram.social.PostMedia;
import io.github.james5635.chafgram.social.PostType;
import io.github.james5635.chafgram.social.repository.PostMediaRepository;
import io.github.james5635.chafgram.social.service.CommentService;
import io.github.james5635.chafgram.social.service.FollowService;
import io.github.james5635.chafgram.social.service.PostService;
import io.github.james5635.chafgram.social.service.ReactionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViewsTest {

    @Mock
    private UserRepository users;
    @Mock
    private FollowService follows;
    @Mock
    private CommentService comments;
    @Mock
    private ReactionService reactions;
    @Mock
    private PostService posts;
    @Mock
    private PostMediaRepository postMedia;

    @InjectMocks
    private Views views;

    @Test
    void postViewIncludesAttachedMediaIds() {
        User author = author();
        Post post = mock(Post.class);
        when(post.getId()).thenReturn(10L);
        when(post.getAuthorId()).thenReturn(1L);
        when(post.getType()).thenReturn(PostType.PHOTO);
        when(post.getBody()).thenReturn("Team offsite");
        when(post.getCreatedAt()).thenReturn(Instant.parse("2026-11-21T10:00:00Z"));
        when(users.findById(1L)).thenReturn(Optional.of(author));
        when(reactions.myPostReaction(author, 10L)).thenReturn(Optional.empty());
        when(posts.isSaved(author.getId(), 10L)).thenReturn(false);

        PostMedia attachment = mock(PostMedia.class);
        when(attachment.getMediaId()).thenReturn(41L);
        when(postMedia.findByPostIdOrderByPositionAsc(10L)).thenReturn(List.of(attachment));

        PostDto dto = views.post(post, author);

        assertThat(dto.mediaIds()).containsExactly(41L);
    }

    private User author() {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", 1L);
        user.setUsername("alice");
        user.setDisplayName("Alice");
        user.setEmail("alice@example.com");
        user.setRole(Role.MEMBER);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }
}
