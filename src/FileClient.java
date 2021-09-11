import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class FileClient {
    static final int TCP_PORT = 9999;
    static final String HOST = "127.0.0.1";
    Socket socket;

    public FileClient() throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(HOST, TCP_PORT));
    }

    public static void main(String[] args) throws IOException {
        new FileClient().send();
    }

    public void send() {
        try {
            //init io stream
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter pw = new PrintWriter(bw, true);
            Scanner in = new Scanner(System.in); //接受用户信息
            String msg = null;
            while ((msg = in.nextLine()) != null) {
                pw.println(msg); //发送给服务器端
                //输出服务器返回的消息
                String str = null;
                while ((str = br.readLine()) != null) {
                    if (str.equals("end")){
                        break;
                    }
                    System.out.println(str);
                }
                System.out.println("---------");
                if (msg.equals("bye")) {
                    break; //退出
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
