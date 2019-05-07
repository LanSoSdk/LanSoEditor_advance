package com.example.advanceDemo.utils;

import android.text.format.DateFormat;
import android.util.Log;

import com.lansosdk.box.LanSoEditorBox;
import com.lansosdk.videoeditor.VideoEditor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;

/**
 * 我们提供的用来 当app异常抛出时， 用来获取当前异常的打印信息， 并保存到代码中设置的位置。 使用方法， 可以参考我们的调用。
 */
public class LanSoSdkCrashHandler implements UncaughtExceptionHandler {

    private static final String TAG = "LanSoSdkCrashHandler";

    private UncaughtExceptionHandler defaultUEH;

    public LanSoSdkCrashHandler() {
        this.defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {

        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);

        // Inject some info about android version and the device, since google
        // can't provide them in the developer console
        StackTraceElement[] trace = ex.getStackTrace();
        StackTraceElement[] trace2 = new StackTraceElement[trace.length + 5];
        System.arraycopy(trace, 0, trace2, 0, trace.length);
        trace2[trace.length + 0] = new StackTraceElement("Android",
                "LanSongMODEL", android.os.Build.MODEL, -1);
        trace2[trace.length + 1] = new StackTraceElement("Android",
                "LanSongVERSION", android.os.Build.VERSION.RELEASE, -1);
        trace2[trace.length + 2] = new StackTraceElement("Android",
                "LanSongFINGERPRINT", android.os.Build.FINGERPRINT, -1);

        trace2[trace.length + 3] = new StackTraceElement("Android",
                "LanSong box version:", LanSoEditorBox.VERSION_BOX, -1);
        trace2[trace.length + 4] = new StackTraceElement("Android",
                "LanSong editor version:", VideoEditor.getSDKVersion(), -1);

        ex.setStackTrace(trace2);

        ex.printStackTrace(printWriter);
        String stacktrace = result.toString();
        printWriter.close();
        Log.e(TAG, stacktrace);

        // // Save the log on SD card if available
        /**
         * 打开下面这几行, 说明把获取到的错误信息存放到SD卡中.
         */
        // if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
        // {
        // String sdcardPath =
        // Environment.getExternalStorageDirectory().getPath();
        // writeLog(stacktrace, sdcardPath + "/lansongsdk_crash");
        // writeLogcat(sdcardPath + "/lansongsdk_logcat");
        // }

        defaultUEH.uncaughtException(thread, ex);
    }

    private void writeLog(String log, String name) {
        CharSequence timestamp = DateFormat.format("yyyyMMdd_kkmmss",
                System.currentTimeMillis());
        String filename = name + "_" + timestamp + ".log";

        FileOutputStream stream;
        try {
            stream = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        OutputStreamWriter output = new OutputStreamWriter(stream);
        BufferedWriter bw = new BufferedWriter(output);

        try {
            bw.write(log);
            bw.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(bw);
            close(output);
        }
    }

    private void writeLogcat(String name) {
        CharSequence timestamp = DateFormat.format("yyyyMMdd_kkmmss",
                System.currentTimeMillis());
        String filename = name + "_" + timestamp + ".log";
        try {
            writeLogcat2(filename);
        } catch (IOException e) {
            Log.e(TAG, "Cannot write logcat to disk");
        }
    }

    private boolean close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
                return true;
            } catch (IOException e) {
                return false;
            }
        } else {
            return false;
        }
    }

    public void writeLogcat2(String filename) throws IOException {
        String[] args = {"logcat", "-v", "time", "-d"};

        Process process = Runtime.getRuntime().exec(args);

        InputStreamReader input = new InputStreamReader(
                process.getInputStream());

        FileOutputStream fileStream;
        try {
            fileStream = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
            return;
        }

        OutputStreamWriter output = new OutputStreamWriter(fileStream);
        BufferedReader br = new BufferedReader(input);
        BufferedWriter bw = new BufferedWriter(output);

        try {
            String line;
            while ((line = br.readLine()) != null) {
                bw.write(line);
                bw.newLine();
            }
        } catch (Exception e) {
        } finally {
            close(bw);
            close(output);
            close(br);
            close(input);
        }
    }
}
