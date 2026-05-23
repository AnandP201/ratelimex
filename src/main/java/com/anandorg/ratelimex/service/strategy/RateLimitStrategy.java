package com.anandorg.ratelimex.service.strategy;

import com.anandorg.ratelimex.model.RateLimitDecision;

public interface RateLimitStrategy {

    RateLimitDecision allow(String tenantId, String userId, String api, int cost);
}
