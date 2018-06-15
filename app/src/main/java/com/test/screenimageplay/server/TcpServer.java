package com.test.screenimageplay.server;

import android.util.Log;


import com.test.screenimageplay.server.interf.OnAcceptBuffListener;
import com.test.screenimageplay.server.interf.OnAcceptTcpStateChangeListener;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created wt
 * Date on  2018/5/28
 *
 * @Desc 创建服务监听
 */

public class TcpServer {
    private ServerSocket serverSocket;
    private int tcpPort = 11111;
    private boolean isAccept = true;
    private EncodeV1 mEncodeV1;
    private OnAcceptBuffListener mListener;
    private OnAcceptTcpStateChangeListener mConnectListener;
    private AcceptMsgThread acceptMsgThread;
    //把线程给添加进来
    private List<AcceptMsgThread> acceptMsgThreadList;
    private boolean isConnect = false;

    public TcpServer() {
        this.acceptMsgThreadList = new ArrayList<>();
    }

    public void startServer() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    // 创建一个ServerSocket对象，并设置监听端口
                    serverSocket = new ServerSocket();
                    serverSocket.setReuseAddress(true);
                    InetSocketAddress socketAddress = new InetSocketAddress(tcpPort);
                    serverSocket.bind(socketAddress);
                    acceptMsgThreadList.clear();
                    while (isAccept) {
                        //服务端接收客户端的连接请求
                        Socket socket = serverSocket.accept();
                        //开启接收消息线程
                        acceptMsgThread = new AcceptMsgThread(socket.getInputStream(),
                                socket.getOutputStream(), mEncodeV1, mListener, mConnectListener);
                        acceptMsgThread.start();
                        //把线程添加到集合中去
                        acceptMsgThreadList.add(acceptMsgThread);
                        Log.e("123", "run: " + acceptMsgThreadList.size());
                        if (acceptMsgThreadList.size() != 0) {
                            //默认先发送成功标识给第一个客户端
                            if (!isConnect) {
                                isConnect = true;
                                acceptMsgThreadList.get(0).sendStartMessage();
                            }
                        }

                    }
                } catch (Exception e) {
                    Log.e("TcpServer", "" + e.toString());
                }

            }
        }.start();
    }

    /**
     * TODO: 2018/6/12 wt像客户端回传消息
     *
     * @param mainCmd    主指令
     * @param subCmd     子指令
     * @param sendBody   文本内容
     * @param sendBuffer byte内容
     */
    public void setBackpassBody(int mainCmd, int subCmd, String sendBody,
                                byte[] sendBuffer) {
        mEncodeV1 = new EncodeV1(mainCmd, subCmd, sendBody, sendBuffer);
    }

    public void setOnAccepttBuffListener(OnAcceptBuffListener listener) {
        this.mListener = listener;
    }

    public void setOnTcpConnectListener(OnAcceptTcpStateChangeListener listener) {
        this.mConnectListener = listener;
    }

    public void stopServer() {
        this.mListener = null;
        isAccept = false;
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    if (acceptMsgThread != null) {
                        acceptMsgThread.shutdown();
                    }
                    if (serverSocket != null) {
                        serverSocket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    // TODO: 2018/6/15 wt 连接中逻辑
    public void setacceptTcpConnect(AcceptMsgThread acceptMsgThread) {
        //投屏正在连接
        Log.e("123", "acceptTcpConnect: zzz");
//        isConnect = true;
    }

    // TODO: 2018/6/15 wt连接断开逻辑
    public void setacceptTcpDisConnect(AcceptMsgThread acceptMsgThread) {
        //连接断开
        boolean remove = acceptMsgThreadList.remove(acceptMsgThread);
        Log.e("123", "移除成功" + remove + "acceptTcpDisConnect: 个数" + acceptMsgThreadList.size());
        if (acceptMsgThreadList == null || acceptMsgThreadList.size() == 0) {
            return;
        }
        //如果停止的不是正在投屏的线程，就不再去走下面的方法
        if (acceptMsgThread != acceptMsgThreadList.get(0)) {
            return;
        }
        acceptMsgThreadList.get(0).sendStartMessage();

    }


}
