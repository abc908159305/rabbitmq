package workqueue;

import com.rabbitmq.client.*;
import utils.RabbitMQUtil;

import java.io.IOException;

public class Consumer1 {
    public static void main(String[] args) throws IOException {
        Connection connection = RabbitMQUtil.getConnection();
        Channel channel = connection.createChannel();
        channel.basicQos(1);//一次只能消费一个消息
        channel.queueDeclare("work",true,false,false,null);
        //参数2：消费者自动向rabbitmq确认消息消费，这里设置为false，避免消费者死掉后消息随之消失
        channel.basicConsume("work",false,new DefaultConsumer(channel){
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                System.out.println("消费者-1：" + new String(body));
                //手动确认消息消费完毕，参数1：确认标识 参数2：false每次确认一个
                channel.basicAck(envelope.getDeliveryTag(),false);
            }
        });
    }
}
