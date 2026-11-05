package com.caio.paymentplatform.charge.idempotency;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service 
public class IdempotencyService {
    
    private final IdempotencyRepository idempotencyRepository;

    public IdempotencyService(IdempotencyRepository idempotencyRepository) {
        this.idempotencyRepository = idempotencyRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public IdempotencyRecord registerOrRetrieve(String idempotencyKey, String requestHash) {

        UUID id = UUID.randomUUID();

        int inserted = idempotencyRepository.insertIfAbsent(
            id,
            idempotencyKey,
            requestHash,
            Instant.now()
        );

        IdempotencyRecord record = idempotencyRepository.findByIdempotencyKey(idempotencyKey)
            .orElseThrow(() -> new IllegalStateException("idempotency record could not be retrieved"));

        if (!record.getRequestHash().equals(requestHash)) {
            throw new IdempotencyKeyConflictException(idempotencyKey);
        }

        if (inserted == 0 && !record.isCompleted()) {
            throw new IllegalStateException("Idempotency record is not completed");
        }

        return record;
    }

    public String generateRequestHash(String method, String path) {

        try {
            
            String requestData = method + ":" + path;

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(requestData.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
        }
    }
}