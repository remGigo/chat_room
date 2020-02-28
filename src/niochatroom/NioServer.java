package niochatroom;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

/**
 * NIO服务器端
 */
public class NioServer {

    public static void main(String[] args) throws IOException {
        new NioServer().start();
    }

    /**
     *启动服务器
     */
    public void start() throws IOException {

        Selector selector = Selector.open();                    //1. 创建Selector

        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();//2. 创建服务器端channel通道

        serverSocketChannel.bind(new InetSocketAddress(8000));//3. 为服务器端channel通道绑定监听端口

        serverSocketChannel.configureBlocking(false);              //4. 设置服务器端channel为非阻塞模式

        //A selection key is created each time a channel is registered with a selector.
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);//5. 将服务器端channel注册到selector上，监听连接事件
        System.out.println("服务器启动成功！");

        for (;;) { // 一看就是写过go的哈哈                         //6. 循环等待新接入的连接

            //这是核心方法，具体实现OS底层对IO的支持不同而不同。
            int readyChannels = selector.select();              //作用是获取已就绪channel数量(这是一个阻塞方法~ )

            if (readyChannels == 0) continue;                         //为什么要这样？好像是因为原生的NIO有点bug

            Set<SelectionKey> selectionKeys = selector.selectedKeys();//selector牛逼这就让我取到这个装有 所有已就绪channel 的集合了

            Iterator<SelectionKey> iterator = selectionKeys.iterator();//set的iterator
            //左面Iterator加了泛型下面.next就不用强转成SelectionKey了
            while (iterator.hasNext()) {

                SelectionKey selectionKey = iterator.next(); //selectionKey实例
                iterator.remove();           //移除Set中的当前selectionKey，不移除也行，还省了后面再注册啦了

                //7. 根据就绪状态，selector调用对应方法处理业务逻辑。注意两种情况传参不一样！
                //你瞧是一个线程吧
                if (selectionKey.isAcceptable())    //喂，那个线程 我要求接入
                    acceptHandler(serverSocketChannel, selector);//调用acceptHandler处理事件

                if (selectionKey.isReadable())      //喂，那个线程 你可以读我这个channel的数据了
                    readHandler(selectionKey, selector);//调用readHandler处理事件
            }
        }
    }


    /**
     * 接入事件处理器。    BIO一上来就直接是要干这个事儿了，不然NIO比它复杂和强大到哪儿了
     * 通过ServerSocketChannel创建与客户端连接的SocketChannel，并将其也注册到selector上并监控它的可读事件
     */
    private void acceptHandler(ServerSocketChannel serverSocketChannel,Selector selector) throws IOException {

        //如果是BIO，就是Socket socket = serverSocket.accept();
        SocketChannel socketChannel = serverSocketChannel.accept();

        socketChannel.configureBlocking(false);       //也要将获取到的客户端的socketChannel设置为非阻塞工作模式

        socketChannel.register(selector, SelectionKey.OP_READ);  //将客户端channel注册到selector上，监听 可读事件

        socketChannel.write(StandardCharsets.UTF_8.encode("你与聊天室里其他人都不是朋友关系，请注意隐私安全"));//channel就好比流~
    }

    /**
     * 可读事件处理器
     */
    private void readHandler(SelectionKey selectionKey, Selector selector) throws IOException {

        //Returns the channel for which this key was created.      注册一个channel就生成一个与之对应的selectionKey嘛
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();  //从selectionKey中获取到已经就绪的channel

        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);//创建buffer 因为只有buffer才能操作channel的读和写！！！

        String request = "";
        while (socketChannel.read(byteBuffer) > 0) {//使用buffer循环读取客户端请求信息(channel中数组)

            byteBuffer.flip();  //切换buffer为读模式，这是读客户已经写入到buffer中的数据了啊

            request += StandardCharsets.UTF_8.decode(byteBuffer);//读取buffer中的内容
        }

        socketChannel.register(selector, SelectionKey.OP_READ);//将channel再次注册到selector上，依然监听其可读事件

        if (request.length() > 0)    //将客户端发送的请求信息 广播给其他客户端
            broadCast(selector, socketChannel, request);
            //System.out.println("有一条消息："+request);
    }

    /**
     * 广播给其他客户端
     */
    private void broadCast(Selector selector, SocketChannel sourceChannel, String request) {

        //获取到所有已接入的客户端channel     用的是.keys()方法，而不是.selectedKeys()方法
        Set<SelectionKey> selectionKeySet = selector.keys();//Returns this selector's key set.

        //循环向所有channel广播信息
        selectionKeySet.forEach(selectionKey -> {
            Channel targetChannel = selectionKey.channel();

            // 剔除发消息的客户端
            if (targetChannel instanceof SocketChannel && targetChannel != sourceChannel) {
                try {
                    ((SocketChannel) targetChannel).write(StandardCharsets.UTF_8.encode(request));// 将信息发送到targetChannel客户端
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
