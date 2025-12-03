package com.osigie.rehook.service.impl.HttpClient;

import com.osigie.rehook.domain.model.EndpointAuth;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.HexFormat;
import java.util.Map;

@Component
public class HmacAuthStrategy implements AuthStrategy {

    @Override
    public Map<String, String> getHeaders(EndpointAuth auth, String payload) {
        String signature = computeHmac(payload, auth.getHmacSecret());
        return Map.of("X-Signature", signature);
    }

    private String computeHmac(String payload, String secret) {
        try {
            Mac sha256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
            sha256.init(keySpec);
            byte[] hash = sha256.doFinal(payload.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Unable to compute HMAC", e);
        }
    }
}
