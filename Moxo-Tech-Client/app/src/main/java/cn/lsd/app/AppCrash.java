package cn.lsd.app;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Looper;

import cn.lsd.app.util.FileUtils;
import cn.lsd.app.util.OSUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;


public class AppCrash extends Exception implements UncaughtExceptionHandler {

    private final static boolean Debug = false;//是否保存错误日志

    /**
     * 定义异常类型
     */
    public final static byte TYPE_NETWORK = 0x01;
    public final static byte TYPE_SOCKET = 0x02;
    public final static byte TYPE_HTTP_CODE = 0x03;
    public final static byte TYPE_HTTP_ERROR = 0x04;
    public final static byte TYPE_XML = 0x05;
    public final static byte TYPE_IO = 0x06;
    public final static byte TYPE_RUN = 0x07;
    public final static byte TYPE_JSON = 0x08;

    private byte type;
    private int code;

    /**
     * 系统默认的UncaughtException处理类
     */
    private UncaughtExceptionHandler mDefaultHandler;

    private AppCrash() {
        this.mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    private AppCrash(byte type, int code, Exception excp) {
        super(excp);
        this.type = type;
        this.code = code;
        if (Debug) {//dfgz
            this.saveLog(excp);
        }
    }

    public int getCode() {
        return this.code;
    }

    public int getType() {
        return this.type;
    }

    /**
     * 提示友好的错误信息
     *
     * @param ctx
     */
    public void makeToast(Context ctx) {
//		switch(this.getType()){
//		case TYPE_HTTP_CODE:
//			String err = ctx.getString(R.string.http_status_code_error, this.getCode());
//			Toast.makeText(ctx, err, Toast.LENGTH_SHORT).show();
//			break;
//		case TYPE_HTTP_ERROR:
//			Toast.makeText(ctx, R.string.http_exception_error, Toast.LENGTH_SHORT).show();
//			break;
//		case TYPE_SOCKET:
//			Toast.makeText(ctx, R.string.socket_exception_error, Toast.LENGTH_SHORT).show();
//			break;
//		case TYPE_NETWORK:
//			Toast.makeText(ctx, R.string.network_not_connected, Toast.LENGTH_SHORT).show();
//			break;
//		case TYPE_XML:
//			Toast.makeText(ctx, R.string.xml_parser_failed, Toast.LENGTH_SHORT).show();
//			break;
//		case TYPE_JSON:
//			Toast.makeText(ctx, R.string.xml_parser_failed, Toast.LENGTH_SHORT).show();
//			break;
//		case TYPE_IO:
//			Toast.makeText(ctx, R.string.io_exception_error, Toast.LENGTH_SHORT).show();
//			break;
//		case TYPE_RUN:
//			Toast.makeText(ctx, R.string.app_run_code_error, Toast.LENGTH_SHORT).show();
//			break;
//		}
    }

    /**
     * 保存异常日志
     *
     * @param excp
     */
    public void saveLog(Exception excp) {
        String errorlog = "errorlog.txt";
        String savePath = "";
        String logFilePath = "";
        FileWriter fw = null;
        PrintWriter pw = null;
        try {
            //判断是否挂载了SD卡
            if (OSUtil.hasSdcard()) {
                savePath = AppConstant.LOG_PATH;
                File file = new File(savePath);
                if (!file.exists()) {
                    file.mkdirs();
                }
                logFilePath = savePath + errorlog;
            } else {
                return;
            }


            File logFile = new File(logFilePath);
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
            fw = new FileWriter(logFile, true);
            pw = new PrintWriter(fw);
            pw.println("--------------------" + (new Date().toLocaleString()) + "---------------------");
            excp.printStackTrace(pw);
            pw.close();
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            FileUtils.closeQuietly(pw, fw);
        }

    }

    public static AppCrash http(int code) {
        return new AppCrash(TYPE_HTTP_CODE, code, null);
    }

    public static AppCrash http(Exception e) {
        return new AppCrash(TYPE_HTTP_ERROR, 0, e);
    }

    public static AppCrash socket(Exception e) {
        return new AppCrash(TYPE_SOCKET, 0, e);
    }

    public static AppCrash io(Exception e) {
        if (e instanceof UnknownHostException || e instanceof ConnectException) {
            return new AppCrash(TYPE_NETWORK, 0, e);
        } else if (e instanceof IOException) {
            return new AppCrash(TYPE_IO, 0, e);
        }
        return run(e);
    }

    public static AppCrash xml(Exception e) {
        return new AppCrash(TYPE_XML, 0, e);
    }

    public static AppCrash json(Exception e) {
        return new AppCrash(TYPE_JSON, 0, e);
    }

    public static AppCrash network(Exception e) {
        if (e instanceof UnknownHostException || e instanceof ConnectException) {
            return new AppCrash(TYPE_NETWORK, 0, e);
        }
        //else if(e instanceof HttpException){
        //	return http(e);
        //}
        else if (e instanceof SocketException) {
            return socket(e);
        }
        return http(e);
    }

    public static AppCrash run(Exception e) {
        return new AppCrash(TYPE_RUN, 0, e);
    }

    /**
     * 获取APP异常崩溃处理对象
     *
     * @param context
     * @return
     */
    public static AppCrash getAppExceptionHandler() {
        return new AppCrash();
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {

        if (!handleException(ex) && mDefaultHandler != null) {
            mDefaultHandler.uncaughtException(thread, ex);
        }

    }


    private boolean handleException(Throwable ex) {
        if (ex == null) {
            return false;
        }

        final Context context = AppManager.getAppManager().currentActivity();

        if (context == null) {
            return false;
        }

        final String crashMsg = getCrashReport(context, ex);
        new Thread() {
            public void run() {
                Looper.prepare();
                //send crash report
                Looper.loop();
            }

        }.start();
        return true;
    }


    private String getCrashReport(Context context, Throwable ex) {
        PackageInfo pinfo = ((AppContext) context.getApplicationContext()).getPackageInfo();
        StringBuffer exceptionStr = new StringBuffer();
        exceptionStr.append("Version: " + pinfo.versionName + "(" + pinfo.versionCode + ")\n");
        exceptionStr.append("Android: " + android.os.Build.VERSION.RELEASE + "(" + android.os.Build.MODEL + ")\n");
        exceptionStr.append("Exception: " + ex.getMessage() + "\n");
        StackTraceElement[] elements = ex.getStackTrace();
        for (int i = 0; i < elements.length; i++) {
            exceptionStr.append(elements[i].toString() + "\n");
        }
        return exceptionStr.toString();
    }
}
