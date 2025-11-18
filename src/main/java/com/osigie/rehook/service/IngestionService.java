package com.osigie.rehook.service;

import java.util.Map;

public interface IngestionService {

    void ingest(String ingestionId,  String payload, Map<String, String> headers);
}
