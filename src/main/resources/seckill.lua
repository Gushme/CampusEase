-- 库存用 String类型，订单用 Set类型，存储每张优惠券已经买过的用户
-- 1.参数列表
-- 1.1 优惠券id
local voucherId = ARGV[1]
-- 1.2 用户id
local userId = ARGV[2]
-- 1.3. 订单id
local orderId = ARGV[3]
-- 1.4. 当前时间戳（毫秒）
local currentTime = tonumber(ARGV[4])

-- 2.数据key
-- 2.1 库存key hash结构
local stockKey = 'seckill:stock:' .. voucherId
-- 2.2 订单key
local orderKey = 'seckill:order:' .. voucherId

-- 3. 从Redis获取开始时间和结束时间
local beginTime = redis.call('hget', stockKey, 'beginTime')
local endTime = redis.call('hget', stockKey, 'endTime')
if beginTime then
    beginTime = tonumber(beginTime)
else
    return 1
end

if endTime then
    endTime = tonumber(endTime)
else
    return 1
end
-- 4. 检查活动是否已经开始和是否已经结束
if currentTime < beginTime then
    -- 4.1. 秒杀尚未开始，返回2
    return 2
elseif currentTime > endTime then
    -- 4.2. 秒杀已经结束，返回3
    return 3
end
-- 5. 检查库存是否充足
if(tonumber(redis.call('get', stockKey, 'stock')) <= 0) then
    return 4
end
-- 6. 判断用户是否下单 SISMEMBER orderKey userId
if(redis.call('sismember', orderKey, userId) == 1) then
    return 5
end

-- 扣库存 incrby stockKey -1
redis.call('incrby', stockKey, -1)

-- 下单，保存 sadd orderKey userId
redis.call('sadd', orderKey, userId)

return 0

