package com.example.bluetooth.le;

import android.util.Log;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

/**
 * Log工具类
 * Created by lost on 2016/4/11.
 */
public class LogUtils {

    private LogUtils() {
        throw new UnsupportedOperationException("cannot be instantiated");
    }

    public static boolean isDebug = true;// 是否需要打印log

    private static final String TAG = "JINAPATIENT";

    // 下面四个是默认tag的函数
    public static void i(String msg) {
        if (isDebug)
            Log.i(TAG, msg);
    }

    public static void d(String msg) {
        if (isDebug)
            Log.d(TAG, msg);
    }

    public static void e(String msg) {
        if (isDebug)
            Log.e(TAG, msg);
    }

    public static void v(String msg) {
        if (isDebug)
            Log.v(TAG, msg);
    }

    // 下面是传入自定义tag的函数
    public static void i(String tag, String msg) {
        if (isDebug)
            Log.i(tag, msg);
    }

    public static void d(String tag, String msg) {
        if (isDebug)
            Log.i(tag, msg);
    }

    public static void e(String tag, String msg) {
        if (isDebug)
            Log.i(tag, msg);
    }

    public static void v(String tag, String msg) {
        if (isDebug)
            Log.i(tag, msg);
    }

    /**
     * 过长的Log信息保存到本地目录
     * @param s
     * @param name
     */
    public static void put(String s, String name)
    {
        try
        {
            FileOutputStream outStream = new FileOutputStream("/sdcard/"+"JINA"+  name +".txt",true);
            OutputStreamWriter writer = new OutputStreamWriter(outStream,"utf-8");
            writer.write(s);
            writer.write("/n");
            writer.flush();
            writer.close();//记得关闭

            outStream.close();
        }
        catch (Exception e)
        {
            Log.e("m", "file write error");
        }
    }

}
