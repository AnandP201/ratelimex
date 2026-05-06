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

    if tokens == nil then
        tokens = capacity
        last_time = now
    end

    local delta = math.max(0, now - last_time)
    local refill = (delta * refill_rate) / 1000
    tokens = math.min(capacity, tokens + refill)


    local remaining_if_consumed = tokens - cost

    if remaining_if_consumed < 0 then
        allowed = 0
        if refill_rate > 0 then
            local candidate_retry = math.ceil((cost - tokens) * 1000 / refill_rate)
            retry_after_ms = math.max(retry_after_ms, candidate_retry)
        else
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
