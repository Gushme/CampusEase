package com.CampusEase.config;

import com.CampusEase.entity.VoucherOrder;
import com.CampusEase.service.IVoucherOrderService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.io.IOException;

/**
 * ClassName: RabbitMQConfig
 * Package: com.CampusEase.config
 * Description:
 *
 * @Author Gush
 * @Create 2024/12/11 10:32
 */
@Configuration
public class RabbitMQConfig {
    @Autowired
    @Lazy
    private IVoucherOrderService voucherOrderService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "direct.seckill.queue"),
            key = "direct.seckill",
            exchange = @Exchange(name = "CampusEase.direct", type = ExchangeTypes.DIRECT)
    ))
    public void recieveMessage(Message message, Channel channel, VoucherOrder voucherOrder){
        try {
            voucherOrderService.handleVoucherOrder(voucherOrder);
            // 创建完成时手动 ack
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("监听到了" + message);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

}
