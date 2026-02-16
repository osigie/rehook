package com.osigie.rehook.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osigie.rehook.config.AbstractContainerBaseTest;
import com.osigie.rehook.service.DispatcherService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@WithMockUser(username = "test@example.com", roles = {"USER"})
public class DeliveryControllerTest extends AbstractContainerBaseTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DispatcherService dispatcherService;

    private void setupDispatcherServiceMocks() {
        when(dispatcherService.findRetries(any(PageRequest.class))).thenReturn(Page.empty());
    }

    @Test
    public void givenDeliveryIds_whenRetry_thenReturnOk() throws Exception {
        //given
        List<UUID> deliveryIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        setupDispatcherServiceMocks();

        //when & then
        mockMvc.perform(post("/api/deliveries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(deliveryIds)))
                .andExpect(status().isOk());
    }

    @Test
    public void givenNoFilters_whenListDeliveries_thenReturnPage() throws Exception {
        //given
        List<com.osigie.rehook.domain.model.Delivery> deliveries = List.of(
                com.osigie.rehook.util.TestDataFactory.createTestDelivery(null, null));
        Page<com.osigie.rehook.domain.model.Delivery> deliveryPage = new PageImpl<>(deliveries, PageRequest.of(0, 10), 1);

        when(dispatcherService.listDeliveries(any(PageRequest.class), any(), any(), any())).thenReturn(deliveryPage);
        setupDispatcherServiceMocks();

        //when & then
        mockMvc.perform(get("/api/deliveries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecords").value(1))
                .andExpect(jsonPath("$.pageNo").value(0))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    public void givenStatusFilter_whenListDeliveries_thenReturnFilteredPage() throws Exception {
        //given
        List<com.osigie.rehook.domain.model.Delivery> deliveries = List.of(
                com.osigie.rehook.util.TestDataFactory.createTestDelivery(null, null));
        Page<com.osigie.rehook.domain.model.Delivery> deliveryPage = new PageImpl<>(deliveries, PageRequest.of(0, 10), 1);

        when(dispatcherService.listDeliveries(any(PageRequest.class), any(), any(), 
                eq(com.osigie.rehook.domain.model.DeliveryStatusEnum.SUCCEEDED))).thenReturn(deliveryPage);
        setupDispatcherServiceMocks();

        //when & then
        mockMvc.perform(get("/api/deliveries")
                .param("status", "SUCCEEDED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecords").value(1));
    }

    @Test
    public void givenDeliveryId_whenGetDelivery_thenReturnDelivery() throws Exception {
        //given
        UUID deliveryId = UUID.randomUUID();
        com.osigie.rehook.domain.model.Delivery delivery = 
                com.osigie.rehook.util.TestDataFactory.createTestDelivery(null, null);

        when(dispatcherService.getDelivery(deliveryId)).thenReturn(delivery);
        setupDispatcherServiceMocks();

        //when & then
        mockMvc.perform(get("/api/deliveries/{id}", deliveryId))
                .andExpect(status().isOk());
    }
}
