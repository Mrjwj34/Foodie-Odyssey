-- 比较当前线程标识和锁中的标识
if redis.call("get", KEYS[1] ) == ARGV[1] then
    -- 相等则删除锁
    return redis.call("del", KEYS[1])
end
-- 不相等则返回0
return 0
