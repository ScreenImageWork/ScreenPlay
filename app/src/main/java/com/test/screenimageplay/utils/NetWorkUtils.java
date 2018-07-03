package com.test.screenimageplay.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.test.screenimageplay.constant.Constants;

import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by wt on 2018/6/29.
 * 网络检查工具类
 */
public class NetWorkUtils {
    /**
     * 检查网络是否可用
     */
    public static boolean isNetConnected(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo info = connectivity.getActiveNetworkInfo();
            if (info != null && info.isConnected()) {
                // 当前网络是连接的
                if (info.getState() == NetworkInfo.State.CONNECTED) {
                    // 当前所连接的网络可用
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 检测wifi是否连接
     */
    public static boolean isWifiConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检测GPRS是否连接
     */
    public static boolean isGPRSConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断当前是否使用的是 WIFI网络
     *
     * @return
     */
    public static boolean isWifiActive(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] info;
        if (connectivity != null) {
            info = connectivity.getAllNetworkInfo();
            if (info != null) {
                for (int i = 0; i < info.length; i++) {
                    if (info[i].getTypeName().equals("WIFI")
                            && info[i].isConnected()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 判断网址是否有效
     */
    public static boolean isLinkAvailable(String link) {
        String regex = "^(http://|https://)?((?:[A-Za-z0-9]+-[A-Za-z0-9]+|[A-Za-z0-9]+)\\.)+([A-Za-z]+)[/\\?\\:]?.*$";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(link);
        return matcher.matches();
    }

    /**
     * 判断热点开启状态
     */
    public static boolean isWifiApEnabled(Context context) {
        return getWifiApState(context) == WIFI_AP_STATE.WIFI_AP_STATE_ENABLED;
    }

    private static WIFI_AP_STATE getWifiApState(Context context) {
        int tmp;
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        try {
            Method method = wifiManager.getClass().getMethod("getWifiApState");
            tmp = ((Integer) method.invoke(wifiManager));
            // Fix for Android 4
            if (tmp > 10) {
                tmp = tmp - 10;
            }
            return WIFI_AP_STATE.class.getEnumConstants()[tmp];
        } catch (Exception e) {
            e.printStackTrace();
            return WIFI_AP_STATE.WIFI_AP_STATE_FAILED;
        }
    }

    enum WIFI_AP_STATE {
        WIFI_AP_STATE_DISABLING, WIFI_AP_STATE_DISABLED, WIFI_AP_STATE_ENABLING, WIFI_AP_STATE_ENABLED, WIFI_AP_STATE_FAILED
    }

    /**
     * 获取ip
     *
     * @return
     */
    public static String getIp(Context context) {
        String ip = "";
        ConnectivityManager connMgr = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiInfo = connMgr
                .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiInfo.isAvailable() && wifiInfo.isConnected()) {
            // 获取本机ip
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            ip = intIP2StringIP(wifiManager.getConnectionInfo().getIpAddress());
        } else {
            ToastUtils.showShort(context, "当前网络不可用,请检查网络设置!");
        }
        return ip;
    }

    /**
     * 将得到的int类型的IP转换为String类型
     *
     * @param ip int类型ip地址
     * @return String类型ip地址
     */
    private static String intIP2StringIP(int ip) {
        return (ip & 0xFF) + "." +
                ((ip >> 8) & 0xFF) + "." +
                ((ip >> 16) & 0xFF) + "." +
                (ip >> 24 & 0xFF);
    }

    public static String intIP2StringIP(long ip) {
        return (ip & 0xFF) + "." +
                ((ip >> 8) & 0xFF) + "." +
                ((ip >> 16) & 0xFF) + "." +
                (ip >> 24 & 0xFF);
    }

    private static int string2IntIp(String strIp) {
        int[] ip = new int[4];
        // 先找到IP地址字符串中.的位置
        int position1 = strIp.indexOf(".");
        int position2 = strIp.indexOf(".", position1 + 1);
        int position3 = strIp.indexOf(".", position2 + 1);
        // 将每个.之间的字符串转换成整型
        ip[0] = Integer.parseInt(strIp.substring(0, position1));
        ip[1] = Integer.parseInt(strIp.substring(position1 + 1, position2));
        ip[2] = Integer.parseInt(strIp.substring(position2 + 1, position3));
        ip[3] = Integer.parseInt(strIp.substring(position3 + 1));
        return (ip[0] << 24) + (ip[1] << 16) + (ip[2] << 8) + ip[3];

    }

    /**
     * 获取子网掩码
     *
     * @param context
     * @return
     */
    public static String getNetMaskIp(Context context) {
        WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        DhcpInfo di = wm.getDhcpInfo();
        return intIP2StringIP(di.netmask);
    }


    public static boolean isInChildNet(String pcIp, Context context) {
        WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        String phoneIp = intIP2StringIP(wm.getConnectionInfo().getIpAddress());
        Log.e("111", "isInChildNet: " + phoneIp);
        long netmask = wm.getDhcpInfo().netmask;
        if (netmask == 0) { //在某些手机下,如 魅族 ,子网掩码会是0,这种情况下就不执行校验逻辑了
            Log.e("111", "isInChildNet: zzz");
            return true;
        }
        boolean b = checkSameSegment(phoneIp, pcIp, netmask);
        return b;

    }

    /**
     * 比较两个ip地址是否在同一个网段中，如果两个都是合法地址，两个都是非法地址时，可以正常比较；
     * 如果有其一不是合法地址则返回false；
     * 注意此处的ip地址指的是如“192.168.1.1”地址
     *
     * @return
     */
    public static boolean checkSameSegment(String phoneIp, String pcIp, long mask) {
        int ipValue1 = getIpV4Value(phoneIp);
        int ipValue2 = getIpV4Value(pcIp);
        int result = ~(ipValue1 ^ ipValue2);
        int i = intRevBtey((int) mask);
        Constants.IP = phoneIp;
        return result >= i;
    }

    private static int getIpV4Value(String ipOrMask) {
        byte[] addr = getIpV4Bytes(ipOrMask);
        int address1 = addr[3] & 0xFF;
        address1 |= ((addr[2] << 8) & 0xFF00);
        address1 |= ((addr[1] << 16) & 0xFF0000);
        address1 |= ((addr[0] << 24) & 0xFF000000);
        return address1;
    }

    private static int intRevBtey(int src) {
        int value;
        value = ((src & 0xFF) << 24
                | ((src >> 8 & 0xFF) << 16)
                | ((src >> 16 & 0xFF) << 8)
                | ((src >> 24 & 0xFF)));
        return value;
    }

    private static byte[] getIpV4Bytes(String ipOrMask) {
        try {
            String[] addrs = ipOrMask.split("\\.");
            int length = addrs.length;
            byte[] addr = new byte[length];
            for (int index = 0; index < length; index++) {
                addr[index] = (byte) (Integer.parseInt(addrs[index]) & 0xff);
            }
            return addr;
        } catch (Exception e) {
        }
        return new byte[4];
    }

    // TODO: 2018/6/28 获取设备名称
    public static String getDeviceModel() {
        return Build.MODEL;
    }

    // TODO: 2018/6/28 获取无线网名称
    public static String getWinfeName(Context context) {
        WifiManager wifiMgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        int wifiState = wifiMgr.getWifiState();
        WifiInfo info = wifiMgr.getConnectionInfo();
        if (info == null || TextUtils.isEmpty(info.getSSID())) {
            return null;
        }
        //获取Android版本号
        int sdkInt = Build.VERSION.SDK_INT;
        if (sdkInt >= 17) {
            if (info.getSSID().startsWith("\"") && info.getSSID().endsWith("\"")) {

                return info.getSSID().substring(1, info.getSSID().length() - 1);
            }
        }
        return info != null ? info.getSSID() : null;
    }
}
