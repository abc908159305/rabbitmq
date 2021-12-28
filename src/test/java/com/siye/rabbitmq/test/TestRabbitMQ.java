package com.siye.rabbitmq.test;

import com.siye.rabbitmq.RabbitMQApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(classes = RabbitMQApplication.class)
@RunWith(SpringRunner.class)
public class TestRabbitMQ {
    @Autowired
    private RabbitTemplate rabbitTemplate;
    //基础直连模型
    @Test
    public void testHello(){
        rabbitTemplate.convertAndSend("hello","helloworld");
    }
    //work模型
    @Test
    public void testWork(){
        for (int i = 0;i < 10;i++){
            rabbitTemplate.convertAndSend("work","work模型" + i);
        }
    }
    //Fanout模型
    @Test
    public void testFanout(){
        rabbitTemplate.convertAndSend("logs","","Fanout广播模型");
    }
    //路由direct模型
    @Test
    public void testDirect(){
        rabbitTemplate.convertAndSend("directs","info","路由direct模型");
    }
    //动态路由topic模型
    @Test
    public void testTopic(){
        rabbitTemplate.convertAndSend("topics","user.save","动态路由topic模型");
    }
}
