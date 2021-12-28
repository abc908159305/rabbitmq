package direct;

import com.rabbitmq.client.*;
import utils.RabbitMQUtil;

import java.io.IOException;

public class Consumer2 {
    public static void main(String[] args) throws IOException {
        Connection connection = RabbitMQUtil.getConnection();
        Channel channel = connection.createChannel();
        channel.exchangeDeclare("logs_direct","direct");
        //创建临时队列
        String queueName = channel.queueDeclare().getQueue();
        //基于routerkey绑定交换机
        channel.queueBind(queueName,"logs_direct","info");
        channel.queueBind(queueName,"logs_direct","error");
        channel.queueBind(queueName,"logs_direct","warning");
        //消费消息
        channel.basicConsume(queueName,true,new DefaultConsumer(channel){
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                System.out.println("消费者2：" + new String(body));
            }
        });
    }
}
