package com.osigie.rehook.service;

import com.osigie.rehook.configuration.tenancy.TenantContext;
import com.osigie.rehook.domain.model.AuthType;
import com.osigie.rehook.domain.model.Endpoint;
import com.osigie.rehook.domain.model.EndpointAuth;
import com.osigie.rehook.domain.model.Subscription;
import com.osigie.rehook.exception.ConflictException;
import com.osigie.rehook.exception.ResourceNotFoundException;
import com.osigie.rehook.repository.EndpointRepository;
import com.osigie.rehook.repository.EventRepository;
import com.osigie.rehook.repository.SubscriptionRepository;
import com.osigie.rehook.service.impl.SubscriptionServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private EndpointRepository endpointRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private SubscriptionServiceImpl subscriptionService;

    private Subscription subscription;
    private UUID subscriptionId;
    private UUID endpointId;
    private Endpoint endpoint;
    private EndpointAuth endpointAuth;

    @BeforeEach
    void setUp() throws Exception {
        subscriptionId = UUID.randomUUID();
        endpointId = UUID.randomUUID();

        subscription = new Subscription("Test Subscription");
        setFieldValue(subscription, "id", subscriptionId);
        setFieldValue(subscription, "tenant", "test-tenant");
        setFieldValue(subscription, "ingestionId", UUID.randomUUID().toString());

        endpointAuth = EndpointAuth.builder()
                .authType(AuthType.API_KEY)
                .apiKeyName("X-API-Key")
                .apiKeyValue("secret-key")
                .build();
        setFieldValue(endpointAuth, "id", UUID.randomUUID());

        endpoint = Endpoint.builder()
                .url("https://example.com/webhook")
                .isActive(true)
                .endpointAuth(endpointAuth)
                .build();
        setFieldValue(endpoint, "id", endpointId);
        setFieldValue(endpoint, "subscription", subscription);
        setFieldValue(endpointAuth, "endpoint", endpoint);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
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
    void givenValidSubscription_whenSave_thenReturnSavedSubscription() {
        // given
        Subscription newSubscription = new Subscription("New Subscription");
        TenantContext.set(TenantContext.builder().tenantId("test-tenant").build());

        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> {
            Subscription saved = invocation.getArgument(0);
            try {
                setFieldValue(saved, "id", UUID.randomUUID());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return saved;
        });

        // when
        Subscription result = subscriptionService.save(newSubscription);

        // then
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals("New Subscription", result.getName());
        assertEquals("test-tenant", result.getTenant());
        assertNotNull(result.getIngestionId());
        verify(subscriptionRepository, times(1)).save(any(Subscription.class));
    }

    @Test
    void givenDuplicateSubscription_whenSave_thenThrowConflictException() {
        // given
        Subscription newSubscription = new Subscription("Duplicate Subscription");
        TenantContext.set(TenantContext.builder().tenantId("test-tenant").build());

        when(subscriptionRepository.save(any(Subscription.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry"));

        // when & then
        ConflictException exception = assertThrows(ConflictException.class, () -> {
            subscriptionService.save(newSubscription);
        });
        assertEquals("Subscription already exists", exception.getMessage());
        verify(subscriptionRepository, times(1)).save(any(Subscription.class));
    }

    @Test
    void givenExistingSubscription_whenUpdate_thenReturnUpdatedSubscription() {
        // given
        Subscription updateData = new Subscription("Updated Subscription Name");

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(subscription);

        // when
        Subscription result = subscriptionService.update(subscriptionId, updateData);

        // then
        assertNotNull(result);
        verify(subscriptionRepository, times(1)).findById(subscriptionId);
        verify(subscriptionRepository, times(1)).save(subscription);
    }

    @Test
    void givenNonExistentSubscription_whenUpdate_thenThrowResourceNotFoundException() {
        // given
        Subscription updateData = new Subscription("Updated Name");
        UUID nonExistentId = UUID.randomUUID();

        when(subscriptionRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // when & then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            subscriptionService.update(nonExistentId, updateData);
        });
        assertEquals("subscription not found", exception.getMessage());
        verify(subscriptionRepository, times(1)).findById(nonExistentId);
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void givenExistingSubscription_whenDelete_thenDeleteSuccessfully() {
        // given
        doNothing().when(eventRepository).deleteBySubscriptionId(subscriptionId);
        doNothing().when(subscriptionRepository).deleteById(subscriptionId);

        // when
        subscriptionService.delete(subscriptionId);

        // then
        verify(eventRepository, times(1)).deleteBySubscriptionId(subscriptionId);
        verify(subscriptionRepository, times(1)).deleteById(subscriptionId);
    }

    @Test
    void givenExistingSubscription_whenFindById_thenReturnSubscription() {
        // given
        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));

        // when
        Subscription result = subscriptionService.findById(subscriptionId);

        // then
        assertNotNull(result);
        assertEquals(subscriptionId, result.getId());
        assertEquals("Test Subscription", result.getName());
        verify(subscriptionRepository, times(1)).findById(subscriptionId);
    }

    @Test
    void givenNonExistentSubscription_whenFindById_thenThrowResourceNotFoundException() {
        // given
        UUID nonExistentId = UUID.randomUUID();
        when(subscriptionRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // when & then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            subscriptionService.findById(nonExistentId);
        });
        assertEquals("subscription not found", exception.getMessage());
        verify(subscriptionRepository, times(1)).findById(nonExistentId);
    }

    @Test
    void givenName_whenFindByName_thenReturnMatchingSubscriptions() {
        // given
        String searchName = "Test";
        List<Subscription> expectedSubscriptions = Arrays.asList(subscription);
        when(subscriptionRepository.findByNameContainingIgnoreCase(searchName)).thenReturn(expectedSubscriptions);

        // when
        List<Subscription> result = subscriptionService.findByName(searchName);

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Subscription", result.get(0).getName());
        verify(subscriptionRepository, times(1)).findByNameContainingIgnoreCase(searchName);
    }

    @Test
    void givenDateRange_whenList_thenReturnPagedSubscriptions() {
        // given
        OffsetDateTime fromDate = OffsetDateTime.now().minusDays(7);
        OffsetDateTime toDate = OffsetDateTime.now();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Subscription> expectedPage = new PageImpl<>(Arrays.asList(subscription));

        when(subscriptionRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(expectedPage);

        // when
        Page<Subscription> result = subscriptionService.list(fromDate, toDate, pageable);

        // then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(subscriptionRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void givenExistingIngestionId_whenFindByIngestionId_thenReturnSubscription() {
        // given
        String ingestionId = subscription.getIngestionId();
        when(subscriptionRepository.findByIngestionId(ingestionId)).thenReturn(Optional.of(subscription));

        // when
        Subscription result = subscriptionService.findByIngestionId(ingestionId);

        // then
        assertNotNull(result);
        assertEquals(ingestionId, result.getIngestionId());
        verify(subscriptionRepository, times(1)).findByIngestionId(ingestionId);
    }

    @Test
    void givenNonExistentIngestionId_whenFindByIngestionId_thenThrowResourceNotFoundException() {
        // given
        String nonExistentIngestionId = "non-existent-id";
        when(subscriptionRepository.findByIngestionId(nonExistentIngestionId)).thenReturn(Optional.empty());

        // when & then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            subscriptionService.findByIngestionId(nonExistentIngestionId);
        });
        assertEquals("Subscription not found", exception.getMessage());
        verify(subscriptionRepository, times(1)).findByIngestionId(nonExistentIngestionId);
    }

    @Test
    void givenExistingSubscription_whenAddEndpoints_thenEndpointsAddedSuccessfully() throws Exception {
        // given
        List<Endpoint> newEndpoints = Arrays.asList(
                createEndpoint("https://endpoint1.com", true),
                createEndpoint("https://endpoint2.com", false)
        );

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(subscription);

        // when
        Subscription result = subscriptionService.addEndpoints(newEndpoints, subscriptionId);

        // then
        assertNotNull(result);
        verify(subscriptionRepository, times(1)).findById(subscriptionId);
        verify(subscriptionRepository, times(1)).save(subscription);
    }

    private Endpoint createEndpoint(String url, boolean isActive) throws Exception {
        Endpoint ep = Endpoint.builder()
                .url(url)
                .isActive(isActive)
                .build();
        setFieldValue(ep, "id", UUID.randomUUID());
        return ep;
    }

    @Test
    void givenNonExistentSubscription_whenAddEndpoints_thenThrowResourceNotFoundException() {
        // given
        UUID nonExistentId = UUID.randomUUID();
        List<Endpoint> newEndpoints = Collections.emptyList();

        when(subscriptionRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // when & then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            subscriptionService.addEndpoints(newEndpoints, nonExistentId);
        });
        assertEquals("subscription not found", exception.getMessage());
        verify(subscriptionRepository, times(1)).findById(nonExistentId);
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void givenExistingEndpoint_whenUpdateEndpoint_thenReturnUpdatedSubscription() throws Exception {
        // given
        EndpointAuth newAuth = EndpointAuth.builder()
                .authType(AuthType.BASIC_AUTH)
                .basicUsername("user")
                .basicPassword("pass")
                .build();
        setFieldValue(newAuth, "id", UUID.randomUUID());

        Endpoint updatedEndpoint = Endpoint.builder()
                .url("https://updated-url.com")
                .isActive(false)
                .endpointAuth(newAuth)
                .build();
        setFieldValue(updatedEndpoint, "id", UUID.randomUUID());

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        when(endpointRepository.findByIdAndSubscriptionId(endpointId, subscriptionId)).thenReturn(Optional.of(endpoint));
        when(endpointRepository.save(any(Endpoint.class))).thenReturn(endpoint);

        // when
        Subscription result = subscriptionService.updateEndpoint(updatedEndpoint, subscriptionId, endpointId);

        // then
        assertNotNull(result);
        verify(subscriptionRepository, times(1)).findById(subscriptionId);
        verify(endpointRepository, times(1)).findByIdAndSubscriptionId(endpointId, subscriptionId);
        verify(endpointRepository, times(1)).save(any(Endpoint.class));
    }

    @Test
    void givenNonExistentSubscription_whenUpdateEndpoint_thenThrowResourceNotFoundException() throws Exception {
        // given
        UUID nonExistentSubId = UUID.randomUUID();
        Endpoint updatedEndpoint = Endpoint.builder()
                .url("https://updated.com")
                .isActive(true)
                .build();
        setFieldValue(updatedEndpoint, "id", UUID.randomUUID());

        when(subscriptionRepository.findById(nonExistentSubId)).thenReturn(Optional.empty());

        // when & then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            subscriptionService.updateEndpoint(updatedEndpoint, nonExistentSubId, endpointId);
        });
        assertEquals("subscription not found", exception.getMessage());
        verify(subscriptionRepository, times(1)).findById(nonExistentSubId);
        verify(endpointRepository, never()).findByIdAndSubscriptionId(any(), any());
    }

    @Test
    void givenNonExistentEndpoint_whenUpdateEndpoint_thenThrowResourceNotFoundException() throws Exception {
        // given
        UUID nonExistentEndpointId = UUID.randomUUID();
        Endpoint updatedEndpoint = Endpoint.builder()
                .url("https://updated.com")
                .isActive(true)
                .build();
        setFieldValue(updatedEndpoint, "id", UUID.randomUUID());

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        when(endpointRepository.findByIdAndSubscriptionId(nonExistentEndpointId, subscriptionId)).thenReturn(Optional.empty());

        // when & then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            subscriptionService.updateEndpoint(updatedEndpoint, subscriptionId, nonExistentEndpointId);
        });
        assertEquals("Endpoint not found", exception.getMessage());
        verify(subscriptionRepository, times(1)).findById(subscriptionId);
        verify(endpointRepository, times(1)).findByIdAndSubscriptionId(nonExistentEndpointId, subscriptionId);
    }

    @Test
    void givenExistingSubscription_whenListEndpoints_thenReturnEndpoints() {
        // given
        List<Endpoint> expectedEndpoints = Arrays.asList(endpoint);
        when(endpointRepository.findBySubscriptionId(subscriptionId)).thenReturn(expectedEndpoints);

        // when
        List<Endpoint> result = subscriptionService.listEndpoints(subscriptionId);

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("https://example.com/webhook", result.get(0).getUrl());
        verify(endpointRepository, times(1)).findBySubscriptionId(subscriptionId);
    }

    @Test
    void givenExistingEndpoint_whenDeleteEndpoint_thenDeleteSuccessfully() {
        // given
        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        when(endpointRepository.findByIdAndSubscriptionId(endpointId, subscriptionId)).thenReturn(Optional.of(endpoint));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(subscription);
        doNothing().when(endpointRepository).delete(any(Endpoint.class));

        // when
        subscriptionService.deleteEndpoint(subscriptionId, endpointId);

        // then
        verify(subscriptionRepository, times(1)).findById(subscriptionId);
        verify(endpointRepository, times(1)).findByIdAndSubscriptionId(endpointId, subscriptionId);
        verify(subscriptionRepository, times(1)).save(subscription);
        verify(endpointRepository, times(1)).delete(endpoint);
    }

    @Test
    void givenNonExistentSubscription_whenDeleteEndpoint_thenThrowResourceNotFoundException() {
        // given
        UUID nonExistentSubId = UUID.randomUUID();

        when(subscriptionRepository.findById(nonExistentSubId)).thenReturn(Optional.empty());

        // when & then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            subscriptionService.deleteEndpoint(nonExistentSubId, endpointId);
        });
        assertEquals("subscription not found", exception.getMessage());
        verify(subscriptionRepository, times(1)).findById(nonExistentSubId);
        verify(endpointRepository, never()).findByIdAndSubscriptionId(any(), any());
    }

    @Test
    void givenNonExistentEndpoint_whenDeleteEndpoint_thenThrowResourceNotFoundException() {
        // given
        UUID nonExistentEndpointId = UUID.randomUUID();

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        when(endpointRepository.findByIdAndSubscriptionId(nonExistentEndpointId, subscriptionId)).thenReturn(Optional.empty());

        // when & then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            subscriptionService.deleteEndpoint(subscriptionId, nonExistentEndpointId);
        });
        assertEquals("Endpoint not found", exception.getMessage());
        verify(subscriptionRepository, times(1)).findById(subscriptionId);
        verify(endpointRepository, times(1)).findByIdAndSubscriptionId(nonExistentEndpointId, subscriptionId);
    }
}
