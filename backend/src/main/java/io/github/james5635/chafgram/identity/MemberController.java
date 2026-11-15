package io.github.james5635.chafgram.identity;

import io.github.james5635.chafgram.identity.dto.MemberResponse;
import io.github.james5635.chafgram.identity.dto.UpdateRoleRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Member directory and administration. Any authenticated member can read the
 * member list and profiles; role changes and status actions require admin.
 */
@RestController
@RequestMapping("/api/v1/members")
public class MemberController {

	private final MemberService memberService;

	public MemberController(MemberService memberService) {
		this.memberService = memberService;
	}

	@GetMapping("/me")
	public MemberResponse me() {
		return memberService.currentMemberResponse(currentUsername());
	}

	@GetMapping
	public List<MemberResponse> list() {
		return memberService.listMembers();
	}

	@GetMapping("/{id}")
	public MemberResponse get(@PathVariable Long id) {
		return memberService.getMember(id);
	}

	@PatchMapping("/{id}/role")
	@PreAuthorize("hasRole('ADMIN')")
	public MemberResponse updateRole(@PathVariable Long id, @RequestBody UpdateRoleRequest request) {
		Role newRole = Validation.parseRole(request.role());
		return memberService.updateRole(currentUsername(), id, newRole);
	}

	@PostMapping("/{id}/suspend")
	@PreAuthorize("hasRole('ADMIN')")
	public MemberResponse suspend(@PathVariable Long id) {
		return memberService.setStatus(currentUsername(), id, UserStatus.SUSPENDED);
	}

	@PostMapping("/{id}/deactivate")
	@PreAuthorize("hasRole('ADMIN')")
	public MemberResponse deactivate(@PathVariable Long id) {
		return memberService.setStatus(currentUsername(), id, UserStatus.DEACTIVATED);
	}

	@PostMapping("/{id}/reactivate")
	@PreAuthorize("hasRole('ADMIN')")
	public MemberResponse reactivate(@PathVariable Long id) {
		return memberService.setStatus(currentUsername(), id, UserStatus.ACTIVE);
	}

	private String currentUsername() {
		return SecurityContextHolder.getContext().getAuthentication().getName();
	}
}
