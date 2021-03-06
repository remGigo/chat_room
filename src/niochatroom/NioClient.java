package niochatroom;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * NIO客户端
 */
public class NioClient {

    //main函数也不用了，提供start()方法供所有客户调用就行了
//    public static void main(String[] args) throws IOException {
//        new NioClient().start("mxm");
//    }
    /**
     * 启动
     */
    public void start(String nickname) throws IOException {

        //连接服务器端
        SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("127.0.0.1", 8000));

        //接收服务器端响应。  新开线程，专门负责来接收服务器端的响应数据
        Selector selector = Selector.open();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_READ);
        new Thread(new NioClientHandler(selector)).start();

        //向服务器端发送数据
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            String request = scanner.nextLine();
            if (request != null && request.length() > 0)
                socketChannel.write(                     //channel是非阻塞的，流是阻塞的
                        StandardCharsets.UTF_8.encode(nickname + " : " + request));
        }
    }
}
