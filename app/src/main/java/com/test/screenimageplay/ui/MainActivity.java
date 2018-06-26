package com.test.screenimageplay.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.tbruyelle.rxpermissions2.RxPermissions;
import com.test.screenimageplay.R;
import com.test.screenimageplay.constant.ScreenImageApi;
import com.test.screenimageplay.core.BaseActivity;
import com.test.screenimageplay.decode.DecodeThread;
import com.test.screenimageplay.entity.Frame;
import com.test.screenimageplay.mediacodec.VIdeoMediaCodec;
import com.test.screenimageplay.server.AcceptMsgThread;
import com.test.screenimageplay.server.NormalPlayQueue;
import com.test.screenimageplay.server.TcpServer;
import com.test.screenimageplay.server.interf.OnAcceptBuffListener;
import com.test.screenimageplay.server.interf.OnAcceptTcpStateChangeListener;
import com.test.screenimageplay.utils.AboutIpUtils;
import com.test.screenimageplay.utils.ToastUtils;
import com.uuzuche.lib_zxing.activity.CodeUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import butterknife.BindView;

public class MainActivity extends BaseActivity implements OnAcceptTcpStateChangeListener, OnAcceptBuffListener {

    @BindView(R.id.iv_code)
    ImageView ivCode;
    @BindView(R.id.sf_view)
    SurfaceView sfView;
    @BindView(R.id.rl_code)
    RelativeLayout rlCode;

    private SurfaceHolder mSurfaceHolder;
    private FileOutputStream fos;
    private VIdeoMediaCodec videoMediaCodec;
    private DecodeThread mDecodeThread;
    private NormalPlayQueue mPlayqueue;
    private TcpServer mTcpServer;
    private String TAG = "wtt";
    private Context mContext;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    rlCode.setVisibility(View.GONE);
                    sfView.setVisibility(View.VISIBLE);
                    break;
                case 2:
                    rlCode.setVisibility(View.VISIBLE);
                    sfView.setVisibility(View.GONE);
                    break;
            }
        }
    };

    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void initView() {
        mContext = this;
        initialFIle();
        startServer();
        //surface保证他们进行交互，当surface销毁之后，surfaceholder断开surface及其客户端的联系
        mSurfaceHolder = sfView.getHolder();
    }


    @Override
    protected void initData() {
        RxPermissions rxPermissions = new RxPermissions(this);
        String[] permissions = {
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE};
        rxPermissions
                .requestEach(permissions)
                .subscribe(permission -> { // will emit 2 Permission objects
                    if (permission.granted) {
                        Log.e("wtt", "accept: 同意");
                    } else if (permission.shouldShowRequestPermissionRationale) {
                        ToastUtils.showShort(mContext, "拒绝权限，等待下次询问哦");

                    } else {
                        startAppSettings();
                        ToastUtils.showShort(mContext, "拒绝权限，不再弹出询问框，请前往APP应用设置中打开此权限");
                    }
                });
        //获取本机ip
        if (TextUtils.isEmpty(AboutIpUtils.getIPAddress(mContext))) {
            Log.e(TAG, "initData: xxx");
            ToastUtils.showShort(mContext, "请先设置网络");
            return;
        }
        Log.e(TAG, "initData: xxx" + AboutIpUtils.getIPAddress(mContext));
        //以ip生成二维码
        Bitmap bitmap = CodeUtils.createImage(AboutIpUtils.getIPAddress(mContext), 500, 500,
                null);
        if (bitmap == null) {
            return;
        }
        rlCode.setVisibility(View.VISIBLE);
        ivCode.setImageBitmap(bitmap);
        //监听surface的生命周期
        mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                //Surface创建时激发，一般在这里调用画面的线程
                initialMediaCodec(holder);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                //Surface的大小发生改变时调用。
            }

            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                //销毁时激发，一般在这里将画面的线程停止、释放。
                if (videoMediaCodec != null) videoMediaCodec.release();
            }
        });
    }

    // TODO: 2018/6/12 wt用于本地测试
    private void initialFIle() {
        //本地生成一个音频文件
        File file = new File(Environment.getExternalStorageDirectory(), "test.aac");
        if (file.exists()) {
            file.delete();
        }
        try {
            file.createNewFile();
            fos = new FileOutputStream(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startServer() {
        //开启服务
        mPlayqueue = new NormalPlayQueue();
        mTcpServer = new TcpServer();
        //发送初始化成功指令
        mTcpServer.setBackpassBody(ScreenImageApi.SERVER.MAIN_CMD,
                ScreenImageApi.SERVER.INITIAL_SUCCESS, "初始化成功", new byte[0]);
        mTcpServer.setOnAccepttBuffListener(this);
        mTcpServer.setOnTcpConnectListener(this);
        mTcpServer.startServer();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void initialMediaCodec(SurfaceHolder holder) {
        //初始化解码器
        videoMediaCodec = new VIdeoMediaCodec(holder);
        //开启解码线程
        mDecodeThread = new DecodeThread(videoMediaCodec.getCodec(), mPlayqueue);
        videoMediaCodec.start();
        mDecodeThread.start();
    }

    //Tcp连接状态的回调...
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void acceptTcpConnect(AcceptMsgThread acceptMsgThread) {
        //接收到客户端的连接...
        Log.e(TAG, "接收到客户端的连接...");
        mTcpServer.setacceptTcpConnect(acceptMsgThread);
        Message msg = new Message();
        msg.what = 1;
        mHandler.sendMessage(msg);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void acceptTcpDisConnect(Exception e, AcceptMsgThread acceptMsgThread, boolean updateUI) {
        //客户端的连接断开...
        Log.e(TAG, "客户端的连接断开..." + e.toString());
//        if (videoMediaCodec != null) videoMediaCodec.release();

        mTcpServer.setacceptTcpDisConnect(acceptMsgThread);
        if (updateUI) {
            //更新页面
            Message msg = new Message();
            msg.what = 2;
            mHandler.sendMessage(msg);
        }
    }

    //接收到关于不同帧类型的回调
    @Override
    public void acceptBuff(Frame frame) {
        //存入缓存
        mPlayqueue.putByte(frame);
    }

    // TODO: 2018/6/25 去权限设置页
    private void startAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + mContext.getPackageName()));
        startActivityForResult(intent, 100);
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void finish() {
        super.finish();
        if (mPlayqueue != null) mPlayqueue.stop();
        if (videoMediaCodec != null) videoMediaCodec.release();
        if (mDecodeThread != null) mDecodeThread.shutdown();
        if (mTcpServer != null) mTcpServer.stopServer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
    }

}
