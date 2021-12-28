package utils;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
public class RabbitMQUtil {
    private static ConnectionFactory connectionFactory;
    static {
        connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("www.siyemao.xyz");
        connectionFactory.setPort(5672);
        connectionFactory.setVirtualHost("/mao");
        connectionFactory.setUsername("siye");
        connectionFactory.setPassword("siye");
    }
    //定义提供连接对象的方法
    public static Connection getConnection() {
        try {
            return connectionFactory.newConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    //定义关闭通道和关闭连接工具的方法
    public static void closeConnectionAndChanel(Channel channel,Connection connection){
        try{
            if (channel != null) channel.close();
            if (connection != null) connection.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
