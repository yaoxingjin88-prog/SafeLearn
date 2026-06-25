package com.safelearn.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class AdminInboxStreamService {

    private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L;
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>> subscribers = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String userId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        subscribers.computeIfAbsent(userId, id -> new CopyOnWriteArrayList<>()).add(emitter);

        Runnable cleanup = () -> removeEmitter(userId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(error -> cleanup.run());

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(Map.of("userId", userId), MediaType.APPLICATION_JSON));
        } catch (IOException exception) {
            cleanup.run();
            log.debug("Admin inbox SSE connect failed for {}: {}", userId, exception.getMessage());
        }
        return emitter;
    }

  /** 通知前端刷新收件箱：notification / message / all */
    public void publishUpdate(String kind) {
        Map<String, Object> payload = Map.of(
                "kind", kind == null || kind.isBlank() ? "all" : kind,
                "at", LocalDateTime.now().format(DATE_TIME)
        );
        subscribers.forEach((userId, emitters) -> broadcast(userId, emitters, "inbox", payload));
    }

    @Scheduled(fixedRate = 25_000)
    public void heartbeat() {
        Map<String, Object> payload = Map.of("ok", true);
        subscribers.forEach((userId, emitters) -> broadcast(userId, emitters, "ping", payload));
    }

    private void broadcast(String userId, CopyOnWriteArrayList<SseEmitter> emitters,
                             String eventName, Map<String, Object> payload) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(payload, MediaType.APPLICATION_JSON));
            } catch (Exception exception) {
                removeEmitter(userId, emitter);
                log.debug("Admin inbox SSE send failed for {}: {}", userId, exception.getMessage());
            }
        }
    }

    private void removeEmitter(String userId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = subscribers.get(userId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            subscribers.remove(userId, emitters);
        }
    }
}
