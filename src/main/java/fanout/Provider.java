package fanout;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import utils.RabbitMQUtil;

import java.io.IOException;

public class Provider {
    public static void main(String[] args) throws IOException {
        Connection connection = RabbitMQUtil.getConnection();
        Channel channel = connection.createChannel();
        //将通道指向交换机 //参数1：交换机名称 参数2：交换机类型
        channel.exchangeDeclare("logs","fanout");
        //发送消息 参数1：交换机名称 参数2：路由key，在广播模型中可以忽略 参数3：消息体
        channel.basicPublish("logs","",null,"fanout type messgae".getBytes());
        RabbitMQUtil.closeConnectionAndChanel(channel,connection);
    }
}
