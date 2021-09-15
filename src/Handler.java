import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Stack;
import java.util.concurrent.TimeUnit;

/**
 * work thread
 *
 * @author wben
 */
public class Handler implements Runnable {
    private Socket socket;
    private DatagramSocket udpServer;
    BufferedReader br;
    BufferedWriter bw;
    PrintWriter pw;
    BufferedReader fr;
    private Stack<File> rootDirStack = new Stack<>();

    public Handler(Socket socket, File rootDir, DatagramSocket datagramSocket) throws IOException {
        this.socket = socket;
        this.udpServer = datagramSocket;
        //检查根目录是否合格
        if (rootDir.isFile()) {
            throw new IOException("根目录必须为文件夹");
        }
        try {
            rootDir = rootDir.getCanonicalFile();
            rootDirStack.push(rootDir);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 初始化 io
     *
     * @throws IOException
     */
    public void initStream() throws IOException {
        br = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        bw = new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        pw = new PrintWriter(bw, true);
    }

    public void run() {
        try {
            System.out.println("新连接，连接地址：" + socket.getInetAddress() + "："
                    + socket.getPort());
            initStream();
            Runtime runtime = Runtime.getRuntime();

            String info = null;
            while (null != (info = br.readLine())) {
                System.out.println(socket.getInetAddress() + ":" + socket.getPort() + ">  " + info);


                //todo
                if (info.equals("bye")) {
                    System.out.println("结束连接: " + socket.getInetAddress() + ":" + socket.getPort());
                    break;
                }
                else if (info.equals("ls")) {
                    getDirList();
                }
                else if (info.startsWith("cd ")) {
                    enterTargetDir(info.substring(3));
/*                    String all[]=info.substring(3).split("\\s+");
                    for (String str:all){
                        System.out.println(str);
                        enterTargetDir(str);
                    }*/
                }
                else if (info.startsWith("get ")) {
//                    sendFileByUDP(info.substring(4));
                    getFile(info.substring(4));
                }
                else {
                    pw.println("unknown command, please enter again");
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (null != socket) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 获取文件目录 ls
     */
    private void getDirList() {
        File files[] = rootDirStack.peek().listFiles();

        for (File file : files) {
            String str = null;
            if (file.isDirectory()) {
                str = String.format("%-10s", "<dir>") + String.format("%-40s", file.getName()) + "  " + getDirSize(file);
            }
            else {
                str = String.format("%-10s", "<file>") + String.format("%-40s", file.getName()) + "  " + getDirSize(file);
            }
            pw.println(str);

        }
        pw.println("end");
    }

    /**
     * 用于获得文件大小
     *
     * @param file 带获取大小的文件
     * @return 文件和目录的大小
     */
    private long getDirSize(File file) {
        if (file.isFile()) {
            return file.length();//如果是文件夹，返回长度
        }
        File[] subFiles = file.listFiles();
        long counter = 0;
        if (subFiles != null) {
            for (File tmp : subFiles) {
                counter += getDirSize(tmp);//迭代计算
            }
        }
        return counter;
    }

    /**
     * 用于执行cd命令
     *
     * @param DirName 文件目录名
     */
    private void enterTargetDir(String DirName) {
        File[] files = rootDirStack.peek().listFiles();
        if (DirName.equals("..")) {
            if (rootDirStack.size() <= 1) {
                pw.println("已经在根目录下，无法退回子目录");
            }
            else {
                rootDirStack.pop();
            }
        }
        else {
            boolean isExist = false;
            for (File file : files) {
                if (DirName.equals(file.getName())) {
                    if (file.isDirectory()) {
                        rootDirStack.push(file);
                        pw.println("cd " + DirName + " >OK");
                        isExist = true;
                    }
                    else {
                        pw.println(DirName + "不是一个目录");
                    }
                }

            }
            if (!isExist) {
                pw.println("unknown directory " + DirName);
            }
        }

        pw.println("end");
    }

    private void getFile(String cmd) throws IOException, InterruptedException {
        //先从客户端获得客户端ip和port信息
        byte[] recvBuffer = new byte[1024];
        DatagramPacket recvPacket = new DatagramPacket(recvBuffer,
                recvBuffer.length);
        udpServer.receive(recvPacket); //byte[] recvBuffer = new byte[1024];

        String all[] = cmd.split("\\s+");

        for (String str : all) {
            System.out.println(str);
            sendFileByUDP(str, recvPacket);
        }
        pw.println("end");
    }

    private void sendFileByUDP(String FileName, DatagramPacket recvPacket) throws IOException, InterruptedException {


        File[] files = rootDirStack.peek().listFiles();
        boolean isExist = false;
        for (File file : files) {
            if (file.getName().equals(FileName)) {
                isExist = true;
                //用tcp尝试传输
                System.out.println("server: 文件存在，开始传输");
                fr = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                String str = null;
                //udp发送
                byte[] data = Files.readAllBytes(Path.of(file.getPath()));
                pw.println("fileInfo:" + FileName + "  " + data.length);
                byte[] sendBuffer = new byte[1024];
                int begin_point = 0;
                while (begin_point < data.length) {
                    int send_length;
                    if (data.length - begin_point > sendBuffer.length) {
                        send_length = sendBuffer.length;
                    }
                    else {
                        send_length = data.length - begin_point;
                    }
                    System.arraycopy(data, begin_point, sendBuffer, 0, send_length);
                    TimeUnit.MICROSECONDS.sleep(1);
                    udpServer.send(new DatagramPacket(
                            sendBuffer, sendBuffer.length, recvPacket.getAddress(), recvPacket.getPort()
                    ));
                    begin_point += send_length;
                }
                /*//tcp发送
                while ((str = fr.readLine()) != null) {
                    pw.println(str);
                }*/
                pw.println("传输成功");
                System.out.println("server: 传输成功");
            }
        }
        if (!isExist) {
            pw.println("该目录下不存在该文件，请重新输入文件名。");
        }
//        pw.println("end");
    }

}
