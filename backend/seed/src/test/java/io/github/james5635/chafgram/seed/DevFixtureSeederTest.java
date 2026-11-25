package io.github.james5635.chafgram.seed;

import io.github.james5635.chafgram.identity.Invite;
import io.github.james5635.chafgram.identity.Role;
import io.github.james5635.chafgram.identity.User;
import io.github.james5635.chafgram.identity.repository.InviteRepository;
import io.github.james5635.chafgram.identity.utils.Tokens;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DevFixtureSeederTest {

    @Mock
    private InviteRepository inviteRepository;

    @InjectMocks
    private DevFixtureSeeder seeder;

    @Test
    void demoInviteStoresOnlyTheHashOfTheKnownToken() {
        User admin = mock(User.class);
        when(admin.getId()).thenReturn(1L);
        when(inviteRepository.save(any(Invite.class))).thenAnswer(call -> call.getArgument(0));

        seeder.reusableDemoInvite(admin);

        ArgumentCaptor<Invite> saved = ArgumentCaptor.forClass(Invite.class);
        verify(inviteRepository).save(saved.capture());
        Invite invite = saved.getValue();
        assertThat(invite.getTokenHash()).isEqualTo(Tokens.sha256Hex("demo-invite-token"));
        assertThat(invite.getRole()).isEqualTo(Role.MEMBER);
        assertThat(invite.getEmail()).isNull();
        assertThat(invite.getCreatedBy()).isEqualTo(1L);
    }
}
