### 一、写在前面

- 什么是消息队列？

顾名思义，消息队列就是一个能够存放消息的队列，通常有一个生产者生产消息，一个或多个消费者消费消息。

消息队列在分布式系统中运用十分广泛，有异步处理、应用解耦、流量削峰等用途。

当然，RabbitMQ不是消息队列的唯一选择，除它以外还有在大数据中十分常见的Kafka、阿里的RocketMQ、Apache的ActiveMQ，甚至Redis也可以当成消息队列使用。

不过，正常情况下RabbitMQ是比较通用的选择。

- RabbitMQ基础模型

![image-20211227213012001](http://www.siyemao.xyz/images/1640611813402.png)

上面是rabbitmq的基础模型，可以看到Server包裹着虚拟主机，一般来说，虚拟主机和用户相对应，一个Rabbitmq可以有多个虚拟主机、多个用户，以对应不同的系统不同的功能模块。

此外，虚拟主机还包含着交换机和数个队列，生产者会将消息交给交换机，然后队列通过某种规则从交换机中取得消息，再由消费者拿到消息。当然，生产者是否要将消息交给交换机，取决于使用了哪种消息模型。

### 二、消息模型

- 1.HelloWorld（直连）模型。

  一个生产者生产消息交给队列，消费者通过队列取得消息，非常简单的一对一消息模型。

  首先编写一个获取RabbitMQ工厂对象的工具类：

  ```
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
  ```

  接着再创建生产者类和消费者类：

  ```
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
  ```

  ```
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
  ```

  运行生产者类，然后再运行消费者类，就可以看到，消费者类拿到生产者的消息了~

- 2.work quene（工作队列）模型

  一个消费者消费的速度还赶不上生产的速度，GDP上不来，那怎么办，创造多个消费者好了。

  一样还是有一个生产者：

  ```
  public class Provider {
      public static void main(String[] args) throws IOException {
          //获取连接
          Connection connection = RabbitMQUtil.getConnection();
          Channel channel = connection.createChannel();
          //通过通道声明队列
          channel.queueDeclare("work", true, false, false, null);
          //生产消息
          for (int i = 0; i < 10; i++) {
              channel.basicPublish("", "work", null, (i+"hello workqueue").getBytes());
          }
          //关闭资源
          RabbitMQUtil.closeConnectionAndChanel(channel, connection);
      }
  }
  ```

  不同的是有两个消费者：

  ```
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
  ```

  ```
  public class Consumer2 {
      ……代码跟Consumer1一样
  }
  ```

  可以看到，和直连模型相比，工作模型并没有多出什么额外的东西，只是新增了一个消费者而已。

  需要注意的是，为了避免消费者拿到消息后死掉，使消息也丢失，我们需要将通道设置为每次只能消费一个消息（channel.basicQos(1)），同时将消费消息时的第二个参数设置为false，取消自动确认，改为在消费完毕后手动确认。

  另外。

  默认情况下，消费者们采用轮询的方式获取消息，你一条我一条，哪怕其中一个消费者效率很慢也是这样。但按照上述设置后，就可以实现能者多劳的效果。

- 3.Fanout（广播）模型

  在广播模型中，可以有多个消费者，每个消费者有自己的临时队列，生产者的消息交给交换机，交换机来决定交给哪个消费者的队列。一般情况下所有的消费者都能拿到消息，实现一条消息被多个消费者消费。

  同样有一个生产者，但是生产者不声明队列，而是声明并指向交换机：

  ```
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
  ```

  消费者同样绑定交换机，并创建临时队列，临时队列绑定交换机：

  ```
  public class Consumer1 {
      public static void main(String[] args) throws IOException {
          Connection connection = RabbitMQUtil.getConnection();
          Channel channel = connection.createChannel();
          //通道绑定交换机
          channel.exchangeDeclare("logs","fanout");
          //创建临时队列
          String queueName = channel.queueDeclare().getQueue();
          //绑定交换机和队列
          channel.queueBind(queueName,"logs","");
          //消费消息
          channel.basicConsume(queueName,true,new DefaultConsumer(channel){
              @Override
              public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                  System.out.println("消费者-1：" + new String(body));
              }
          });
      }
  }
  ```

  消费者2、消费者3都是同样的代码，这里就省略掉了。

  此时启动消费者，然后再启动生产者，就能看到所有消费者都拿到了同一条消息。

- Routing（路由）模型

  在广播模型中，所有消费者都能拿到消息，但是如果我们不想让他拿到呢？如果我们想要不同的消费去拿不同类型的消息呢？这个时候就能用到路由模型了。

  - 4.路由之订阅模型-Direct（直连/订阅制）

    在直连模型下，队列不能直接绑定交换机了，而是要指定一个routerkey，消息发送方在向交换机发送消息时也必须指定routerkey，这样交换机在把消息交给队列时，就会通过routerkey进行判断，实现”订阅制“。

    和上面的所有模型一样，首先创建一个生产者：

    ```
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
    ```

    可以看到，这次指定了模式为direct路由模式，同时指定routerkey为info。

    创建消费者1和消费者2：

    ```
    public class Consumer1 {
        public static void main(String[] args) throws IOException {
            Connection connection = RabbitMQUtil.getConnection();
            Channel channel = connection.createChannel();
            channel.exchangeDeclare("logs_direct","direct");
            //创建临时队列
            String queueName = channel.queueDeclare().getQueue();
            //基于routerkey绑定交换机
            channel.queueBind(queueName,"logs_direct","error");
            //消费消息
            channel.basicConsume(queueName,true,new DefaultConsumer(channel){
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    System.out.println("消费者1：" + new String(body));
                }
            });
        }
    }
    ```

    ```
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
    ```

    很明显的，消费者1只指定了一个routerkey：error，而消费者2则是有多个routerkey：info、error、warning……如果此时运行消费者1和2，然后运行生产者，消费者1空手而归，而消费者2就会拿到消息。这是因为2绑定了和生产者一样的routerkey：info。

  - 5.Routing路由模型之Topics（动态路由）模型

    在direct订阅模型中，如果需要接受多种消息，就要指定多个routerkey，如果数量一多就很很麻烦，这种情况就能使用topics模型。

    ```
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
    ```

    ```
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
    ```

    可以看到，消费者能够通过*或#通配符来动态指定routerkey，来匹配生产者user.info.xx这种形式，这样就比direct模型要灵活强大很多。

    注：*匹配一个词，#匹配多个。

- 6.RPC模型

  RPC模型并不适用于消息队列，而是用于RPC通信，所以这里就暂时不进行学习了。

- 7.PublisherConfirms（发布和订阅模型）

  这是新出的一种模型，并没有得到广泛使用，也暂时不学习了。

### 三、Spirngboot整合RabbitMQ

和jdbc一样，rabbitmq的这种原始方式虽然可用，但是同样会产生大量样板代码，所以Springboot封装了一套东西供我们使用（所以上面这么多内容时闹什么呢……）。

- 1.初来乍到Hello直连模型

  使用RabbitTemplate可以轻松发送消息：

  ```
  @SpringBootTest(classes = RabbitMQApplication.class)
  @RunWith(SpringRunner.class)
  public class TestRabbitMQ {
      @Autowired
      private RabbitTemplate rabbitTemplate;
      @Test
      public void test(){
          rabbitTemplate.convertAndSend("hello","helloworld");
      }
  }
  ```

  在消费者类上使用注解@RabbitListener可以监听消息，具体使用如下：

  ```
  @Component
  //声明并创建队列
  @RabbitListener(queuesToDeclare = @Queue(value = "hello"))
  //也可以设置队列是否持久化、是否自动删除等参数
  //@RabbitListener(queuesToDeclare = @Queue(value = "hello",durable = "false",autoDelete = "true"))
  public class HelloConsumer {
      //具体接收消息的方法
      @RabbitHandler
      public void receivel(String message){
          System.out.println("message: " + message);
      }
  }
  ```

  

- 2.Work模型

  @RabbitListener不止可以用在类上，还可以用在方法上，表明这个方法就是一个消费者，这样就能造出多个消费者，实现Work模型了。

  ```
  //work模型
      @Test
      public void testWork(){
          for (int i = 0;i < 10;i++){
              rabbitTemplate.convertAndSend("work","work模型" + i);
          }
      }
  ```

  ```
  @Component
  public class WorkConsumer {
      @RabbitListener(queuesToDeclare = @Queue("work"))
      public void receiver1(String message){
          System.out.println("消费者1：" + message);
      }
      @RabbitListener(queuesToDeclare = @Queue("work"))
      public void receiver2(String message){
          System.out.println("消费者2：" + message);
      }
  }
  ```

- 3.Fanout广播模型

  ```
  //Fanout模型
      @Test
      public void testFanout(){
          rabbitTemplate.convertAndSend("logs","","Fanout广播模型");
      }
  ```

  ```
  @Component
  public class FanoutConsumer {
      @RabbitListener(bindings = {
              @QueueBinding(value = @Queue,//创建临时队列
                      exchange = @Exchange(value = "logs",type = "fanout"))//绑定交换机
      })
      public void receive1(String message){
          System.out.println("消费者1：" + message);
      }
      @RabbitListener(bindings = {
              @QueueBinding(value = @Queue,//创建临时队列
                      exchange = @Exchange(value = "logs",type = "fanout"))//绑定交换机
      })
      public void receive2(String message){
          System.out.println("消费者2：" + message);
      }
  }
  ```

  

- 4.路由direct模型

  ```
      //路由direct模型
      @Test
      public void testDirect(){
          rabbitTemplate.convertAndSend("directs","info","路由direct模型");
      }
  ```

  ```
  @Component
  public class DirectConsumer {
      @RabbitListener(bindings = {
              @QueueBinding(
                      value = @Queue,
                      exchange = @Exchange(value = "directs",type = "direct"),
                      key = {"info","error","warn"}
              )
      })
      public void receiv1(String message){
          System.out.println("消费者1：" + message);
      }
      @RabbitListener(bindings = {
              @QueueBinding(
                      value = @Queue,
                      exchange = @Exchange(value = "directs",type = "direct"),
                      key = {"error"}
              )
      })
      public void receiv2(String message){
          System.out.println("消费者2：" + message);
      }
  }
  ```

- 5.动态路由topic模型

  ```
  //动态路由topic模型
      @Test
      public void testTopic(){
          rabbitTemplate.convertAndSend("topics","user.save","动态路由topic模型");
      }
  ```

  ```
  @Component
  public class TopicConsumer {
      @RabbitListener(bindings = {
              @QueueBinding(
                      value = @Queue,
                      exchange = @Exchange(type = "topic",name = "topics"),
                      key = {"user.save","user.*"}
              )
      })
      public void receive1(String message){
          System.out.println("消费者1：" + message);
      }
      @RabbitListener(bindings = {
              @QueueBinding(
                      value = @Queue,
                      exchange = @Exchange(type = "topic",name = "topics"),
                      key = {"user.delete"}
              )
      })
      public void receive2(String message){
          System.out.println("消费者2：" + message);
      }
  }
  ```

  

### 四、RabbitMQ集群17

待更新……

