package workqueue;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import utils.RabbitMQUtil;

import java.io.IOException;

public class Provider {
    public static void main(String[] args) throws IOException {
        //获取连接
        Connection connection = RabbitMQUtil.getConnection();
        Channel channel = connection.createChannel();
        //通过通道声明队列
        channel.queueDeclare("work", true, false, false, null);
        //生产消息
        for (int i = 0; i < 40; i++) {
            channel.basicPublish("", "work", null, (i+"hello workqueue").getBytes());
        }
        //关闭资源
        RabbitMQUtil.closeConnectionAndChanel(channel, connection);
    }
}
