package topic;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import utils.RabbitMQUtil;

import java.io.IOException;

public class Provider {
    public static void main(String[] args) throws IOException {
        Connection connection = RabbitMQUtil.getConnection();
        Channel channel = connection.createChannel();
        //声明交换机和交换机类型
        channel.exchangeDeclare("topics","topic");
        //发布消息
        String routerKey = "user.save.info";
        channel.basicPublish("topics",routerKey,null,("这里是topic动态路由模型，routerkey：[" + routerKey + "]发布的消息").getBytes());
        RabbitMQUtil.closeConnectionAndChanel(channel,connection);
    }
}
