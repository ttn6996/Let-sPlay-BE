package com.letsplay.system;

import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemService {
    private final SystemHeartbeatRepository heartbeats;
    private final JdbcTemplate jdbcTemplate;

    public SystemService(SystemHeartbeatRepository heartbeats, JdbcTemplate jdbcTemplate) {
        this.heartbeats = heartbeats;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public PingResult recordPing(PingSource source) {
        heartbeats.recordPing(source.name(), Instant.now());
        SystemHeartbeat state = heartbeats.findById(source.name()).orElseThrow();
        return new PingResult("ok", source.name(), state.getLastPingAt(), state.getPingCount());
    }

    public boolean databaseReady() {
        return Integer.valueOf(1).equals(jdbcTemplate.queryForObject("SELECT 1", Integer.class));
    }

    public enum PingSource { SELF, CRON }
    public record PingResult(String status, String source, Instant lastPingAt, long pingCount) {}
}
