import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Stack;

/**
 * work thread
 *
 * @author wben
 */
public class Handler implements Runnable {
    private Socket socket;
    BufferedReader br;
    BufferedWriter bw;
    PrintWriter pw;
    private Stack<File> rootDirStack = new Stack<>();

    public Handler(Socket socket, File rootDir) throws IOException {
        this.socket = socket;

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
                System.out.println(info);
                pw.println("you said:" + info);


                //todo
                if (info.equals("bye")) {
                    break;
                }
                else if (info.equals("ls")) {
                    getDirList();
                }else if (info.startsWith("cd ")){
                    enterTargetDir(info.substring(3));
                }
            }
        } catch (IOException e) {
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
     * @param file  带获取大小的文件
     * @return  文件和目录的大小
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
     * @param DirName
     * 文件目录名
     */
    private void enterTargetDir(String DirName) {
        File[] files = rootDirStack.peek().listFiles();
        if (DirName.equals("..")){
            if (rootDirStack.size()<=1){
                pw.println("已经在根目录下，无法退回子目录");
            }else {
                rootDirStack.pop();
            }
        }else {
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
                pw.println("unknown directory "+DirName);
            }
        }

        pw.println("end");
    }

}
