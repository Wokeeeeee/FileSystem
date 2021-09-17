import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;

public class FileClient {
    static final int TCP_PORT = 9999;
    static final int UDP_PORT = 8888;
    static final String HOST = "127.0.0.1";
    Socket socket;
    DatagramSocket udpClient;

    public FileClient() throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(HOST, TCP_PORT));
        udpClient = new DatagramSocket();
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
            //tcp发送消息
            String msg = null;
            while ((msg = in.nextLine()) != null) {
                if (msg.startsWith("bye")) {
                    pw.println(msg);
                    break; //退出
                }
                else if (msg.startsWith("get ")) {
                    //用udp
                    connectByUDP();
                    pw.println(msg);
                    String str = null;
                    while ((str = br.readLine()) != null) {
                        if (str.equals("end")) {
                            break;
                        }
                        else if (str.startsWith("fileInfo:")) {
                            String all[] = str.substring(9).split("\\s+");
                            String name=all[0];
                            int size=Integer.parseInt(all[1]);
                            System.out.println("开始接收文件，文件名为：" + name + "文件大小为：" + size);
                            System.out.println("----------------");
                            downloadFileByUDP(name,size);
                        }else {
                            System.out.println(str);
                        }
                    }
                }
                else {
                    pw.println(msg); //发送给服务器端
                    //tcp消息接收
                    //输出服务器返回的消息
                    String str = null;
                    while ((str = br.readLine()) != null) {
                        if (str.equals("end")) {
                            break;
                        }
                        System.out.println(str);
                    }
                    System.out.println("---------");
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

    /**
     * udp connect
     * @throws IOException
     */
    private void connectByUDP() throws IOException {
        //先向服务器发送一段信息，让服务器知道客户端的IP信息
        byte[] info = "hello world!".getBytes();
        SocketAddress socketAddres = new InetSocketAddress(HOST, UDP_PORT);
        DatagramPacket dp_send = new DatagramPacket(info, info.length,
                socketAddres);
        udpClient.send(dp_send);

    }

    /**
     * 根据文件大小 下载客户端发送的文件
     * @param fileName
     * @param fileLength
     * @throws IOException
     */
    private void downloadFileByUDP(String fileName, int fileLength) throws IOException {
        File file = new File(fileName);
        FileOutputStream fout = new FileOutputStream(file);
        byte[] recvBuffer = new byte[1024];
        DatagramPacket recvPacket = new DatagramPacket(recvBuffer,
                recvBuffer.length);
        if (file.exists()) {
            file.delete();
        }
        int begin_point = 0;
        while (begin_point < fileLength) {
            int write_length;
            if (fileLength - begin_point > recvPacket.getData().length) {
                write_length = recvPacket.getData().length;
            }
            else {
                write_length = fileLength - begin_point;
            }
            udpClient.receive(recvPacket);
//            System.out.println(new String(recvPacket.getData()));
            fout.write(recvPacket.getData(), 0, write_length);
            begin_point += recvPacket.getData().length;
        }
        System.out.println("文件传输成功，文件名为:" + fileName + "   文件路径为:" + file.getAbsolutePath());
    }
}
