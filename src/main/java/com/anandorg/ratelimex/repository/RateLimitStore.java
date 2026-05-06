package com.anandorg.ratelimex.repository;

import java.util.List;

import com.anandorg.ratelimex.model.RateLimitBucket;

public interface RateLimitStore {

    RateLimitStoreResult consume(List<RateLimitBucket> buckets, int cost);
}
