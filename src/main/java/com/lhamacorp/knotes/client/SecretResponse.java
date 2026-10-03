package com.lhamacorp.knotes.client;

import java.time.Instant;

record SecretResponse(String id, String name, String versionId, String secretString, Instant createdAt) {
}
