package com.example.userauth.utils;

import org.springframework.http.ResponseEntity;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

public class ETagUtil {
    public static String generateETag(String content) {
        // Use MD5 hash for ETag (can use SHA256 for more security)
        return '"' + DigestUtils.md5DigestAsHex(content.getBytes(StandardCharsets.UTF_8)) + '"';
    }

    public static <T> ResponseEntity<T> withETag(ResponseEntity.BodyBuilder builder, String eTag, T body) {
        return builder.eTag(eTag).body(body);
    }
}
