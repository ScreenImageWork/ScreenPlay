package com.wt.screenimage_lib;

import android.app.Application;
import android.content.Context;
import android.os.Handler;

import com.wt.screenimage_lib.server.tcp.TcpServer;
import com.wt.screenimage_lib.server.tcp.interf.OnAcceptBuffListener;
import com.wt.screenimage_lib.server.tcp.interf.OnAcceptTcpStateChangeListener;

import java.util.ArrayList;


/**
 * Created by xu.wang
 * Date on  2018/7/3 18:49:43.
 *
 * @Desc 录屏控制类
 */

public class ScreenImageController {
    protected Handler mHandler;
    protected Context mContext;
    protected TcpServer tcpServer;
    private static ScreenImageController mController;
    public ArrayList<OnAcceptTcpStateChangeListener> mList = new ArrayList<>();

    private ScreenImageController() {
    }

    public static ScreenImageController getInstance() {
        synchronized (ScreenImageController.class) {
            if (mController == null) {
                mController = new ScreenImageController();
            }
        }
        return mController;
    }


    public ScreenImageController init(Application application) {
        mHandler = new Handler(application.getMainLooper());
        mContext = application;
        return mController;
    }

    public ScreenImageController startServer() {
        if (tcpServer == null) {
            tcpServer = new TcpServer();
        }
        tcpServer.startServer();
        return mController;
    }

    public ScreenImageController stopServer() {
        if (tcpServer != null) tcpServer.stopServer();
        return mController;
    }

    public ScreenImageController setOnAcceptBuffListener(OnAcceptBuffListener listener) {
        if (tcpServer != null) tcpServer.setOnAccepttBuffListener(listener);
        return mController;
    }

    public void setOnAcceptTcpStateChangeListener(OnAcceptTcpStateChangeListener listener) {
        if (mList.contains(listener)) {
            return;
        }
        mList.add(listener);
    }

    public void removeOnAcceptTcpStateChangeListener(OnAcceptTcpStateChangeListener listener) {
        mList.remove(listener);
    }
}
