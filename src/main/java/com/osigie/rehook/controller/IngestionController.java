package com.osigie.rehook.controller;

import com.osigie.rehook.service.IngestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(value = "/ingest")
@Validated
public class IngestionController {
    private final IngestionService ingestionService;

    public IngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }


    @PostMapping("/{ingestionId}")
    public ResponseEntity<Void> ingest(
            @PathVariable UUID ingestionId,
            @RequestBody String payload,
            @RequestHeader Map<String, Object> headers
    ) {
        try {
            ingestionService.ingest(ingestionId.toString(), payload, headers);
            return ResponseEntity.accepted().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
