package topic;

import com.rabbitmq.client.*;
import utils.RabbitMQUtil;

import java.io.IOException;

public class Consumer1 {
    public static void main(String[] args) throws IOException {
        Connection connection = RabbitMQUtil.getConnection();
        Channel channel = connection.createChannel();
        //声明交换机
        channel.exchangeDeclare("topics","topic");
        //创建临时队列
        String queueName = channel.queueDeclare().getQueue();
        //绑定交换机和队列 动态通配符形式的routerkey
        channel.queueBind(queueName,"topics","user.#");
        channel.basicConsume(queueName,true,new DefaultConsumer(channel){
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                System.out.println("消费者1：" + new String(body));
            }
        });
    }
}
