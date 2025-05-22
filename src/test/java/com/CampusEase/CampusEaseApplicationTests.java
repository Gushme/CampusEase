package com.CampusEase;

import com.CampusEase.service.impl.ShopServiceImpl;
import com.CampusEase.utils.RedisIdWorker;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
class CampusEaseApplicationTests {

    @Autowired
    private ShopServiceImpl shopService;

    @Autowired
    private RedisIdWorker redisIdWorker;

    @Test
    public void testSaveShop(){
        shopService.saveShop2Redis(1L, 10L);
    }

    private ExecutorService es = Executors.newFixedThreadPool(500);
    @Test
    public void testIdWorker() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(300);
        Runnable task = () -> {
            for (int i = 0; i < 100; i++) {
                System.out.println(redisIdWorker.nextId("order"));
            }
            countDownLatch.countDown();
        };

        // 30000个id
        long begin = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            es.submit(task);
        }
        countDownLatch.await();
        long end = System.currentTimeMillis();
        System.out.println("time = " + (end - begin));
    }

    @Resource
    RabbitTemplate rabbitTemplate;
    @Test
    public void testSendMessage(){
        rabbitTemplate.convertAndSend("CampusEase.direct","direct.seckill","测试发送消息");
    }

    @Test
    public void testGPTDemo() {
        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com/v1")
                .apiKey("sk-e17abee33b214d4795f551c5694614e3") //设置模型apiKey
                .modelName("deepseek-chat") //设置模型名称
                .build();
        //向模型提问
        String answer = model.chat("你是什么模型");
        //输出结果
        System.out.println(answer);
    }
}
