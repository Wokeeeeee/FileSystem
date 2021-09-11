

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class FileServerMultiThread {
    ServerSocket serverSocket;
    private final int PORT = 9999;
    private final File rootDir;

    public FileServerMultiThread(File rootDir) throws IOException {
        if (!rootDir.isDirectory()) {
            throw new IOException(rootDir + "不是目录");
        }
        serverSocket = new ServerSocket(PORT);
        System.out.println("服务器启动。");
        this.rootDir = rootDir;
    }

    public static void main(String[] args) throws IOException {
        File root;
        System.out.println("根目录参数：>"+args[0]);
        root = new File(args[0]);
        new FileServerMultiThread(root).servic();
    }


    public void servic() {
        while (true) {
            Socket socket = null;
            try {
                socket = serverSocket.accept();
                Thread work = new Thread(new Handler(socket,this.rootDir));

                work.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
