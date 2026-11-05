package com.letsplay.system;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SelfPingScheduler {
    private static final Logger logger = LoggerFactory.getLogger(SelfPingScheduler.class);
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();
    private final SystemPingKey pingKey;
    private final String baseUrl;

    public SelfPingScheduler(
            SystemPingKey pingKey,
            @Value("${RENDER_EXTERNAL_URL:}") String renderExternalUrl,
            @Value("${server.port:8082}") String serverPort) {
        this.pingKey = pingKey;
        this.baseUrl = renderExternalUrl == null || renderExternalUrl.isBlank()
                ? "http://127.0.0.1:" + serverPort
                : renderExternalUrl.replaceAll("/+$", "");
    }

    @Scheduled(fixedDelayString = "${app.self-ping.interval-ms:300000}",
            initialDelayString = "${app.self-ping.interval-ms:300000}")
    public void pingSelf() {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/api/system/self-ping"))
                    .timeout(Duration.ofSeconds(20))
                    .header("X-Internal-Ping-Key", pingKey.value())
                    .GET()
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() / 100 != 2) {
                logger.warn("Self-ping returned HTTP {}", response.statusCode());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            logger.warn("Self-ping was interrupted");
        } catch (Exception exception) {
            logger.warn("Self-ping failed: {}", exception.getClass().getSimpleName());
        }
    }
}
