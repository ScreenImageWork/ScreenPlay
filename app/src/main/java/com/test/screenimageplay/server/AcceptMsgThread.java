package com.test.screenimageplay.server;

import android.os.SystemClock;
import android.util.Log;

import com.test.screenimageplay.constant.ScreenImageApi;
import com.test.screenimageplay.entity.Frame;
import com.test.screenimageplay.entity.ReceiveData;
import com.test.screenimageplay.entity.ReceiveHeader;
import com.test.screenimageplay.server.interf.OnAcceptBuffListener;
import com.test.screenimageplay.server.interf.OnAcceptTcpStateChangeListener;
import com.test.screenimageplay.utils.AnalyticDataUtils;
import com.test.screenimageplay.utils.DecodeUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by wt on 2018/6/7.
 * 接收消息,执行操作线程
 */
public class AcceptMsgThread extends Thread implements AnalyticDataUtils.OnAnalyticDataListener {
    private InputStream InputStream;
    private OutputStream outputStream;
    private EncodeV1 mEncodeV1;
    private volatile boolean startFlag = true;
    private OnAcceptBuffListener listener;
    private OnAcceptTcpStateChangeListener mStateChangeListener;
    private DecodeUtils mDecoderUtils;
    private AnalyticDataUtils mAnalyticDataUtils;
    //当前投屏线程
    private AcceptMsgThread acceptMsgThread;
    private String TAG = "AcceptMsgThread";

    public AcceptMsgThread(InputStream is, OutputStream outputStream, EncodeV1 encodeV1, OnAcceptBuffListener
            listener, OnAcceptTcpStateChangeListener disconnectListenerlistener) {
        this.InputStream = is;
        this.outputStream = outputStream;
        this.mEncodeV1 = encodeV1;
        this.listener = listener;
        this.mStateChangeListener = disconnectListenerlistener;
        mDecoderUtils = new DecodeUtils();
        mAnalyticDataUtils = new AnalyticDataUtils();
        mAnalyticDataUtils.setOnAnalyticDataListener(this);
        mDecoderUtils.setOnVideoListener(new DecodeUtils.OnVideoListener() {
            @Override
            public void onSpsPps(byte[] sps, byte[] pps) {
                Frame spsPpsFrame = new Frame();
                spsPpsFrame.setType(Frame.SPSPPS);
                spsPpsFrame.setSps(sps);
                spsPpsFrame.setPps(pps);
                AcceptMsgThread.this.listener.acceptBuff(spsPpsFrame);
            }

            @Override
            public void onVideo(byte[] video, int type) {
                Frame frame = new Frame();
                switch (type) {
                    case Frame.KEY_FRAME:
                        frame.setType(Frame.KEY_FRAME);
                        frame.setBytes(video);
                        AcceptMsgThread.this.listener.acceptBuff(frame);
                        break;
                    case Frame.NORMAL_FRAME:
                        frame.setType(Frame.NORMAL_FRAME);
                        frame.setBytes(video);
                        AcceptMsgThread.this.listener.acceptBuff(frame);
                        break;
                    case Frame.AUDIO_FRAME:
                        frame.setType(Frame.AUDIO_FRAME);
                        frame.setBytes(video);
                        AcceptMsgThread.this.listener.acceptBuff(frame);
//                        Log.e("AcceptH264MsgThread", "audio frame ...");
                        break;
                    default:
                        Log.e("AcceptH264MsgThread", "other video...");
                        break;
                }

            }
        });
    }


    // TODO: 2018/6/14 向客户端回传初始化成功标识
    public void sendStartMessage(AcceptMsgThread acceptMsgThread) {
        this.acceptMsgThread=acceptMsgThread;
        //告诉客户端我已经初始化成功
        byte[] content = mEncodeV1.buildSendContent();
        try {
            outputStream.write(content);
            if (mStateChangeListener != null) {
                mStateChangeListener.acceptTcpConnect(acceptMsgThread);
            }

        } catch (IOException e) {
            if (mStateChangeListener != null) {
                mStateChangeListener.acceptTcpDisConnect(e,acceptMsgThread);
            }
        }
    }

    @Override
    public void run() {
        super.run();
        readMessage();
//        while (startFlag) {
//            //开始接收客户端发过来的数据
//            byte[] header = new byte[0];
//            try {
//                header = mAnalyticDataUtils.readByte(InputStream, 18);
//                //数据如果为空，则休眠，防止cpu空转,  0.0 不可能会出现的,会一直阻塞在之前
//                if (header.length == 0) {
//                    SystemClock.sleep(1);
//                    continue;
//                }
//                //根据协议分析数据头
//                ReceiveHeader receiveHeader = mAnalyticDataUtils.analysisHeader(header);
//                if (receiveHeader.getStringBodylength() == 0 && receiveHeader.getBuffSize() == 0) {
//                    SystemClock.sleep(1);
//                    continue;
//                }
//                if (receiveHeader.getEncodeVersion() != ScreenImageApi.encodeVersion1) {
//                    Log.e(TAG, "接收到的数据格式不对...");
//                    continue;
//                }
//                //TODO 可以处理主指令和子指令操作
//                Log.e("wtt", "run: " + receiveHeader.getEncodeVersion());
//                //解析拆分数据
//                mAnalyticDataUtils.analyticData(InputStream, receiveHeader);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
//        }
    }

    // TODO: 2018/6/14 去读取数据
    private void readMessage() {
        try {
            while (startFlag) {
                //开始接收客户端发过来的数据
                byte[] header = mAnalyticDataUtils.readByte(InputStream, 18);
                //数据如果为空，则休眠，防止cpu空转,  0.0 不可能会出现的,会一直阻塞在之前
                if (header.length == 0) {
                    SystemClock.sleep(1);
                    continue;
                }
                //根据协议分析数据头
                ReceiveHeader receiveHeader = mAnalyticDataUtils.analysisHeader(header);
                if (receiveHeader.getStringBodylength() == 0 && receiveHeader.getBuffSize() == 0) {
                    SystemClock.sleep(1);
                    continue;
                }
                if (receiveHeader.getEncodeVersion() != ScreenImageApi.encodeVersion1) {
                    Log.e(TAG, "接收到的数据格式不对...");
                    continue;
                }
                operation(receiveHeader, InputStream);
            }
        } catch (Exception e) {
            if (mStateChangeListener != null) {
                mStateChangeListener.acceptTcpDisConnect(e, acceptMsgThread);
            }
        } finally {
            startFlag = false;
        }
    }

    // TODO: 2018/6/12 根据指令处理相关事务
    private void operation(ReceiveHeader receiveHeader, InputStream InputStream) throws IOException {
        //主：投屏
        if (receiveHeader.getMainCmd() == 0xA2) {
            switch (receiveHeader.getSubCmd()) {
                //解析音视频播放
                case 0x01:
                    //解析拆分帧数据
                    mAnalyticDataUtils.analyticData(InputStream, receiveHeader);
                    break;
            }
        }
    }


    // TODO: 2018/6/14 中止线程
    public void shutdown() {
        startFlag = false;
        //中断非阻塞状态线程
        this.interrupt();
    }


    // TODO: 2018/6/14 从解析成功后回调过来
    @Override
    public void onSuccess(ReceiveData data) {
        if (data == null) {
            return;
        }
        //区分音视频
        mDecoderUtils.isCategory(data.getBuff());
    }

}
