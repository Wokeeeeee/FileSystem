

import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class FileServerThreadPool {
    ExecutorService executorService; // 线程池
    final int POOL_SIZE = 6; // 单个处理器线程池工作线程数目
    ServerSocket serverSocket;
    DatagramSocket udpServer;
    private final int PORT = 9999;
    private final int UDP_PORT = 8888;
    private final File rootDir;

    public FileServerThreadPool(File rootDir) throws IOException {
        if (!rootDir.isDirectory()) {
            throw new IOException(rootDir + "不是目录");
        }
        serverSocket = new ServerSocket(PORT);
        udpServer = new DatagramSocket(UDP_PORT);
        this.rootDir = rootDir;
        //线程池创建 定长线程池
        executorService = Executors.newFixedThreadPool(Runtime.getRuntime()
                .availableProcessors() * POOL_SIZE);
        System.out.println("服务器启动。");
    }

    public static void main(String[] args) throws IOException {
        File root;
        System.out.println("根目录参数：>" + args[0]);
        root = new File(args[0]);
        new FileServerThreadPool(root).servic();
    }

    /**
     * service implements
     */
    public void servic() {
        while (true) {
            Socket socket = null;
            try {
                socket = serverSocket.accept();
                Thread work = new Thread(new Handler(socket, this.rootDir, udpServer));

                work.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
