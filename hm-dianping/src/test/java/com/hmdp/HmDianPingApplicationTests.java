package com.hmdp;

import com.hmdp.service.impl.ShopServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;


import javax.annotation.Resource;

@SpringBootTest
class HmDianPingApplicationTests {

    // 新增此方法，利用单元测试进行缓存预热
    @Resource
    private ShopServiceImpl shopService;

   @Test
    void testSaveShop(){
        shopService.saveShop2Redis(1L,10l);
    }

}
