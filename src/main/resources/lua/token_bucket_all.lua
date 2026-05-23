-- ARGV =
-- [ currentTime (ms) , apiCost ,
-- token capacity userId, refillRate userId, expiry userId,
-- token capacity api,refillRate api, expiry api,
-- token capacity tenant,refillRate tenant, expiry tenant ]

-- i : 1 -> 3
-- capacity : 3 * i
-- refill_rate : (3 * i) + 1
-- ttl : (3 * i) + 2


local now = tonumber(ARGV[1])
local cost = tonumber(ARGV[2])

local allowed = 1
local min_remaining = nil
local retry_after_ms = 0
local tokens_by_index = {}
local timestamps_by_index = {}

for i = 1, #KEYS do

    local arg_offset = 3 + ((i - 1) * 3)

    local capacity = tonumber(ARGV[arg_offset])
    local refill_rate = tonumber(ARGV[arg_offset + 1])

    local data = redis.call("HMGET", KEYS[i], "tokens", "timestamp")

    local tokens = tonumber(data[1])
    local last_time = tonumber(data[2])

    -- If first request ever, to server
    -- we will have no hashmaps stored
    if tokens == nil then
        tokens = capacity
        last_time = now
    end


    local delta = math.max(0, now - last_time)

    local refill = (delta * refill_rate) / 1000

    tokens = math.min(capacity, tokens + refill)

    -- check do we have tokens left
    local left_tokens = tokens - cost

    if left_tokens < 0 then
        -- no tokens left, so calculate retry time
        allowed = 0
        if refill_rate > 0 then
            -- do we have a refill_rate ? YES
            local candidate_retry = math.ceil((cost - tokens) * 1000 / refill_rate)
            retry_after_ms = math.max(retry_after_ms, candidate_retry)
        else
            -- if cannot refill , so add a constant retry time
            retry_after_ms = math.max(retry_after_ms, 1000)
        end
    end

    tokens_by_index[i] = tokens
    timestamps_by_index[i] = now
end

for i = 1, #KEYS do
    local arg_offset = 3 + ((i - 1) * 3)
    local ttl_seconds = tonumber(ARGV[arg_offset + 2])
    local tokens = tokens_by_index[i]

    if allowed == 1 then
        tokens = tokens - cost
    end

    local observable_remaining = math.max(0, math.floor(tokens))

    if min_remaining == nil or observable_remaining < min_remaining then
        min_remaining = observable_remaining
    end

    redis.call("HSET", KEYS[i], "tokens", tokens, "timestamp", timestamps_by_index[i])
    redis.call("EXPIRE", KEYS[i], ttl_seconds)
end

return { allowed, min_remaining or 0, retry_after_ms }
