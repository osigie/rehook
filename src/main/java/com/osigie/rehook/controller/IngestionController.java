package com.osigie.rehook.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.osigie.rehook.service.IngestionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(value = "/ingest")
public class IngestionController {
    private final IngestionService ingestionService;

    public IngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/{ingestionId}")
    public ResponseEntity<?> ingest(@PathVariable(name = "ingestionId") String ingestionId, @RequestBody Map<String, Object> payload, @RequestHeader Map<String, String> headers
    ) throws JsonProcessingException {

        ObjectMapper mapper = new ObjectMapper();
        String payloadString = mapper.writeValueAsString(payload);

        this.ingestionService.ingest(ingestionId, payloadString, headers);
        System.out.println("ingestionId: " + ingestionId);
        return ResponseEntity.accepted().build();
    }
}
