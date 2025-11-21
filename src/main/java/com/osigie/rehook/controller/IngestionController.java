package com.osigie.rehook.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.osigie.rehook.service.IngestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        return ResponseEntity.accepted().build();
    }
}
