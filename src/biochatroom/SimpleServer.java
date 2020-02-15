package biochatroom;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

//BIO网络编程
public class SimpleServer {

    public static void main(String[] args) throws IOException {

        RequestHandler requestHandler = new RequestHandler();

        try (ServerSocket serverSocket = new ServerSocket(6666)) {
            System.out.println("Listening on " + serverSocket.getLocalSocketAddress());

            while (true) {
                Socket clientSocket = serverSocket.accept();  //第一处等待：等待用户连接       这就是阻塞！
                System.out.println("Incoming connection from " + clientSocket.getRemoteSocketAddress());

                new ClientHandler(clientSocket, requestHandler).run();
            }
        }
    }
}