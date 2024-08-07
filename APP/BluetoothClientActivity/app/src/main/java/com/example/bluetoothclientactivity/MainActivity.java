package com.example.bluetoothclientactivity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

/**
 * Android手机客户端通过蓝牙发送数据到部署在Windows PC电脑上。
 * 如果运行失败，请打开手机的设置，检查是否赋予该App权限：
 *
 * <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
 * <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
 *
 * <uses-permission android:name="android.permission.BLUETOOTH" />
 * <uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
 *
 * Android手机的蓝牙客户端。
 * 代码启动后检查当前手机是否已经和蓝牙名称为  TARGET_DEVICE_NAME 的配对成功。
 * 如果配对成功，直接发起对服务器的连接并发送数据到服务器端。
 * 如果当前手机蓝牙和服务器端没有配对成功，则先启动蓝牙扫描，去搜索目标蓝牙设备。发现找到目标设备后连接目标设备并发送数据。
 *
 */

public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter mBluetoothAdapter;

    //要连接的目标蓝牙设备。
    private final String TARGET_DEVICE_NAME = "HUHAN";

    private final String TAG = "蓝牙调试";
    private final String MY_UUID = "00001101-0000-1000-8000-00805F9B34FB";

    // 广播接收发现蓝牙设备。
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                String name = device.getName();
                if (name != null)
                    Log.d(TAG, "发现设备:" + name);

                if (name != null && name.equals("HUHAN")) {
                    Log.d(TAG, "发现目标设备，开始线程连接!");

                    // 蓝牙搜索是非常消耗系统资源开销的过程，一旦发现了目标感兴趣的设备，可以考虑关闭扫描。
                    mBluetoothAdapter.cancelDiscovery();

                    new Thread(new ClientThread(device)).start();
                }
            }
        }
    };

    private class ClientThread extends Thread {
        private BluetoothDevice device;

        public ClientThread(BluetoothDevice device) {
            this.device = device;
        }

        @Override
        public void run() {
            BluetoothSocket socket;

            try {
                socket = device.createRfcommSocketToServiceRecord(UUID.fromString(MY_UUID));
                for(int count = 10; count > 0; --count){

                    Log.d(TAG, "连接服务端...");
                    socket.connect();
                    Log.d(TAG, "连接建立.");

                    //从服务端接收数据
                    String inString = receiveDataFromServer(socket);
                    String[] eachData = inString.split(" ");//切分三类数据
                    Log.d(TAG, inString);
                    TextView tv = (TextView)findViewById(R.id.textView);
                    tv.setText(eachData[0] + " " + eachData[1] + " " + eachData[2] + " " + eachData[3]);

                    socket.close();
                }

                //String outString = inString;
                // 开始往服务器端发送数据
                //sendDataToServer(socket, outString);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private String receiveDataFromServer(BluetoothSocket socket){
            String inString = "";
            try {
                InputStream inStream = socket.getInputStream();
                DataInputStream dataInputStream = new DataInputStream(inStream);

                inString = dataInputStream.readUTF();//读蓝牙输入流数据

                dataInputStream.close();
                inStream.close();

                Log.d(TAG, "接收数据");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return inString;
        }

        private void sendDataToServer(BluetoothSocket socket, String outString) {
            try {
                OutputStream outStream = socket.getOutputStream();
                DataOutputStream dataOutputStream = new DataOutputStream(outStream);

                Log.d(TAG, "发送数据");
                dataOutputStream.writeUTF(outString);
                Log.d(TAG, outString);

                dataOutputStream.close();
                outStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private BluetoothDevice getPairedDevices() {
        // 获得和当前Android已经配对的蓝牙设备。
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices != null && pairedDevices.size() > 0) {
            // 遍历
            for (BluetoothDevice device : pairedDevices) {
                // 把已经取得配对的蓝牙设备名字和地址打印出来。
                Log.d(TAG, device.getName() + " : " + device.getAddress());
                if (TextUtils.equals(TARGET_DEVICE_NAME, device.getName())) {
                    Log.d(TAG, "已配对目标设备 -> " + TARGET_DEVICE_NAME);
                    return device;
                }
            }
        }

        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice device = getPairedDevices();
        if (device == null) {
            // 注册广播接收器。
            // 接收蓝牙发现讯息。
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver, filter);

            if (mBluetoothAdapter.startDiscovery()) {
                Log.d(TAG, "启动蓝牙扫描设备...");
            }
        } else {
            new ClientThread(device).start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
    }
}