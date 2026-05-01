local stockKey = KEYS[1]
local usersKey = KEYS[2]

local userId = ARGV[1]
local now = tonumber(ARGV[2])
local startTime = tonumber(ARGV[3])
local endTime = tonumber(ARGV[4])

if now < startTime then
    return 1
end

if now > endTime then
    return 2
end

local stock = tonumber(redis.call('GET', stockKey) or '0')
if stock <= 0 then
    return 3
end

if redis.call('SISMEMBER', usersKey, userId) == 1 then
    return 4
end

redis.call('DECR', stockKey)
redis.call('SADD', usersKey, userId)
return 0
