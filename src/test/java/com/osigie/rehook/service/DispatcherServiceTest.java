package com.osigie.rehook.service;

import com.osigie.rehook.domain.HttpResponse;
import com.osigie.rehook.domain.model.*;
import com.osigie.rehook.exception.ResourceNotFoundException;
import com.osigie.rehook.repository.DeliveryRepository;
import com.osigie.rehook.service.impl.DispatcherServiceImpl;
import com.osigie.rehook.service.impl.HttpClient.HttpClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DispatcherServiceTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private HttpClientService httpClientService;

    @InjectMocks
    private DispatcherServiceImpl dispatcherService;

    @Captor
    private ArgumentCaptor<Delivery> deliveryCaptor;

    private Delivery delivery;
    private UUID deliveryId;
    private Event event;
    private Endpoint endpoint;
    private Subscription subscription;

    @BeforeEach
    void setUp() throws Exception {
        deliveryId = UUID.randomUUID();

        subscription = new Subscription("Test Subscription");
        setFieldValue(subscription, "id", UUID.randomUUID());
        setFieldValue(subscription, "tenant", "test-tenant");

        endpoint = Endpoint.builder()
                .url("https://example.com/webhook")
                .isActive(true)
                .build();
        setFieldValue(endpoint, "id", UUID.randomUUID());
        setFieldValue(endpoint, "subscription", subscription);

        event = Event.builder()
                .payload("{\"test\": \"data\"}")
                .subscription(subscription)
                .build();
        setFieldValue(event, "id", UUID.randomUUID());

        delivery = Delivery.builder()
                .event(event)
                .endpoint(endpoint)
                .status(DeliveryStatusEnum.QUEUED)
                .retryCount(0)
                .build();
        setFieldValue(delivery, "id", deliveryId);
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
    void givenSuccessfulResponse_whenDispatchDeliveriesAsync_thenSetStatusToSucceeded() {
        // given
        List<UUID> deliveryIds = Collections.singletonList(deliveryId);
        HttpResponse successResponse = new HttpResponse(200, Map.of("Content-Type", "application/json"), Map.of("status", "ok"));

        when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(delivery));
        when(httpClientService.send(any(Delivery.class))).thenReturn(successResponse);
        when(deliveryRepository.save(any(Delivery.class))).thenReturn(delivery);

        // when
        dispatcherService.dispatchDeliveriesAsync(deliveryIds);

        // then - need to wait for async execution
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(deliveryRepository, times(1)).findById(deliveryId);
        verify(httpClientService, times(1)).send(any(Delivery.class));
        verify(deliveryRepository, times(1)).save(deliveryCaptor.capture());

        Delivery savedDelivery = deliveryCaptor.getValue();
        assertEquals(DeliveryStatusEnum.SUCCEEDED, savedDelivery.getStatus());
        assertNull(savedDelivery.getNextRetryAt());
        assertEquals(0, savedDelivery.getRetryCount());
        assertEquals(1, savedDelivery.getDeliveryAttempts().size());
    }

    @Test
    void givenFailedResponseFirstAttempt_whenDispatchDeliveriesAsync_thenSetStatusToRetry() {
        // given
        List<UUID> deliveryIds = Collections.singletonList(deliveryId);
        HttpResponse failResponse = new HttpResponse(500, Map.of(), Map.of("error", "Internal Server Error"));

        when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(delivery));
        when(httpClientService.send(any(Delivery.class))).thenReturn(failResponse);
        when(deliveryRepository.save(any(Delivery.class))).thenReturn(delivery);

        // when
        dispatcherService.dispatchDeliveriesAsync(deliveryIds);

        // then
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(deliveryRepository, times(1)).save(deliveryCaptor.capture());

        Delivery savedDelivery = deliveryCaptor.getValue();
        assertEquals(DeliveryStatusEnum.RETRY, savedDelivery.getStatus());
        assertEquals(1, savedDelivery.getRetryCount());
        assertNotNull(savedDelivery.getNextRetryAt());
    }

    @Test
    void givenMaxRetriesReached_whenDispatchDeliveriesAsync_thenSetStatusToDLQ() throws Exception {
        // given
        setFieldValue(delivery, "retryCount", 3); // Already at max retries
        List<UUID> deliveryIds = Collections.singletonList(deliveryId);
        HttpResponse failResponse = new HttpResponse(500, Map.of(), Map.of("error", "Internal Server Error"));

        when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(delivery));
        when(httpClientService.send(any(Delivery.class))).thenReturn(failResponse);
        when(deliveryRepository.save(any(Delivery.class))).thenReturn(delivery);

        // when
        dispatcherService.dispatchDeliveriesAsync(deliveryIds);

        // then
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(deliveryRepository, times(1)).save(deliveryCaptor.capture());

        Delivery savedDelivery = deliveryCaptor.getValue();
        assertEquals(DeliveryStatusEnum.DLQ, savedDelivery.getStatus());
        assertNotNull(savedDelivery.getNextRetryAt());
    }

    @Test
    void givenRetryCountIncreases_whenDispatchDeliveriesAsync_thenCalculateCorrectBackoff() throws Exception {
        // given
        setFieldValue(delivery, "retryCount", 1); // Second attempt (0-indexed)
        List<UUID> deliveryIds = Collections.singletonList(deliveryId);
        HttpResponse failResponse = new HttpResponse(503, Map.of(), Map.of("error", "Service Unavailable"));
        OffsetDateTime beforeDispatch = OffsetDateTime.now();

        when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(delivery));
        when(httpClientService.send(any(Delivery.class))).thenReturn(failResponse);
        when(deliveryRepository.save(any(Delivery.class))).thenReturn(delivery);

        // when
        dispatcherService.dispatchDeliveriesAsync(deliveryIds);

        // then
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(deliveryRepository, times(1)).save(deliveryCaptor.capture());

        Delivery savedDelivery = deliveryCaptor.getValue();
        assertEquals(2, savedDelivery.getRetryCount()); // Incremented from 1 to 2
        assertNotNull(savedDelivery.getNextRetryAt());
        // Backoff: calculateNextRetry is called with retryCount=1: 60 * 2^1 = 120 seconds
        OffsetDateTime expectedMinNextRetry = beforeDispatch.plusSeconds(120);
        assertTrue(savedDelivery.getNextRetryAt().isAfter(expectedMinNextRetry.minusSeconds(5)) ||
                   savedDelivery.getNextRetryAt().equals(expectedMinNextRetry));
    }

    @Test
    void givenNonExistentDelivery_whenDispatchDeliveriesAsync_thenLogErrorAndContinue() {
        // given
        UUID nonExistentId = UUID.randomUUID();
        List<UUID> deliveryIds = Arrays.asList(deliveryId, nonExistentId);
        HttpResponse successResponse = new HttpResponse(200, Map.of(), Map.of());

        when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(delivery));
        when(deliveryRepository.findById(nonExistentId)).thenReturn(Optional.empty());
        when(httpClientService.send(any(Delivery.class))).thenReturn(successResponse);
        when(deliveryRepository.save(any(Delivery.class))).thenReturn(delivery);

        // when
        dispatcherService.dispatchDeliveriesAsync(deliveryIds);

        // then
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(deliveryRepository, times(1)).findById(deliveryId);
        verify(deliveryRepository, times(1)).findById(nonExistentId);
        verify(httpClientService, times(1)).send(any(Delivery.class));
        verify(deliveryRepository, times(1)).save(any(Delivery.class));
    }

    @Test
    void givenRetryDeliveries_whenFindRetries_thenReturnRetryableDeliveryIds() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        List<UUID> retryIds = Arrays.asList(UUID.randomUUID(), UUID.randomUUID());
        Page<UUID> expectedPage = new PageImpl<>(retryIds, pageable, retryIds.size());

        when(deliveryRepository.findAllByStatusAndNextRetryAtBefore(
                eq(DeliveryStatusEnum.RETRY), any(OffsetDateTime.class), eq(pageable)))
                .thenReturn(expectedPage);

        // when
        Page<UUID> result = dispatcherService.findRetries(pageable);

        // then
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(retryIds, result.getContent());
        verify(deliveryRepository, times(1))
                .findAllByStatusAndNextRetryAtBefore(eq(DeliveryStatusEnum.RETRY), any(OffsetDateTime.class), eq(pageable));
    }

    @Test
    void givenNoRetryDeliveries_whenFindRetries_thenReturnEmptyPage() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<UUID> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(deliveryRepository.findAllByStatusAndNextRetryAtBefore(
                eq(DeliveryStatusEnum.RETRY), any(OffsetDateTime.class), eq(pageable)))
                .thenReturn(emptyPage);

        // when
        Page<UUID> result = dispatcherService.findRetries(pageable);

        // then
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void givenDateRangeAndStatus_whenListDeliveries_thenReturnFilteredDeliveries() throws Exception {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        OffsetDateTime fromDate = OffsetDateTime.now().minusDays(7);
        OffsetDateTime toDate = OffsetDateTime.now();
        DeliveryStatusEnum status = DeliveryStatusEnum.SUCCEEDED;

        Delivery delivery1 = Delivery.builder()
                .status(DeliveryStatusEnum.SUCCEEDED)
                .retryCount(0)
                .build();
        setFieldValue(delivery1, "id", UUID.randomUUID());

        Page<Delivery> expectedPage = new PageImpl<>(Collections.singletonList(delivery1));

        when(deliveryRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(expectedPage);

        // when
        Page<Delivery> result = dispatcherService.listDeliveries(pageable, fromDate, toDate, status);

        // then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(deliveryRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void givenNullDates_whenListDeliveries_thenReturnDeliveries() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        DeliveryStatusEnum status = DeliveryStatusEnum.QUEUED;

        Page<Delivery> expectedPage = new PageImpl<>(Collections.singletonList(delivery));

        when(deliveryRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(expectedPage);

        // when
        Page<Delivery> result = dispatcherService.listDeliveries(pageable, null, null, status);

        // then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void givenExistingDelivery_whenGetDelivery_thenReturnDelivery() {
        // given
        when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(delivery));

        // when
        Delivery result = dispatcherService.getDelivery(deliveryId);

        // then
        assertNotNull(result);
        assertEquals(deliveryId, result.getId());
        verify(deliveryRepository, times(1)).findById(deliveryId);
    }

    @Test
    void givenNonExistentDelivery_whenGetDelivery_thenThrowResourceNotFoundException() {
        // given
        UUID nonExistentId = UUID.randomUUID();
        when(deliveryRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // when & then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            dispatcherService.getDelivery(nonExistentId);
        });
        assertEquals("Delivery with id " + nonExistentId + " not found", exception.getMessage());
        verify(deliveryRepository, times(1)).findById(nonExistentId);
    }

    @Test
    void givenMultipleDeliveryIds_whenDispatchDeliveriesAsync_thenProcessAllInParallel() throws Exception {
        // given
        UUID deliveryId2 = UUID.randomUUID();
        Delivery delivery2 = Delivery.builder()
                .event(event)
                .endpoint(endpoint)
                .status(DeliveryStatusEnum.QUEUED)
                .retryCount(0)
                .build();
        setFieldValue(delivery2, "id", deliveryId2);

        List<UUID> deliveryIds = Arrays.asList(deliveryId, deliveryId2);
        HttpResponse successResponse = new HttpResponse(200, Map.of(), Map.of());

        when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(delivery));
        when(deliveryRepository.findById(deliveryId2)).thenReturn(Optional.of(delivery2));
        when(httpClientService.send(any(Delivery.class))).thenReturn(successResponse);
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        dispatcherService.dispatchDeliveriesAsync(deliveryIds);

        // then
        try {
            Thread.sleep(1000); // Give more time for parallel execution
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(deliveryRepository, times(1)).findById(deliveryId);
        verify(deliveryRepository, times(1)).findById(deliveryId2);
        verify(httpClientService, times(2)).send(any(Delivery.class));
        verify(deliveryRepository, times(2)).save(any(Delivery.class));
    }

    @Test
    void given400ErrorResponse_whenDispatchDeliveriesAsync_thenSetStatusToRetry() {
        // given
        List<UUID> deliveryIds = Collections.singletonList(deliveryId);
        HttpResponse clientErrorResponse = new HttpResponse(400, Map.of(), Map.of("error", "Bad Request"));

        when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(delivery));
        when(httpClientService.send(any(Delivery.class))).thenReturn(clientErrorResponse);
        when(deliveryRepository.save(any(Delivery.class))).thenReturn(delivery);

        // when
        dispatcherService.dispatchDeliveriesAsync(deliveryIds);

        // then
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(deliveryRepository, times(1)).save(deliveryCaptor.capture());

        Delivery savedDelivery = deliveryCaptor.getValue();
        // Even 4xx errors should trigger retry logic
        assertEquals(DeliveryStatusEnum.RETRY, savedDelivery.getStatus());
    }

    @Test
    void givenDeliveryAttempt_whenDispatchDeliveriesAsync_thenCaptureResponseDetails() {
        // given
        List<UUID> deliveryIds = Collections.singletonList(deliveryId);
        Map<String, String> responseHeaders = Map.of("X-Request-ID", "12345", "Content-Type", "application/json");
        Map<String, Object> responseBody = Map.of("status", "error", "message", "Something went wrong");
        HttpResponse errorResponse = new HttpResponse(500, responseHeaders, responseBody);

        when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(delivery));
        when(httpClientService.send(any(Delivery.class))).thenReturn(errorResponse);
        when(deliveryRepository.save(any(Delivery.class))).thenReturn(delivery);

        // when
        dispatcherService.dispatchDeliveriesAsync(deliveryIds);

        // then
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(deliveryRepository, times(1)).save(deliveryCaptor.capture());

        Delivery savedDelivery = deliveryCaptor.getValue();
        assertEquals(1, savedDelivery.getDeliveryAttempts().size());

        DeliveryAttempt attempt = savedDelivery.getDeliveryAttempts().iterator().next();
        assertEquals(500, attempt.getStatusCode());
        assertNotNull(attempt.getExecutedAt());
        assertTrue(attempt.getDuration() >= 0);
        assertEquals(responseBody, attempt.getResponseBody());
        assertEquals(responseHeaders, attempt.getResponseHeaders());
    }
}
