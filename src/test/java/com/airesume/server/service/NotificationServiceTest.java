package com.airesume.server.service;

import com.airesume.server.mapper.UserNotificationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private UserNotificationMapper userNotificationMapper;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() throws Exception {
        notificationService = new NotificationService(userNotificationMapper);
        Field emitterMapField = NotificationService.class.getDeclaredField("emitterMap");
        emitterMapField.setAccessible(true);
        emitterMapField.set(notificationService, new ConcurrentHashMap<>());
    }

    @SuppressWarnings("unchecked")
    private Map<Long, SseEmitter> getEmitterMap() throws Exception {
        Field field = NotificationService.class.getDeclaredField("emitterMap");
        field.setAccessible(true);
        return (Map<Long, SseEmitter>) field.get(notificationService);
    }

    @Test
    void registerEmitterShouldStoreNewEmitter() throws Exception {
        SseEmitter emitter = notificationService.registerEmitter(1L);
        assertNotNull(emitter);
        assertSame(emitter, getEmitterMap().get(1L));
    }

    @Test
    void registerEmitterShouldCompleteOldEmitterOnReRegister() throws Exception {
        SseEmitter firstEmitter = notificationService.registerEmitter(1L);
        assertTrue(getEmitterMap().containsKey(1L));

        notificationService.registerEmitter(1L);

        assertThrows(IllegalStateException.class, () -> firstEmitter.send(SseEmitter.event().name("test")));
    }

    @Test
    void registerEmitterShouldHandleMultipleUsers() throws Exception {
        SseEmitter emitter1 = notificationService.registerEmitter(1L);
        SseEmitter emitter2 = notificationService.registerEmitter(2L);
        SseEmitter emitter3 = notificationService.registerEmitter(3L);

        Map<Long, SseEmitter> map = getEmitterMap();
        assertEquals(3, map.size());
        assertSame(emitter1, map.get(1L));
        assertSame(emitter2, map.get(2L));
        assertSame(emitter3, map.get(3L));
    }

    @Test
    void registerEmitterShouldReplaceAndCompleteOldOnReconnect() throws Exception {
        SseEmitter first = notificationService.registerEmitter(1L);
        SseEmitter second = notificationService.registerEmitter(1L);
        SseEmitter third = notificationService.registerEmitter(1L);

        assertEquals(1, getEmitterMap().size());
        assertSame(third, getEmitterMap().get(1L));

        assertThrows(IllegalStateException.class, () -> first.send(SseEmitter.event().name("test")));
        assertThrows(IllegalStateException.class, () -> second.send(SseEmitter.event().name("test")));
    }

    @Test
    void shutdownShouldClearRegisteredEmitters() throws Exception {
        notificationService.registerEmitter(1L);
        notificationService.registerEmitter(2L);

        notificationService.shutdown();

        assertTrue(getEmitterMap().isEmpty());
    }
}
