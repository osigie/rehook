package com.osigie.rehook.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osigie.rehook.config.AbstractContainerBaseTest;
import com.osigie.rehook.domain.model.Subscription;
import com.osigie.rehook.service.IngestionService;
import com.osigie.rehook.service.SubscriptionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class IngestionControllerTest extends AbstractContainerBaseTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IngestionService ingestionService;

    @MockitoBean
    private SubscriptionService subscriptionService;

    @Test
    public void givenValidIngestionRequest_whenIngest_thenReturnAccepted() throws Exception {
        //given
        UUID ingestionId = UUID.randomUUID();
        String payload = "{\"event\": \"test\"}";
        Subscription subscription = new Subscription("test-subscription");
        subscription.setIngestionId(ingestionId.toString());
        subscription.setTenant("test-tenant");
        when(subscriptionService.findByIngestionId(ingestionId.toString())).thenReturn(subscription);
        
        //when & then
        mockMvc.perform(post("/ingest/{ingestionId}", ingestionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .header("X-Custom-Header", "value"))
                .andExpect(status().isAccepted());
        
        verify(ingestionService).ingest(eq(ingestionId.toString()), eq(payload), any(Map.class));
    }

    @Test
    public void givenInvalidIngestionId_whenIngest_thenReturnBadRequest() throws Exception {
        //given
        UUID ingestionId = UUID.randomUUID();
        String payload = "{\"event\": \"test\"}";
        Subscription subscription = new Subscription("test-subscription");
        subscription.setIngestionId(ingestionId.toString());
        subscription.setTenant("test-tenant");
        when(subscriptionService.findByIngestionId(ingestionId.toString())).thenReturn(subscription);
        doThrow(new IllegalArgumentException("Invalid ingestion ID")).when(ingestionService)
                .ingest(any(String.class), any(String.class), any(Map.class));
        
        //when & then
        mockMvc.perform(post("/ingest/{ingestionId}", ingestionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isBadRequest());
    }
}
