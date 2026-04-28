local value = redis.call('GET', KEYS[1])
if value then
    redis.call('DEL', KEYS[1])
    return value
else
    return 0
end
