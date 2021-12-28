package com.siye.rabbitmq.hello;

import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
//声明并创建队列
@RabbitListener(queuesToDeclare = @Queue(value = "hello"))
//也可以设置队列是否持久化、是否自动删除等参数
//@RabbitListener(queuesToDeclare = @Queue(value = "hello",durable = "false",autoDelete = "true"))
public class HelloConsumer {
    //具体接收消息的方法
    @RabbitHandler
    public void receivel(String message){
        System.out.println("message: " + message);
    }
}
