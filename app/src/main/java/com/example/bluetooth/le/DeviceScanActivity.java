/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.bluetooth.le;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.example.bluetooth.le.BluetoothLeClass.OnServiceDiscoverListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class DeviceScanActivity extends Activity implements AdapterView.OnItemClickListener {
    private final static String TAG = "blueQQQQ";
    private final static String UUID_KEY_DATA = "0000ff0b-0000-1000-8000-00805f9b34fb";// 发送的UUID
    private final static String UUID_KEY_GET = "0000ff0a-0000-1000-8000-00805f9b34fb"; //获取数据的UUID
    private final static String UUID_SERVICE = "0000fff0-0000-1000-8000-00805f9b34fb"; //服务service

    private boolean mesured = false;

    private BLEDeviceListAdapter mLeDeviceListAdapter;
    /**
     * 搜索BLE终端
     */
    private BluetoothAdapter mBluetoothAdapter;
    /**
     * 读写BLE终端
     */
    private BluetoothLeClass mBLE;
    private boolean mScanning;
    private Handler mHandler;

    private ArrayList<BluetoothDevice> devices;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    private ListView listView;
    private Button bt_send;
    private EditText et_code;

    private List<String> dataList = new ArrayList<String>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listView = (ListView) findViewById(R.id.listView);
        et_code = (EditText) findViewById(R.id.et_code);
        bt_send = (Button) findViewById(R.id.btn_send);
        devices = new ArrayList<BluetoothDevice>();
        listView.setOnItemClickListener(this);
        bt_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
        mHandler = new Handler();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        //开启蓝牙
        mBluetoothAdapter.enable();

        mBLE = new BluetoothLeClass(this);
        if (!mBLE.initialize()) {
            Log.e(TAG, "Unable to initialize Bluetooth");
            finish();
        }
        //发现BLE终端的Service时回调
        mBLE.setOnServiceDiscoverListener(mOnServiceDiscover);
        //收到BLE终端数据交互的事件
//        mBLE.setOnDataAvailableListener(mOnDataAvailable);
    }


    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {

            super.onCharacteristicChanged(gatt, characteristic);

            String receiveStr = Utils.bytesToHexString(characteristic.getValue());

            LogUtils.i("onCharacteristicChanged--接收到的notify值------>" + characteristic.getUuid() + "-------" + Utils.bytesToHexString(characteristic.getValue()));

            // 判断是否可以进行数据传输
            if (receiveStr.equals("aa0600")) {
                startWriteData();
            }

            if (receiveStr.equals("aa0400")) {
                LogUtils.i("设置时间成功");
            }

            // 获取历史包大小
            if (receiveStr.contains("aa01")) {
                isPrepare = true;

                if (receiveStr.equals("aa010000")) {
                    LogUtils.i("没有历史数据");
                } else {
                    LogUtils.i("得到的历史数据包大小为--->" + receiveStr);
                    String countStr = receiveStr.substring(6, 8);
                    LogUtils.i("获得的包的大小为---->" + Integer.parseInt(countStr, 16));
                    count = Integer.valueOf(Integer.parseInt(countStr, 16));
                    gethistoryTimer.schedule(new GetHistoryTask(), 0, 1000);
                }

            }


            // 接收每个包
            if (receiveStr.contains("dd")) {

                receiveStr = receiveStr.replace("dd", "");


                if (!dataList.contains(receiveStr)) {// 如果不包括，则添加到list
                    if (receiveStr.length() > 16) {
                        dataList.add(receiveStr.substring(0, 15));
                        LogUtils.i("得到的包的字符串1---》" + receiveStr.substring(0, 15));
                        dataList.add(receiveStr.substring(16, 31));
                        LogUtils.i("得到的包的字符串2---》" + receiveStr.substring(16, 31));
                    } else {
                        dataList.add(receiveStr);
                        LogUtils.i("得到的包的字符串单包---》" + receiveStr);
                    }

                }

            }

            LogUtils.i("解析到的包的大小为---->" + dataList.size());

            if (receiveStr.equals("aa0300")) {
                LogUtils.i("删除成功！");
            }


            //收到设备notify值 （设备上报值）
        }


        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

            LogUtils.i("onCharacteristicRead--接收到的notify值------>" + characteristic.getUuid() + "-------" + Utils.bytesToHexString(characteristic.getValue()));
            //收到设备notify值 （设备上报值）
            //读取到值

        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //write成功（发送值成功）
                LogUtils.i("发送成功-uuid===>" + characteristic.getUuid() + "发送的值---->" + Utils.bytesToHexString(characteristic.getValue()));


            }

        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    // 连接成功

                    LogUtils.i("连接成功！！！！");

                    gatt.discoverServices();// 寻找服务


                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    // 断开连接

                    LogUtils.i("断开连接.....");
                    searchTimer.cancel();

                    isPrepare = false;


                }
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt,
                                     BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                                      BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);

        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //获取到RSSI，  RSSI 正常情况下 是 一个 负值，如 -33 ； 这个值的绝对值越小，代表设备离手机越近
                //通过mBluetoothGatt.readRemoteRssi();来获取
            }
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                //寻找到服务
                LogUtils.i("寻找到服务！！！");
                isPrepare = false;

                searchTimer.schedule(timerTask, 0, 1000);
            }
        }
    };

    /**
     * 开始写入数据
     */
    private void startWriteData() {
    }


    @Override
    protected void onResume() {
        super.onResume();

        // Initializes list view adapter.
        mLeDeviceListAdapter = new BLEDeviceListAdapter(this, devices);

        listView.setAdapter(mLeDeviceListAdapter);
        scanLeDevice(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
        mBLE.disconnect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mBLE.close();
    }

    private BluetoothGatt bluetoothGatt;

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    /**
     * 搜索到BLE终端服务的事件
     */
    private BluetoothLeClass.OnServiceDiscoverListener mOnServiceDiscover = new OnServiceDiscoverListener() {

        @Override
        public void onServiceDiscover(BluetoothGatt gatt) {
            displayGattServices(mBLE.getSupportedGattServices());
        }

    };


    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!devices.contains(device))
                                devices.add(device);
                            mLeDeviceListAdapter.notifyDataSetChanged();
                        }
                    });
                }
            };

    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;

        for (BluetoothGattService gattService : gattServices) {
            //-----Service的字段信息-----//
            int type = gattService.getType();
            Log.e(TAG, "-->service type:" + Utils.getServiceType(type));
            Log.e(TAG, "-->includedServices size:" + gattService.getIncludedServices().size());
            Log.e(TAG, "-->service uuid:" + gattService.getUuid());

            //-----Characteristics的字段信息-----//
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            for (final BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                Log.e(TAG, "---->char uuid:" + gattCharacteristic.getUuid());

                int permission = gattCharacteristic.getPermissions();
                Log.e(TAG, "---->char permission:" + Utils.getCharPermission(permission));

                int property = gattCharacteristic.getProperties();
                Log.e(TAG, "---->char property:" + Utils.getCharPropertie(property));

                byte[] data = gattCharacteristic.getValue();
                if (data != null && data.length > 0) {
                    Log.e(TAG, "---->char value:" + new String(data));
                }

            }
        }//

    }


    /**
     * 设置并发送数据
     */
    private void setSetting() {
        BluetoothGattService sendService = bluetoothGatt.getService(UUID.fromString(UUID_SERVICE));
        enableNotification(true, sendService);
        LogUtils.i("服务是否为null---->" + (sendService == null));

        if (sendService != null) {
            BluetoothGattCharacteristic sendCharacteristic = sendService.getCharacteristic(UUID.fromString(UUID_KEY_DATA));

            LogUtils.i("发送的串口是否为Null--->" + (sendCharacteristic == null));
            if (sendCharacteristic != null) {

                sendCharacteristic.setValue(Utils.getHexBytes("5501"));
                bluetoothGatt.writeCharacteristic(sendCharacteristic);


            }
        }

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
        if (device == null) return;
        if (mScanning) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }

        if (!bluetoothGatt.connect()) {
            bluetoothGatt = device.connectGatt(DeviceScanActivity.this, false, gattCallback);
        } else {

        }

    }


    /**
     * set notification
     *
     * @param enable
     * @param service
     * @return
     */
    private boolean enableNotification(boolean enable, BluetoothGattService service) {

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(UUID_KEY_GET));

        if (bluetoothGatt == null || characteristic == null)
            return false;
        if (!bluetoothGatt.setCharacteristicNotification(characteristic,
                enable))
            return false;
        BluetoothGattDescriptor clientConfig = characteristic
                .getDescriptor(UUID.fromString(UUID_KEY_GET));
        if (clientConfig == null)
            return false;

        if (enable) {
            clientConfig
                    .setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        } else {
            clientConfig
                    .setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        }
        return bluetoothGatt.writeDescriptor(clientConfig);
    }

    /**
     * 获取历史数据
     */
    private void getHistoryData(int count) {
        LogUtils.i("获得数量为---->" + count);
        BluetoothGattService sendService = bluetoothGatt.getService(UUID.fromString(UUID_SERVICE));
        enableNotification(true, sendService);
        if (sendService != null) {
            BluetoothGattCharacteristic sendCharacteristic = sendService.getCharacteristic(UUID.fromString(UUID_KEY_DATA));
            if (sendCharacteristic != null) {
                sendCharacteristic.setValue(Utils.getHexBytes("5502" + Utils.addZeroForNum(Integer.toHexString(count), 2) + "00"));
                bluetoothGatt.writeCharacteristic(sendCharacteristic);
            }
        }

    }

    /**
     * 自定义定时器类
     */


    MyTimerTask timerTask = new MyTimerTask();
    Timer searchTimer = new Timer(true);
    Timer gethistoryTimer = new Timer(true);


    Handler searchHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case 1:
                    searchTimer.cancel();// 取消定时操作

                    break;

                case 2:

                    break;
            }
        }
    };


    private boolean isPrepare = false;

    private class MyTimerTask extends TimerTask {
        @Override
        public void run() {
            Message message = new Message();
            if (isPrepare) {
                message.what = 1;// 表示可以传输数据


            } else {
                message.what = 2;// 表示不可以传输数据
                setSetting();
            }

            searchHandler.sendMessage(message);
        }
    }


    private int count = 0;

    private class GetHistoryTask extends TimerTask {

        @Override
        public void run() {
            if (count != 0) {
                LogUtils.i("获取历史执行---------->" + count);
                getHistoryData(count);
                count = count - 1;
            } else {
                gethistoryTimer.cancel();
                deleteRecord();
            }
        }
    }


    /**
     * 删除记录
     */
    private void deleteRecord() {
        BluetoothGattService sendService = bluetoothGatt.getService(UUID.fromString(UUID_SERVICE));
        enableNotification(true, sendService);
        if (sendService != null) {
            BluetoothGattCharacteristic sendCharacteristic = sendService.getCharacteristic(UUID.fromString(UUID_KEY_DATA));
            if (sendCharacteristic != null) {
                sendCharacteristic.setValue(Utils.getHexBytes("5503"));
                bluetoothGatt.writeCharacteristic(sendCharacteristic);
            }
        }
    }


}