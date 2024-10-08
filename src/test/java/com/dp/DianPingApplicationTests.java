package com.dp;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dp.dto.LoginFormDTO;
import com.dp.entity.Shop;
import com.dp.entity.User;
import com.dp.mapper.UserMapper;
import com.dp.service.impl.ShopServiceImpl;
import com.dp.service.impl.UserServiceImpl;
import com.dp.utils.CacheClient;
import com.dp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.dp.utils.RedisConstants.SHOP_GEO_KEY;

@SpringBootTest
@RunWith(SpringRunner.class)
class DianPingApplicationTests {

    @Resource
    private ShopServiceImpl shopService;

    @Resource
    private CacheClient cacheClient;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private UserServiceImpl userService;

    @Resource
    private UserMapper userMapper;

    private ExecutorService es = Executors.newFixedThreadPool(500);



    @Test
    void testSavaShop() {
//        shopService.saveShop2Redis(1L,10L);
//        Shop shop = shopService.getById(1L);
//
//        cacheClient.setWithLogicalExpire(CACHE_SHOP_KEY + 1L, shop, 10L, TimeUnit.SECONDS);


    }

    @Test
    void testIdWorker() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(300);

        Runnable task = () -> {
            for (int i = 0; i < 100; i++) {
                long id = redisIdWorker.nextId("order");
                System.out.println("id = " + id);
            }
            latch.countDown();
        };
        long begin = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            es.submit(task);
        }
        latch.await();
        long end = System.currentTimeMillis();
        System.out.println("time = " + (end - begin));
    }


    @Test
    void createTokens(){
        // 获取用户手机号列表
        List<User> users = userMapper.selectList(new QueryWrapper<>());
        List<String> phoneNumbers = new ArrayList<>();
        for (User user : users) {
            phoneNumbers.add(user.getPhone());
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("E:\\token.txt"))) {
            for (String phoneNumber : phoneNumbers) {
                String token = (String) userService.login(new LoginFormDTO(phoneNumber,"",""),null).getData(); // 调用 login 方法
                writer.write(token);
                writer.newLine(); // 每个 token 单独一行
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void loadShopData() {
        // 1.查询店铺信息
        List<Shop> list = shopService.list();
        // 2.把店铺分组，按照typeId分组，typeId一致的放到一个集合
        Map<Long, List<Shop>> map = list.stream().collect(Collectors.groupingBy(Shop::getTypeId));
        // 3.分批完成写入Redis
        for (Map.Entry<Long, List<Shop>> entry : map.entrySet()) {
            // 3.1.获取类型id
            Long typeId = entry.getKey();
            String key = SHOP_GEO_KEY + typeId;
            // 3.2.获取同类型的店铺的集合
            List<Shop> value = entry.getValue();
            List<RedisGeoCommands.GeoLocation<String>> locations = new ArrayList<>(value.size());
            // 3.3.写入redis GEOADD key 经度 纬度 member
            for (Shop shop : value) {
                // stringRedisTemplate.opsForGeo().add(key, new Point(shop.getX(), shop.getY()), shop.getId().toString());
                locations.add(new RedisGeoCommands.GeoLocation<>(
                        shop.getId().toString(),
                        new Point(shop.getX(), shop.getY())
                ));
            }
            stringRedisTemplate.opsForGeo().add(key, locations);
        }
    }

}
