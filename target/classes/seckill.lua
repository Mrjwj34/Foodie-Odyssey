-- 参数列表
-- 优惠券id
local voucherId = ARGV[1]
-- 用户id
local userId = ARGV[2]

-- 库存key
local stockKey = "seckill:stock:" .. voucherId
-- 订单key
local orderKey = "seckill:order:" .. voucherId

-- 脚本业务
-- 判断库存是否充足
if(tonumber(redis.call("get", stockKey)) <= 0) then
    return 1
end
-- 判断用户是否已经下单 sismember orkerkey userid
if(redis.call("sismember", orderKey, userId) == 1) then
-- 已经下单
    return 2
end
-- 库存减1
redis.call("incrby", stockKey, -1)
-- 订单记录
redis.call("sadd", orderKey, userId)
return 0