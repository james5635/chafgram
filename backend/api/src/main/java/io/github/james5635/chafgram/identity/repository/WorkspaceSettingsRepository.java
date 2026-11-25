package io.github.james5635.chafgram.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import io.github.james5635.chafgram.identity.WorkspaceSettings;

public interface WorkspaceSettingsRepository extends JpaRepository<WorkspaceSettings, Byte> {
}
