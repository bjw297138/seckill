---@diagnostic disable: undefined-global

-- 获取传入的参数
local productId = ARGV[1]  -- 商品
local userId = ARGV[2]      -- 用户ID


-- 库存 key，用于存储商品剩余库存数量
local stockKey = 'product:stock:' .. productId
-- 下订单 key，使用集合存储已下单用户ID，防止重复下单
local orderKey = 'product:user:' .. productId

-- 检查库存是否充足
if (tonumber(redis.call('get', stockKey)) <= 0) then
    -- 库存不足，返回状态码 1
    return 1
end

-- 检查用户是否已经下过单
if (redis.call('sismember', orderKey, userId) == 1) then
    -- 用户已经下过单，返回状态码 2
    return 2
end

-- 执行秒杀操作
-- 库存减 1
redis.call('incrby', stockKey , -1)
-- 将用户ID添加到已下单集合中
redis.call('sadd', orderKey, userId)

-- 秒杀成功，返回状态码 0
return 0