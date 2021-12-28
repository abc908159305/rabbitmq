package helloworld;

import com.rabbitmq.client.*;
import utils.RabbitMQUtil;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Consumer {
    public static void main(String args[]) throws IOException, TimeoutException {
        //获取连接工厂
        Connection connection = RabbitMQUtil.getConnection();
        //创建通道
        Channel channel = connection.createChannel();
        //通道绑定对象
        channel.queueDeclare("hello",true,false,false,null);
        //消费消息
        //参数1：消费哪个队列 队列名称
        //参数2：开启消息的自动确认机制
        //参数3：消费时的回调接口
        channel.basicConsume("hello",true,new DefaultConsumer(channel){
            @Override//最后一个参数：消息队列中取出的消息
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                System.out.println("获取消息 " + new String(body));
            }
        });
    }
}
