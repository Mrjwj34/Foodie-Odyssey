-- 参数列表
-- 优惠券id
local voucherId = ARGV[1]
-- 用户id
local userId = ARGV[2]
-- 订单id
local orderId = ARGV[3]

-- 库存key
local stockKey = "seckill:voucher:" .. voucherId
-- 订单key
local orderKey = "seckill:order:" .. voucherId

-- 脚本业务
-- 判断库存是否充足
if(tonumber(redis.call("hget", stockKey, "stock")) <= 0) then
    return 1
end
-- 判断用户是否已经下单 sismember orkerkey userid
if(redis.call("sismember", orderKey, userId) == 1) then
-- 已经下单
    return 2
end
-- 库存减1
redis.call("hincrby", stockKey, "stock", -1)
-- 订单记录
redis.call("sadd", orderKey, userId)
-- 将订单数据加入stream消息队列
redis.call("xadd", "stream.orders", "*", "id", orderId, "voucherId", voucherId, "userId", userId)
return 0