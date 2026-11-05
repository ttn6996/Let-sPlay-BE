package com.letsplay.system;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SystemHeartbeatRepository extends JpaRepository<SystemHeartbeat, String> {
    @Modifying
    @Query(value = """
            INSERT INTO system_heartbeat (id, last_ping_at, ping_count)
            VALUES (:source, :pingedAt, 1)
            ON CONFLICT (id) DO UPDATE
                SET last_ping_at = EXCLUDED.last_ping_at,
                    ping_count = system_heartbeat.ping_count + 1
            """, nativeQuery = true)
    int recordPing(@Param("source") String source, @Param("pingedAt") Instant pingedAt);
}
