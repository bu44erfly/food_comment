
缓存和数据库一致性
  查询店铺：
    id--> 查询（先查缓存再查数据库，回写缓存
  修改店铺数据：
    Shop --->  改数据库，删除缓存
  

下单
  获取锁-->创建订单 --->mq-->扣减存量（乐观锁）
    1.获取锁：redis setnx(threadId)   释放锁(lua：判断锁+释放)
    2.zookeeper
点赞
  使用redis zset 
  使用blog id 查找 redis -->blog中是否由userId(user的评分数据）, 依次操作isliked字段
点赞排行榜
 redis zset zset.range(0,5) -->用户列表

共同关注：
(key1 key2 )
  redis set 取交集---> ids


