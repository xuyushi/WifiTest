# WifiTest
WIFI STRESS TEST
title: wifi压力测试
date: July 9, 2015 7:12 PM
categories: android
tags: [android,wifi]
description: 
---
[TOC]


## 效果图
![效果图](https://raw.githubusercontent.com/xuyushi/Blog_img/master/wifitest.gif)

开关wifi
并且在开wifi后，判断网络连接
由于存在wifi连接之后并没有网络的情况，所以采用ping 来判断网络的通断

## AndroidManifest中申请权限

```xml
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
<uses-permission android:name="android.permission.WAKE_LOCK"/>
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.WRITE_SETTINGS" />
```

## 控件，界面初始化
  详见源码
  
## 循环任务
使用timer来实现
```java
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
```

判断 isRunning 标志位，
* 如果为！isRunning ，或者任务的次数执行完毕，则取消定时任务。并且发送message(0)
* 如果为！isRunning，切换wifi的状态，并在wifi开的适合判断网络连接，并且发送message（1）


### 判断网络通断代码
```java
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
```

通过` Runtime.getRuntime().exec`来执行命令行
`status = p.waitFor();`来获取运行结果。
> p.waitFor会阻塞

## 自定义handle处理消息
```java
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
```

源码：https://github.com/xuyushi/WifiTest

