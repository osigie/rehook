package com.osigie.rehook.service;

import com.osigie.rehook.domain.DeliveriesCreatedEvent;
import com.osigie.rehook.domain.model.*;
import com.osigie.rehook.repository.DeliveryRepository;
import com.osigie.rehook.repository.EventRepository;
import com.osigie.rehook.service.impl.IngestionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class IngestionServiceTest {

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private IngestionServiceImpl ingestionService;

    @Captor
    private ArgumentCaptor<Event> eventCaptor;

    @Captor
    private ArgumentCaptor<List<Delivery>> deliveriesCaptor;

    @Captor
    private ArgumentCaptor<DeliveriesCreatedEvent> eventPublisherCaptor;

    private Subscription subscription;
    private UUID subscriptionId;
    private String ingestionId;
    private String payload;
    private Map<String, Object> headers;
    private Endpoint activeEndpoint;
    private Endpoint inactiveEndpoint;

    @BeforeEach
    void setUp() throws Exception {
        subscriptionId = UUID.randomUUID();
        ingestionId = UUID.randomUUID().toString();

        subscription = new Subscription("Test Subscription");
        setFieldValue(subscription, "id", subscriptionId);
        setFieldValue(subscription, "tenant", "test-tenant");
        setFieldValue(subscription, "ingestionId", ingestionId);

        payload = "{\"event\": \"test\", \"data\": \"sample data\"}";
        headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "custom-value");

        // Active endpoint
        activeEndpoint = Endpoint.builder()
                .url("https://active-endpoint.com/webhook")
                .isActive(true)
                .build();
        setFieldValue(activeEndpoint, "id", UUID.randomUUID());
        setFieldValue(activeEndpoint, "subscription", subscription);

        // Inactive endpoint
        inactiveEndpoint = Endpoint.builder()
                .url("https://inactive-endpoint.com/webhook")
                .isActive(false)
                .build();
        setFieldValue(inactiveEndpoint, "id", UUID.randomUUID());
        setFieldValue(inactiveEndpoint, "subscription", subscription);

        Set<Endpoint> endpoints = new HashSet<>();
        endpoints.add(activeEndpoint);
        endpoints.add(inactiveEndpoint);
        setFieldValue(subscription, "endpoints", endpoints);
    }

    private void setFieldValue(Object target, String fieldName, Object value) throws Exception {
        Field field = null;
        Class<?> clazz = target.getClass();
        
        // Search in the class hierarchy
        while (clazz != null) {
            try {
                field = clazz.getDeclaredField(fieldName);
                break;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        
        if (field == null) {
            throw new NoSuchFieldException("Field '" + fieldName + "' not found in class hierarchy of " + target.getClass().getName());
        }
        
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void givenValidIngestionRequest_whenIngest_thenCreateEventAndDeliveries() throws Exception {
        // given
        when(subscriptionService.findByIngestionId(ingestionId)).thenReturn(subscription);
        when(eventRepository.findBySubscriptionIdAndIdempotencyKey(any(UUID.class), anyString()))
                .thenReturn(Optional.empty());
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event event = invocation.getArgument(0);
            setFieldValue(event, "id", UUID.randomUUID());
            return event;
        });
        when(deliveryRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<Delivery> deliveries = invocation.getArgument(0);
            for (Delivery d : deliveries) {
                setFieldValue(d, "id", UUID.randomUUID());
            }
            return deliveries;
        });
        doNothing().when(applicationEventPublisher).publishEvent(any(DeliveriesCreatedEvent.class));

        // when
        ingestionService.ingest(ingestionId, payload, headers);

        // then
        verify(subscriptionService, times(1)).findByIngestionId(ingestionId);
        verify(eventRepository, times(1)).findBySubscriptionIdAndIdempotencyKey(eq(subscriptionId), anyString());
        verify(eventRepository, times(1)).save(eventCaptor.capture());
        verify(deliveryRepository, times(1)).saveAll(deliveriesCaptor.capture());
        verify(applicationEventPublisher, times(1)).publishEvent(eventPublisherCaptor.capture());

        // Verify event
        Event savedEvent = eventCaptor.getValue();
        assertNotNull(savedEvent);
        assertEquals(payload, savedEvent.getPayload());
        assertEquals(headers, savedEvent.getHeaders());
        assertNotNull(savedEvent.getIdempotencyKey());
        assertEquals(subscription, savedEvent.getSubscription());
        assertNotNull(savedEvent.getReceivedAt());

        // Verify deliveries - only active endpoint should have a delivery created
        List<Delivery> savedDeliveries = deliveriesCaptor.getValue();
        assertNotNull(savedDeliveries);
        assertEquals(1, savedDeliveries.size());
        assertEquals(activeEndpoint, savedDeliveries.get(0).getEndpoint());
        assertEquals(0, savedDeliveries.get(0).getRetryCount());
        assertEquals(DeliveryStatusEnum.QUEUED, savedDeliveries.get(0).getStatus());

        // Verify event publisher
        DeliveriesCreatedEvent publishedEvent = eventPublisherCaptor.getValue();
        assertNotNull(publishedEvent);
        assertEquals(1, publishedEvent.deliveryIds().size());
    }

    @Test
    void givenDuplicateEvent_whenIngest_thenSkipProcessing() throws Exception {
        // given
        Event existingEvent = Event.builder()
                .payload(payload)
                .subscription(subscription)
                .idempotencyKey("some-key")
                .build();
        setFieldValue(existingEvent, "id", UUID.randomUUID());

        when(subscriptionService.findByIngestionId(ingestionId)).thenReturn(subscription);
        when(eventRepository.findBySubscriptionIdAndIdempotencyKey(any(UUID.class), anyString()))
                .thenReturn(Optional.of(existingEvent));

        // when
        ingestionService.ingest(ingestionId, payload, headers);

        // then
        verify(subscriptionService, times(1)).findByIngestionId(ingestionId);
        verify(eventRepository, times(1)).findBySubscriptionIdAndIdempotencyKey(eq(subscriptionId), anyString());
        verify(eventRepository, never()).save(any());
        verify(deliveryRepository, never()).saveAll(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void givenMultipleActiveEndpoints_whenIngest_thenCreateDeliveriesForAllActive() throws Exception {
        // given
        Endpoint anotherActiveEndpoint = Endpoint.builder()
                .url("https://another-active.com/webhook")
                .isActive(true)
                .build();
        setFieldValue(anotherActiveEndpoint, "id", UUID.randomUUID());
        setFieldValue(anotherActiveEndpoint, "subscription", subscription);

        Set<Endpoint> endpoints = new HashSet<>();
        endpoints.add(activeEndpoint);
        endpoints.add(inactiveEndpoint);
        endpoints.add(anotherActiveEndpoint);
        setFieldValue(subscription, "endpoints", endpoints);

        when(subscriptionService.findByIngestionId(ingestionId)).thenReturn(subscription);
        when(eventRepository.findBySubscriptionIdAndIdempotencyKey(any(UUID.class), anyString()))
                .thenReturn(Optional.empty());
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event event = invocation.getArgument(0);
            setFieldValue(event, "id", UUID.randomUUID());
            return event;
        });
        when(deliveryRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<Delivery> deliveries = invocation.getArgument(0);
            for (Delivery d : deliveries) {
                setFieldValue(d, "id", UUID.randomUUID());
            }
            return deliveries;
        });
        doNothing().when(applicationEventPublisher).publishEvent(any(DeliveriesCreatedEvent.class));

        // when
        ingestionService.ingest(ingestionId, payload, headers);

        // then
        verify(deliveryRepository, times(1)).saveAll(deliveriesCaptor.capture());
        verify(applicationEventPublisher, times(1)).publishEvent(eventPublisherCaptor.capture());

        List<Delivery> savedDeliveries = deliveriesCaptor.getValue();
        assertEquals(2, savedDeliveries.size()); // Only 2 active endpoints

        // All deliveries should be for active endpoints
        savedDeliveries.forEach(delivery -> {
            assertTrue(delivery.getEndpoint().isActive());
            assertEquals(DeliveryStatusEnum.QUEUED, delivery.getStatus());
            assertEquals(0, delivery.getRetryCount());
        });

        DeliveriesCreatedEvent publishedEvent = eventPublisherCaptor.getValue();
        assertEquals(2, publishedEvent.deliveryIds().size());
    }

    @Test
    void givenNoActiveEndpoints_whenIngest_thenCreateNoDeliveries() throws Exception {
        // given
        // Set only inactive endpoint
        Set<Endpoint> endpoints = new HashSet<>();
        endpoints.add(inactiveEndpoint);
        setFieldValue(subscription, "endpoints", endpoints);

        when(subscriptionService.findByIngestionId(ingestionId)).thenReturn(subscription);
        when(eventRepository.findBySubscriptionIdAndIdempotencyKey(any(UUID.class), anyString()))
                .thenReturn(Optional.empty());
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event event = invocation.getArgument(0);
            setFieldValue(event, "id", UUID.randomUUID());
            return event;
        });
        when(deliveryRepository.saveAll(anyList())).thenReturn(Collections.emptyList());
        doNothing().when(applicationEventPublisher).publishEvent(any(DeliveriesCreatedEvent.class));

        // when
        ingestionService.ingest(ingestionId, payload, headers);

        // then
        verify(deliveryRepository, times(1)).saveAll(anyList());
        verify(applicationEventPublisher, times(1)).publishEvent(eventPublisherCaptor.capture());

        // Verify empty deliveries published
        DeliveriesCreatedEvent publishedEvent = eventPublisherCaptor.getValue();
        assertTrue(publishedEvent.deliveryIds().isEmpty());
    }

    @Test
    void givenNoEndpoints_whenIngest_thenCreateNoDeliveries() throws Exception {
        // given
        setFieldValue(subscription, "endpoints", Collections.emptySet());

        when(subscriptionService.findByIngestionId(ingestionId)).thenReturn(subscription);
        when(eventRepository.findBySubscriptionIdAndIdempotencyKey(any(UUID.class), anyString()))
                .thenReturn(Optional.empty());
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event event = invocation.getArgument(0);
            setFieldValue(event, "id", UUID.randomUUID());
            return event;
        });
        when(deliveryRepository.saveAll(anyList())).thenReturn(Collections.emptyList());
        doNothing().when(applicationEventPublisher).publishEvent(any(DeliveriesCreatedEvent.class));

        // when
        ingestionService.ingest(ingestionId, payload, headers);

        // then
        verify(deliveryRepository, times(1)).saveAll(anyList());
        verify(applicationEventPublisher, times(1)).publishEvent(eventPublisherCaptor.capture());

        // Verify empty deliveries published
        DeliveriesCreatedEvent publishedEvent = eventPublisherCaptor.getValue();
        assertTrue(publishedEvent.deliveryIds().isEmpty());
    }

    @Test
    void givenValidEvent_whenIngest_thenGenerateIdempotencyKey() throws Exception {
        // given
        when(subscriptionService.findByIngestionId(ingestionId)).thenReturn(subscription);
        when(eventRepository.findBySubscriptionIdAndIdempotencyKey(any(UUID.class), anyString()))
                .thenReturn(Optional.empty());
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event event = invocation.getArgument(0);
            setFieldValue(event, "id", UUID.randomUUID());
            return event;
        });
        when(deliveryRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<Delivery> deliveries = invocation.getArgument(0);
            for (Delivery d : deliveries) {
                setFieldValue(d, "id", UUID.randomUUID());
            }
            return deliveries;
        });
        doNothing().when(applicationEventPublisher).publishEvent(any(DeliveriesCreatedEvent.class));

        // when
        ingestionService.ingest(ingestionId, payload, headers);

        // then
        verify(eventRepository, times(1)).save(eventCaptor.capture());
        
        // Verify idempotency key is generated and is not null/empty
        String idempotencyKey = eventCaptor.getValue().getIdempotencyKey();
        assertNotNull(idempotencyKey);
        assertFalse(idempotencyKey.isEmpty());
        assertTrue(idempotencyKey.length() > 0);
    }
}
