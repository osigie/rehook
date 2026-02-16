package com.osigie.rehook.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osigie.rehook.config.AbstractContainerBaseTest;
import com.osigie.rehook.domain.model.AuthType;
import com.osigie.rehook.dto.request.EndpointAuthRequestDto;
import com.osigie.rehook.dto.request.EndpointRequestDto;
import com.osigie.rehook.dto.request.SubscriptionRequestDto;
import com.osigie.rehook.service.SubscriptionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@WithMockUser(username = "test@example.com", roles = {"USER"})
public class SubscriptionControllerTest extends AbstractContainerBaseTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SubscriptionService subscriptionService;

    @Test
    public void givenSubscriptionRequest_whenCreate_thenReturnCreated() throws Exception {
        //given
        SubscriptionRequestDto requestDto = new SubscriptionRequestDto("test-subscription");
        com.osigie.rehook.domain.model.Subscription subscription = com.osigie.rehook.domain.model.Subscription.builder().name("test-subscription").build();

        when(subscriptionService.save(any(com.osigie.rehook.domain.model.Subscription.class))).thenReturn(subscription);

        //when & then
        mockMvc.perform(post("/api/subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated());
    }

    @Test
    public void givenSubscriptionId_whenGet_thenReturnSubscription() throws Exception {
        //given
        UUID subscriptionId = UUID.randomUUID();
        com.osigie.rehook.domain.model.Subscription subscription = com.osigie.rehook.domain.model.Subscription.builder().name("test-subscription").build();

        when(subscriptionService.findById(subscriptionId)).thenReturn(subscription);

        //when & then
        mockMvc.perform(get("/api/subscriptions/{id}", subscriptionId))
                .andExpect(status().isCreated());
    }

    @Test
    public void givenNoFilters_whenList_thenReturnPage() throws Exception {
        //given
        List<com.osigie.rehook.domain.model.Subscription> subscriptions = List.of(com.osigie.rehook.domain.model.Subscription.builder().name("test-subscription").build());
        Page<com.osigie.rehook.domain.model.Subscription> subscriptionPage = new PageImpl<>(subscriptions, PageRequest.of(0, 10), 1);

        when(subscriptionService.list(any(), any(), any(PageRequest.class))).thenReturn(subscriptionPage);

        //when & then
        mockMvc.perform(get("/api/subscriptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecords").value(1))
                .andExpect(jsonPath("$.pageNo").value(0))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    public void givenSubscriptionId_whenDelete_thenReturnNoContent() throws Exception {
        //given
        UUID subscriptionId = UUID.randomUUID();

        //when & then
        mockMvc.perform(delete("/api/subscriptions/{id}", subscriptionId))
                .andExpect(status().isNoContent());
    }

    @Test
    public void givenEndpoints_whenCreateEndpoints_thenReturnCreated() throws Exception {
        //given
        UUID subscriptionId = UUID.randomUUID();
        EndpointAuthRequestDto endpointAuth = new EndpointAuthRequestDto(AuthType.NONE, null, null, null, null, null);
        EndpointRequestDto endpointDto = new EndpointRequestDto(true, "https://example.com/webhook", endpointAuth);
        List<EndpointRequestDto> endpointDtos = List.of(endpointDto);
        com.osigie.rehook.domain.model.Subscription subscription = com.osigie.rehook.domain.model.Subscription.builder().name("test-subscription").build();

        when(subscriptionService.addEndpoints(anyList(), eq(subscriptionId))).thenReturn(subscription);

        //when & then
        mockMvc.perform(post("/api/subscriptions/{id}/endpoints", subscriptionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(endpointDtos)))
                .andExpect(status().isCreated());
    }

    @Test
    public void givenSubscriptionId_whenGetEndpoints_thenReturnEndpoints() throws Exception {
        //given
        UUID subscriptionId = UUID.randomUUID();
        List<com.osigie.rehook.domain.model.Endpoint> endpoints = List.of(
                com.osigie.rehook.util.TestDataFactory.createTestEndpoint(null));

        when(subscriptionService.listEndpoints(subscriptionId)).thenReturn(endpoints);

        //when & then
        mockMvc.perform(get("/api/subscriptions/{id}/endpoints", subscriptionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    public void givenEndpointRequest_whenUpdateEndpoint_thenReturnUpdated() throws Exception {
        //given
        UUID subscriptionId = UUID.randomUUID();
        UUID endpointId = UUID.randomUUID();
        EndpointAuthRequestDto endpointAuth = new EndpointAuthRequestDto(AuthType.NONE, null, null, null, null, null);
        EndpointRequestDto requestDto = new EndpointRequestDto(true, "https://updated.com/webhook", endpointAuth);
        com.osigie.rehook.domain.model.Subscription subscription = com.osigie.rehook.domain.model.Subscription.builder().name("test-subscription").build();

        when(subscriptionService.updateEndpoint(any(), eq(subscriptionId), eq(endpointId))).thenReturn(subscription);

        //when & then
        mockMvc.perform(put("/api/subscriptions/{id}/endpoints/{endpointId}", subscriptionId, endpointId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk());
    }

    @Test
    public void givenSubscriptionIdAndEndpointId_whenDeleteEndpoint_thenReturnNoContent() throws Exception {
        //given
        UUID subscriptionId = UUID.randomUUID();
        UUID endpointId = UUID.randomUUID();

        //when & then
        mockMvc.perform(delete("/api/subscriptions/{id}/endpoints/{endpointId}", subscriptionId, endpointId))
                .andExpect(status().isNoContent());
    }
}