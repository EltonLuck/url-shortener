package com.elton_luck.url_shortener.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitServiceTest {

    @Test
    void tenConsumed_eleventhRejected() {
        RateLimitService limits = new RateLimitService();
        for (int i = 0; i < 10; i++) {
            assertTrue(limits.tryCreate("203.0.113.7").isConsumed(),
                    "token " + (i + 1) + " should be consumed");
        }
        assertFalse(limits.tryCreate("203.0.113.7").isConsumed(),
                "11th create from the same IP should be rejected");
    }

    @Test
    void differentIp_getsFreshBucket() {
        RateLimitService limits = new RateLimitService();
        for (int i = 0; i < 10; i++) {
            limits.tryCreate("203.0.113.7");
        }
        assertTrue(limits.tryCreate("203.0.113.8").isConsumed(),
                "a different IP has its own bucket");
    }
}