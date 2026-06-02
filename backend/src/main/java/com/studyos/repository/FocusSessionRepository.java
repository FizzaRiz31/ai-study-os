package com.studyos.repository;

import com.studyos.model.FocusSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

public interface FocusSessionRepository extends JpaRepository<FocusSession, Long> {
    List<FocusSession> findByUserIdAndEndedAtAfter(Long userId, Instant after);
}
