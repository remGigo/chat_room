package niochatroom;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

/**
 * 客户端线程类，专门接收服务器端响应信息
 */
public class NioClientHandler implements Runnable {

    private Selector selector;

    public NioClientHandler(Selector selector) {
        this.selector = selector;
    }

    @Override
    public void run() {
        try {
            for (;;) {
                int readyChannels = selector.select();

                if (readyChannels == 0) continue;

                Set<SelectionKey> selectionKeys = selector.selectedKeys();//获取已就绪channel的集合

                Iterator<SelectionKey> iterator = selectionKeys.iterator();

                while (iterator.hasNext()) {

                    SelectionKey selectionKey = iterator.next();//selectionKey实例

                    iterator.remove();//移除Set中的当前selectionKey

                    //根据就绪状态，调用对应方法处理业务逻辑
                    if (selectionKey.isReadable())   //如果是 可读事件
                        readHandler(selectionKey, selector);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 可读事件处理器
     */
    private void readHandler(SelectionKey selectionKey, Selector selector) throws IOException {

        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();//要从 selectionKey 中获取到已经就绪的channel

        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);//创建buffer

        String response = "";
        while (socketChannel.read(byteBuffer) > 0) {//循环读取服务器端响应信息

            byteBuffer.flip();//切换buffer为读模式

            response += StandardCharsets.UTF_8.decode(byteBuffer);//读取buffer中的内容  emmm提示我改成StringBiulder
        }

        socketChannel.register(selector, SelectionKey.OP_READ);//将channel再次注册到selector上，并监听其可读事件

        if (response.length() > 0)    //将服务器端响应信息打印到本地
            System.out.println(response);
    }
}
