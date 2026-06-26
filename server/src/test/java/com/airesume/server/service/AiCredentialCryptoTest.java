package com.airesume.server.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiCredentialCryptoTest {

    @Test
    void shouldEncryptAndDecryptApiKey() {
        AiCredentialCrypto crypto = new AiCredentialCrypto("test-secret-for-ai-key-encryption");

        String encrypted = crypto.encrypt("sk-test-secret");

        assertNotEquals("sk-test-secret", encrypted);
        assertTrue(crypto.isEncrypted(encrypted));
        assertEquals("sk-test-secret", crypto.decrypt(encrypted));
    }

    @Test
    void shouldTreatExistingPlaintextAsBackwardCompatible() {
        AiCredentialCrypto crypto = new AiCredentialCrypto("test-secret-for-ai-key-encryption");

        assertEquals("sk-existing", crypto.decrypt("sk-existing"));
    }
}
