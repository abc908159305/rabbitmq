package direct;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import utils.RabbitMQUtil;

import java.io.IOException;

public class Provider {
    public static void main(String[] args) throws IOException {
        Connection connection = RabbitMQUtil.getConnection();
        Channel channel = connection.createChannel();
        //通过通道声明交换机，参数1：交换机名称 参数2：路由模式
        channel.exchangeDeclare("logs_direct","direct");
        //发送消息
        String routerKey = "info";
        channel.basicPublish("logs_direct",routerKey,null,("这是direct模型发布的基于RouterKey：[" + routerKey + "]发送的消息").getBytes());
        RabbitMQUtil.closeConnectionAndChanel(channel,connection);
    }
}
