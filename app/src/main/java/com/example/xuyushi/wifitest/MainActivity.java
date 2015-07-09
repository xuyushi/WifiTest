package com.example.xuyushi.wifitest;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends Activity {

    private static final int MAX_COUNT = 20;
    private static final String TAG = "MainActivity";
    private WifiManager mWifiManager;
    private Timer mTimer;
    public boolean isRunning = false;
    public int mMaxTestCount = MAX_COUNT;
    public int mCurrentCount = 0;
    public int mPassCount = 0;
    private PowerManager.WakeLock mWakeLock = null;

    private String mPingIpAddrResult = null;
    private String mIpAddress = "8.8.8.8";

    private TextView maxCount;
    private TextView currentCount;
    private TextView passCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       Window window = getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        params.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE;
        window.setAttributes(params);
        setContentView(R.layout.activity_main);
        init();

    }

    private void init() {
        mWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        acquireWakeLock();
        maxCount = (TextView) findViewById(R.id.max_count);
        currentCount = (TextView) findViewById(R.id.current_count);
        passCount = (TextView) findViewById(R.id.pass_count);
        maxCount.setText(getString(R.string.max_count) + mMaxTestCount);
        passCount.setText(getString(R.string.pass_count) + mPassCount);
        currentCount.setText(getString(R.string.current_count) + mCurrentCount);
        mWifiManager.setWifiEnabled(true);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseWakeLock();
    }

    public void startTest(View view) {
        isRunning = true;
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!isRunning || (mMaxTestCount != 0 && mCurrentCount >= mMaxTestCount)) {
                    mHandler.sendEmptyMessage(0);
                    mTimer.cancel();
                    return;
                } else {
                    if (mWifiManager.getWifiState() == mWifiManager.WIFI_STATE_DISABLED) {
                        mWifiManager.setWifiEnabled(true);
                        Log.d(TAG, "*********wifistate is closed, try open wifi now!*****");
                    } else if (mWifiManager.getWifiState() == mWifiManager.WIFI_STATE_ENABLED) {
                        pingIpAddr();
                        if (null != mPingIpAddrResult) {
                            Log.d(TAG, mPingIpAddrResult);
                        }
                        mWifiManager.setWifiEnabled(false);
                        Log.d(TAG, "*********wifistate is opened, try close wifi now!******");
                        incCurCount();
                    }
                    mHandler.sendEmptyMessage(1);
                }

            }
        }, 2000, 10000);
    }

    public void stopTest(View view) {
        isRunning = false;
        if (mTimer != null)
            mTimer.cancel();
    }

    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case 0:
                    isRunning = false;
                    if (mTimer != null)
                        mTimer.cancel();
                    break;
                case 1:
                    currentCount.setText(getString(R.string.current_count) + mCurrentCount);
                    passCount.setText(getString(R.string.pass_count) + mPassCount);
                    Log.d(TAG, "already test time:" + mCurrentCount);

                    break;

                default:
                    break;
            }
        }
    };

    public int incCurCount() {
        mCurrentCount = mCurrentCount + 1;
        return mCurrentCount;
    }

    //获取电源锁，保持该服务在屏幕熄灭时仍然获取CPU时，保持运行
    private void acquireWakeLock() {
        if (null == mWakeLock) {
            PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "PostLocationService");
            if (null != mWakeLock) {
                mWakeLock.acquire();
            }
        }
    }

    //释放设备电源锁
    private void releaseWakeLock() {
        if (null != mWakeLock) {
            mWakeLock.release();
            mWakeLock = null;
        }
    }


    private void pingIpAddr() {
        try {
            // TODO: Hardcoded for now, make it UI configurable
            Process p = Runtime.getRuntime().exec("ping -c 1 -w 7 " + mIpAddress);
            int status = p.waitFor();
            if (status == 0) {
                mPingIpAddrResult = "Pass";
                mPassCount++;
            } else {
                mPingIpAddrResult = "Fail: IP addr not reachable";
            }
        } catch (IOException e) {
            mPingIpAddrResult = "Fail: IOException";
        } catch (InterruptedException e) {
            mPingIpAddrResult = "Fail: InterruptedException";
        }
    }

    private class NetPing extends AsyncTask {

        @Override
        protected Void doInBackground(Object... params) {
            pingIpAddr();
            return null;
        }
    }

    public void setAirPlaneMode(Context context, boolean enable, int value) {
      /*  Settings.System.putInt(context.getContentResolver(), "State", enable ? 1 : 0);
        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.putExtra("stat", enable);
        context.sendBroadcast(intent);*/ //注释部分为 4.2版本之前的做法，现在已经失效。

        //对版本做出判断
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            //if less than verson 4.2
            Settings.System.putInt(
                    getContentResolver(),
                    Settings.System.AIRPLANE_MODE_ON, value);
        } else {
            Settings.Global.putInt(
                    getContentResolver(),
                    Settings.Global.AIRPLANE_MODE_ON, value);
        }
        // broadcast an intent to inform
        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.putExtra("state", !enable);
        sendBroadcast(intent);

    }

    public boolean isAirPlaneOn(Context context) {
       /* int isEnable = Settings.System.getInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0);
        return (isEnable==1)?true:false;*/
        //注释部分，是4.2版本之前的做法，现在已经失效。

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            //android version lower than 4.2
            return Settings.System.getInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0) != 0;

        } else {
            return Settings.Global.getInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
        }
    }

}

