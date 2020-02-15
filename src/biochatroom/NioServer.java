package biochatroom;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;


//具体NIO解耦实现见模块niochatroom
public class NioServer {

    public static void main(String[] args) throws IOException {

        ServerSocketChannel serverChannel = ServerSocketChannel.open(); //之前是new的ServerSocket

        serverChannel.configureBlocking(false);//在异步操作中，所有的操作都是不会被block的
        serverChannel.bind(new InetSocketAddress(8888));

        System.out.println("Listening on " + serverChannel.getLocalAddress());

        Selector selector = Selector.open();//设置好端口号后还不能马上accept，因为是非阻塞，所以要加个selector
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);//把serverChannel注册到selector里去，selector会汇报这个channel能否被accept

        ByteBuffer buffer = ByteBuffer.allocate(1024);
        RequestHandler requestHandler = new RequestHandler();
        while (true) {
            int selected = selector.select();
            if (selected == 0) {
                continue;
            }

            Set<SelectionKey> selectedKeys = selector.selectedKeys();//获得就绪的channel集合
            Iterator<SelectionKey> keyIter = selectedKeys.iterator();

            while (keyIter.hasNext()) {//遍历
                SelectionKey key = keyIter.next();//拿到的key就是我们选择出的有数据的东东

                if (key.isAcceptable()) {//每连上一个用户，我们在selector中加一个client的SocketChannel
                    ServerSocketChannel channel = (ServerSocketChannel) key.channel();
                    SocketChannel clientChannel = channel.accept();  //这下才可以accept~
                    System.out.println("Incoming connection from " + clientChannel.getRemoteAddress());
                    //直接调accept的话一定返回null，因为服务器刚启动没有client会进来，但我们通过select让它汇报出来的东西可以被accept
                    clientChannel.configureBlocking(false);
                    clientChannel.register(selector, SelectionKey.OP_READ);
                }

                if (key.isReadable()) {//每次用户有输入，我们就读输入，并且将结果写到对应的client的SocketChannel
                    SocketChannel channel = (SocketChannel) key.channel();
                    channel.read(buffer);
                    String request = new String(buffer.array()).trim();
                    buffer.clear();

                    System.out.println(String.format(
                            "Request from %s: %s",
                            channel.getRemoteAddress(),
                            request));
                    String response = requestHandler.handle(request);
                    channel.write(ByteBuffer.wrap(response.getBytes()));
                }

                keyIter.remove();
            }
        }
    }
}
