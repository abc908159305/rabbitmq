package helloworld;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import org.junit.Test;
import utils.RabbitMQUtil;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Provider {
    public static void main(String args[]) throws IOException, TimeoutException {
        Connection connection = RabbitMQUtil.getConnection();
        //通过连接获取通道
        Channel channel = connection.createChannel();
        //通道绑定对应消息队列
        //参数1：队列名称，如果队列不存在自动创建
        //参数2：队列是否持久化，true持久化队列，false不持久化
        //参数3：是否独占队列，true独占队列，false不独占
        //参数4：是否再消费完成后自动删除队列
        //参数5：额外附加参数
        channel.queueDeclare("hello",true,false,false,null);
        //发布消息
        //参数1：交换机名称，参数2：队列名称，参数3：传递消息额外设置(如果想要消息持久化，则使用MessageProperties.PERSISTENT_TEXT_PLAIN)，参数4：消息具体类容
        channel.basicPublish("","hello", MessageProperties.PERSISTENT_TEXT_PLAIN,"hello world".getBytes());
        RabbitMQUtil.closeConnectionAndChanel(channel,connection);
    }
}
