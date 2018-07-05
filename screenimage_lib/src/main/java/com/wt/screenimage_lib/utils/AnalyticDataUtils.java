package com.wt.screenimage_lib.utils;

import android.util.Log;

import com.wt.screenimage_lib.entity.ReceiveData;
import com.wt.screenimage_lib.entity.ReceiveHeader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by wt on 2018/6/12.
 * 根据协议解析数据
 */
public class AnalyticDataUtils {
    private OnAnalyticDataListener mListener;


    /**
     * 分析头部数据
     */
    public ReceiveHeader analysisHeader(byte[] header) {
        //实现数组之间的复制
        //bytes：源数组
        //srcPos：源数组要复制的起始位置
        //dest：目的数组
        //destPos：目的数组放置的起始位置
        //length：复制的长度
        byte[] buff = new byte[4];
        System.arraycopy(header, 1, buff, 0, 4);
        final short mainCmd = ByteUtil.bytesToShort(buff);       //主指令  1`5
        buff = new byte[4];
        System.arraycopy(header, 5, buff, 0, 4);
        final short subCmd = ByteUtil.bytesToShort(buff);    //子指令  5`9
        buff = new byte[4];
        System.arraycopy(header, 9, buff, 0, 4);
        int stringBodyLength = ByteUtil.bytesToInt(buff);//文本数据 9 ~ 13;
        buff = new byte[4];
        System.arraycopy(header, 13, buff, 0, 4);
        int byteBodySize = ByteUtil.bytesToInt(buff);//byte数据 13^17
        return new ReceiveHeader(mainCmd, subCmd, header[0], stringBodyLength, byteBodySize);
    }


    // TODO: 2018/6/11 wt处理协议相应指令
    public ReceiveData analyticData(InputStream is, ReceiveHeader receiveHeader) throws IOException {
        byte[] sendBody = null;
        byte[] buff = null;
        //文本长度
        if (receiveHeader.getStringBodylength() != 0) {
            sendBody = readByte(is, receiveHeader.getStringBodylength());
        }

        if (receiveHeader.getBuffSize() != 0) {
            buff = readByte(is, receiveHeader.getBuffSize());
        }
        ReceiveData data = new ReceiveData();
        data.setHeader(receiveHeader);
        data.setSendBody(sendBody == null ? "" : new String(sendBody));
        data.setBuff(buff);
       if (mListener != null) mListener.onSuccess(data);
        return data;
    }

//    // TODO: 2018/7/5 wt 解析头数据
//    public ReceiveData synchAnalyticData(InputStream is, ReceiveHeader receiveHeader) throws IOException {
//        byte[] sendBody = null;
//        byte[] buff = null;
//        //文本长度
//        if (receiveHeader.getStringBodylength() != 0) {
//            sendBody = readByte(is, receiveHeader.getStringBodylength());
//        }
//        //音视频长度
//        if (receiveHeader.getBuffSize() != 0) {
//            buff = readByte(is, receiveHeader.getBuffSize());
//        }
//        ReceiveData data = new ReceiveData();
//        data.setHeader(receiveHeader);
//        data.setSendBody(sendBody == null ? "" : new String(sendBody));
//        Log.e("wtt", "analyticData: " + new String(sendBody));
//        data.setBuff(buff);
//        return data;
//    }

    /**
     * 保证从流里读到指定长度数据
     *
     * @param is
     * @param readSize
     * @return
     * @throws Exception
     */
    public static byte[] readByte(InputStream is, int readSize) throws IOException {
        byte[] buff = new byte[readSize];
        int len = 0;
        int eachLen = 0;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while (len < readSize) {
            eachLen = is.read(buff);
            if (eachLen != -1) {
                len += eachLen;
                baos.write(buff, 0, eachLen);
            } else {
                baos.close();
                throw new IOException("AnalyticDataUtils" + "   :tcp have diaconnect...");
            }
            if (len < readSize) {
                buff = new byte[readSize - len];
            }
        }
        byte[] b = baos.toByteArray();
        baos.close();
        return b;
    }


    public interface OnAnalyticDataListener {
        void onSuccess(ReceiveData data) ;

    }

    public void setOnAnalyticDataListener(OnAnalyticDataListener listener) {
        this.mListener = listener;
    }
}
