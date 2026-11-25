package io.github.james5635.chafgram.identity;

import io.github.james5635.chafgram.AbstractIntegrationTest;
import io.github.james5635.chafgram.common.AuditLog;
import io.github.james5635.chafgram.common.service.AuditService;
import io.github.james5635.chafgram.identity.dto.CreateInviteResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import io.github.james5635.chafgram.identity.service.InviteService;
import io.github.james5635.chafgram.identity.service.MemberService;

/**
 * Staff-only admin filters: member search (FR-10.1) and audit-log search
 * (FR-10.2).
 */
class AdminFilterIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MemberService memberService;

	@Autowired
	private InviteService inviteService;

	@Autowired
	private AuditService audit;

	User admin;
	User member;
	User moderator;

	@BeforeEach
	void setUp() {
		wipeSocial();
		admin = memberService.bootstrap(
				"alice", "Alice", "alice@example.com", "a-very-strong-password");

		CreateInviteResponse memberInvite = inviteService.create(null, Role.MEMBER, admin);
		member = inviteService.accept(memberInvite.token(),
				"bob", "Bob", "bob@example.com", "bob-very-strong-password");

		CreateInviteResponse moderatorInvite = inviteService.create(null, Role.MODERATOR, admin);
		moderator = inviteService.accept(moderatorInvite.token(),
				"maya", "Maya", "maya@example.com", "maya-very-strong-password");

		CreateInviteResponse taoInvite = inviteService.create(null, Role.MEMBER, admin);
		inviteService.accept(taoInvite.token(),
				"tao", "Tao", "tao@example.com", "tao-very-strong-password");
	}

	// ── members: staff-only (FR-10.1) ───────────────────────────────────────

	@Test
	@WithMockUser(username = "bob", roles = "MEMBER")
	void plainMemberCannotListMembers() throws Exception {
		mockMvc.perform(post("/graphql")
						.with(csrf())
						.contentType("application/json")
						.content("{\"query\":\"{ members { id } }\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.errors[0].extensions.classification", is("FORBIDDEN")));
	}

	@Test
	@WithMockUser(username = "alice", roles = "ADMIN")
	void adminCanListMembers() throws Exception {
		mockMvc.perform(post("/graphql")
						.with(csrf())
						.contentType("application/json")
						.content("{\"query\":\"{ members { username } }\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.members[*].username", hasItem("alice")));
	}

	@Test
	@WithMockUser(username = "maya", roles = "MODERATOR")
	void moderatorCanListMembers() throws Exception {
		mockMvc.perform(post("/graphql")
						.with(csrf())
						.contentType("application/json")
						.content("{\"query\":\"{ members { username } }\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.members.length()", is(4)));
	}

	@Test
	@WithMockUser(username = "alice", roles = "ADMIN")
	void memberQueryMatchesUsernamePrefixCaseInsensitively() throws Exception {
		mockMvc.perform(post("/graphql")
						.with(csrf())
						.contentType("application/json")
						.content("{\"query\":\"{ members(query: \\\"ma\\\") { username } }\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.members.length()", is(1)))
				.andExpect(jsonPath("$.data.members[0].username", is("maya")));
	}

	@Test
	void memberQueryIsAPrefixMatchNotASubstringMatch() {
		var maya = memberService.listMembers("ma", 50);
		assertThat(maya).hasSize(1);
		assertThat(maya.get(0).username()).isEqualTo("maya");

		var tao = memberService.listMembers("ta", 50);
		assertThat(tao).hasSize(1);
		assertThat(tao.get(0).username()).isEqualTo("tao");

		// "lic" is a substring of "alice" but not a prefix of any username
		// or display name
		assertThat(memberService.listMembers("lic", 50)).isEmpty();
	}

	@Test
	void memberQueryAlsoMatchesDisplayNamePrefix() {
		var byDisplayName = memberService.listMembers("Maya", 50);
		assertThat(byDisplayName).hasSize(1);
		assertThat(byDisplayName.get(0).username()).isEqualTo("maya");
	}

	@Test
	void memberQueryWithoutMatchReturnsEmptyList() {
		assertThat(memberService.listMembers("zzz", 50)).isEmpty();
	}

	@Test
	void memberQueryRespectsFirstLimit() {
		assertThat(memberService.listMembers(null, 2)).hasSize(2);
	}

	// ── audit log filters (FR-10.2) ─────────────────────────────────────────

	@Test
	void auditLogFiltersByActorActionAndTargetType() {
		audit.log(admin.getId(), "member.suspend", "member", member.getId(), Map.of());
		audit.log(moderator.getId(), "report.resolve", "report", 42L, Map.of());
		audit.log(admin.getId(), "workspace.update", "workspace", 1L, Map.of());

		assertThat(audit.findByCriteria(admin.getId(), null, null, null, null, 100))
				.hasSize(2)
				.allSatisfy(entry -> assertThat(entry.getActorId()).isEqualTo(admin.getId()));

		assertThat(audit.findByCriteria(null, "report.resolve", null, null, null, 100))
				.hasSize(1)
				.first()
				.satisfies(entry -> assertThat(entry.getAction()).isEqualTo("report.resolve"));

		assertThat(audit.findByCriteria(null, null, "member", null, null, 100))
				.hasSize(1)
				.first()
				.satisfies(entry -> assertThat(entry.getTargetType()).isEqualTo("member"));

		assertThat(audit.findByCriteria(admin.getId(), "workspace.update", "workspace", null, null, 100))
				.hasSize(1);
	}

	@Test
	void auditLogFiltersByTimeWindow() {
		Instant before = Instant.now().minusSeconds(1);
		audit.log(admin.getId(), "first.action", "member", 1L, Map.of());
		Instant between = Instant.now();
		audit.log(admin.getId(), "second.action", "member", 2L, Map.of());
		Instant after = Instant.now().plusSeconds(1);

		List<AuditLog> window = audit.findByCriteria(null, null, null, between, after, 100);
		assertThat(window).hasSize(1);
		assertThat(window.get(0).getAction()).isEqualTo("second.action");

		List<AuditLog> lowerBoundOnly = audit.findByCriteria(null, null, null, before, null, 100);
		assertThat(lowerBoundOnly).hasSize(2);

		List<AuditLog> upperBoundOnly = audit.findByCriteria(null, null, null, null, between, 100);
		assertThat(upperBoundOnly).hasSize(1);
		assertThat(upperBoundOnly.get(0).getAction()).isEqualTo("first.action");
	}

	@Test
	void auditLogWithoutFiltersReturnsEverythingNewestFirst() {
		audit.log(admin.getId(), "first.action", "member", 1L, Map.of());
		audit.log(admin.getId(), "second.action", "member", 2L, Map.of());

		List<AuditLog> all = audit.findByCriteria(null, null, null, null, null, 100);
		assertThat(all).hasSize(2);
		assertThat(all.get(0).getAction()).isEqualTo("second.action");
	}

	@Test
	@WithMockUser(username = "alice", roles = "ADMIN")
	void auditLogGraphQlSupportsFilters() throws Exception {
		audit.log(admin.getId(), "member.suspend", "member", member.getId(), Map.of());
		audit.log(moderator.getId(), "report.resolve", "report", 42L, Map.of());

		mockMvc.perform(post("/graphql")
						.with(csrf())
						.contentType("application/json")
						.content("{\"query\":\"{ auditLog(action: \\\"report.resolve\\\") { action actorId } }\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.auditLog.length()", is(1)))
				.andExpect(jsonPath("$.data.auditLog[0].action", is("report.resolve")));
	}

	@Test
	@WithMockUser(username = "bob", roles = "MEMBER")
	void plainMemberCannotReadAuditLog() throws Exception {
		mockMvc.perform(post("/graphql")
						.with(csrf())
						.contentType("application/json")
						.content("{\"query\":\"{ auditLog { id action } }\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.errors[0].extensions.classification", is("FORBIDDEN")));
	}

	@Test
	@WithMockUser(username = "alice", roles = "ADMIN")
	void auditLogRejectsMalformedActorId() throws Exception {
		mockMvc.perform(post("/graphql")
						.with(csrf())
						.contentType("application/json")
						.content("{\"query\":\"{ auditLog(actorId: \\\"not-a-number\\\") { id } }\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.errors[0].message", containsString("actorId")));
	}

	@Test
	@WithMockUser(username = "alice", roles = "ADMIN")
	void auditLogRejectsMalformedFromInstant() throws Exception {
		mockMvc.perform(post("/graphql")
						.with(csrf())
						.contentType("application/json")
						.content("{\"query\":\"{ auditLog(from: \\\"yesterday\\\") { id } }\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.errors[0].message", containsString("from")));
	}
}
