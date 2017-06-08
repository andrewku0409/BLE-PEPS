package com.uaes.ble_client_v2;

import android.Manifest;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;


public class BleClientMainActivity extends ListActivity{

    private final static String TAG = BleClientMainActivity.class.getSimpleName();
    private final static String UUID_RESPONSE = "12486a95-dcf5-4a09-bdbd-e4c76c43624e";
    private final static String UUID_REQUEST = "86f7f7ec-ec9d-4e70-a602-26d274909fdf";
    private final int MY_REQUEST_PERMISSION = 1;
    private String BTtoFind = "UAES";
    Date[] date = new Date[2];
    private byte[] test = new byte[1];
    private BluetoothGattCharacteristic ResponseCharac;
    private ListView listview;

    private LeDeviceListAdapter mLeDeviceListAdapter;
    private ArrayList<BluetoothDevice> Devices = new ArrayList<BluetoothDevice>();



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

    private TextView Request;
    private TextView Response;
    private TextView RequestContent;
    private TextView ResponseContent;
    private TextView ConnectStatusContent;
    private TextView connectedRssi;
    private TextView connectedRssiSum;
    private TextView connectedRssiAverage;
    private EditText targetRssiEditText;
    private int targetRssi;
    private ImageView rssiStatusLED;
    private TextView rssiStatus;

    // Stops scanning after 10 seconds.
    private static final long SCAN_DUTY = 10000;
   // private static final long SCAN_PERIOD = 3;
   private static final long SCAN_PERIOD = 1000000;
    private int SCAN_COUNT = 0;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_client_main);

        //Toolbar toolbar =(Toolbar) findViewById(R.id.Toolbar);
        //toolbar.setTitle("2222");
        //toolbar.setTitle(getResources().getString(R.string.app_name));
       // toolbar.setLogo(R.drawable.ic_launcher);


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
        mBLE.setOnDataAvailableListener(mOnDataAvailable);

        mBLE.setOnOnConnectStatusChangedListener(OnConnectStatusChanged);

        setContentView(R.layout.activity_ble_client_main);

        Request=(TextView)findViewById(R.id.Request);
        Response = (TextView)findViewById(R.id.Response);
        RequestContent=(TextView)findViewById(R.id.RequestContent);
        ResponseContent=(TextView)findViewById(R.id.ResponseContent);
        ConnectStatusContent=(TextView)findViewById(R.id.ConnectStatusContent);
        connectedRssi = (TextView)findViewById(R.id.connectedRSSI);
        connectedRssiSum = (TextView)findViewById(R.id.connectedRssiSum);
        connectedRssiAverage = (TextView)findViewById(R.id.connectedRssiAverage);
        targetRssiEditText = (EditText) findViewById(R.id.TargetRssi);
        rssiStatusLED = (ImageView) findViewById(R.id.rssiStatusLED);
        rssiStatus = (TextView) findViewById(R.id.rssiStatus);


        listview = getListView();

    }

    @Override
    protected void onResume() {
        super.onResume();


       // permissionRequest();


        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter(this);
        //setListAdapter(mLeDeviceListAdapter);
        if(listview==null)
            Log.i("debug","null");
        listview.setAdapter(mLeDeviceListAdapter);
        scanLeDevice(true);
        Toast.makeText(this, R.string.ble_scan_start, Toast.LENGTH_SHORT).show();


        registerReceiver(searchDevices, pairIntentFilter());
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
        mBLE.disconnect();
        mBLE.readRemoteRssi();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mBLE.close();
    }

    int devicePos;
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id){
        final adDataClass.adData device = mLeDeviceListAdapter.getDevice(position);
        devicePos = position;
        if(device == null) return;
/*
        if(mScanning){
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }
        */
        /*
        Intent connectIntent = new Intent(BleClientMainActivity.this, connectionActivity.class);
        Bundle blueToothObject= new Bundle();
        blueToothObject.putSerializable("BLE", mBLE);
        blueToothObject.putSerializable("Device", device);
        BleClientMainActivity.this.startActivity(connectIntent);
        */

        String intentAction;
        intentAction = "com.bluetooth.device.action.FOUND";

        broadcastUpdate(intentAction);

        //mBLE.connect(device.bluetoothAddress);

    }



    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                    Toast.makeText(BleClientMainActivity.this, R.string.ble_scan_stop, Toast.LENGTH_SHORT).show();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }



/*
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {

                @Override
                public void run() {
                    SCAN_COUNT++;
                    if((SCAN_COUNT == SCAN_PERIOD) && (mScanning == false)) {
                        mScanning = true;
                        mBluetoothAdapter.startLeScan(mLeScanCallback);
                        Toast.makeText(this, R.string.restart_scan, Toast.LENGTH_SHORT).show();
                    } else {
                        mScanning = false;
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                        Toast.makeText(this, R.string.stop_scan, Toast.LENGTH_SHORT).show();
                    }
                   // invalidateOptionsMenu();

                    mHandler.postDelayed(this,SCAN_DUTY);
                }
            }, SCAN_DUTY);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
       //invalidateOptionsMenu();
    }
*/
    // show all device can be connected
    /*
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
        if(device == null) return;
        if(mScanning){
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }
        mBLE.connect(device.getAddress());

    }
    */

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    String name = device.getName();

                    //if(name.equals("UAES")) {
                        final adDataClass.adData adData = adDataClass.fromScanData(device,rssi,scanRecord);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // method1
                                // show all possible ble terminations, connected by click

                                // used for advertisement analysis
                                try {
                                    targetRssi = -Integer.parseInt(targetRssiEditText.getText().toString());
                                }
                                catch (Exception e){
                                    targetRssi = -50;

                                }

                                mLeDeviceListAdapter.addDevice(adData, targetRssi);
                                mLeDeviceListAdapter.notifyDataSetChanged();

                                // used for pairing
                                if (!Devices.contains(device)) {
                                    Devices.add(device);
                                }


                                /*
                                // method2
                                // connect unique uuid
                                BLE_Termination_name = device.getName();
                                if(BLE_Termination_name.equals(BTtoFind)){
                                    Toast.makeText(this, R.string.ble_termination_found, Toast.LENGTH_SHORT).show();
                                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                                    mScanning = false;
                                    mBLE.connect(device.getAddress());
                                }
                                */
                            }
                        });
                   // }
                }
            };

    /**
     * 搜索到BLE终端服务的事件
     */
    private BluetoothLeClass.OnServiceDiscoverListener mOnServiceDiscover = new BluetoothLeClass.OnServiceDiscoverListener(){

        @Override
        public void onServiceDiscover(BluetoothGatt gatt) {

            displayGattServices(mBLE.getSupportedGattServices());


            final UUID serUUID = UUID.fromString(UUID_REQUEST);
            mBLE.getService(serUUID);
        }

    };

    private BluetoothLeClass.OnConnectStatusChangedListener OnConnectStatusChanged = new BluetoothLeClass.OnConnectStatusChangedListener(){

        @Override
        public void onConnectStatusChanged(BluetoothGatt gatt, int status, int newState) {
            final int newState_temp = newState;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    switch(newState_temp){
                        case (BluetoothProfile.STATE_DISCONNECTED):
                            ConnectStatusContent.setText(R.string.disconnected);
                            break;

                        case (BluetoothProfile.STATE_CONNECTING):
                            ConnectStatusContent.setText(R.string.connecting);
                            break;

                        case (BluetoothProfile.STATE_CONNECTED):
                            ConnectStatusContent.setText(R.string.connected);

                            break;

                        case (BluetoothProfile.STATE_DISCONNECTING):
                            ConnectStatusContent.setText(R.string.disconnecting);
                            break;

                        default:
                            ConnectStatusContent.setText(R.string.no_idea);
                            break;
                    }

                }
            });

        }


    };



    /**
     * 收到BLE终端数据交互的事件
     */
    private BluetoothLeClass.OnDataAvailableListener mOnDataAvailable = new BluetoothLeClass.OnDataAvailableListener(){

        /**
         * 读取BLE终端数据回调
         */
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         final BluetoothGattCharacteristic characteristic, int status) {
            String t = Utils.bytesToHexString(characteristic.getValue());
            //Toast.makeText(BleClientMainActivity.this, t, Toast.LENGTH_SHORT).show();
            if (status == BluetoothGatt.GATT_SUCCESS)
/*
                Log.e(TAG,"onCharRead "+gatt.getDevice().getName()
                        +" read "
                        +characteristic.getUuid().toString()
                        +" -> "
                        +Utils.bytesToHexString(characteristic.getValue()));
                */
                Log.e(TAG,"onCharRead "+gatt.getDevice().getName()
                        +" read "
                        +characteristic.getUuid().toString()
                        +" -> "
                        +characteristic.getStringValue(0));




            //设置数据内容
            if(test[0]<8) {
                test[0]++;
            }else{
                test[0] = 0;
            }
            ResponseCharac.setValue(test);
            //String tttt= test.toString();

            //往蓝牙模块写入数据
            mBLE.writeCharacteristic(ResponseCharac);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    RequestContent.setText(characteristic.getStringValue(0));

                }
            });



        }

        /**
         * 收到BLE终端写入数据回调
         */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          final BluetoothGattCharacteristic characteristic, int status)  {
            Log.e(TAG,"onCharWrite "+gatt.getDevice().getName()
                    +" write "
                    +characteristic.getUuid().toString()
                    +" -> "
                    +new String(characteristic.getValue()));


            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    int t = test[0];
                    Integer.toString(t);
                    ResponseContent.setText(Integer.toString(t));
                }
            });
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                          final BluetoothGattCharacteristic characteristic)  {
            Log.e(TAG,"onCharChanged "+gatt.getDevice().getName()
                    +" changed "
                    +characteristic.getUuid().toString()
                    +" -> "
                    +new String(characteristic.getValue()));
        }

        @Override
        public void  onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status){
            final int rssi_temp = rssi;
            Log.e(TAG,"onReadRemoteRssi "+gatt.getDevice().getName()
                    +" rssi: "
                    +" -> "
                    +rssi);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    connectedRssi.setText(Integer.toString(rssi_temp));
                }
            });

        }

    };


    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;

        for (BluetoothGattService gattService : gattServices) {
            //-----Service的字段信息-----//
            int type = gattService.getType();
            Log.e(TAG,"-->service type:"+Utils.getServiceType(type));
            Log.e(TAG,"-->includedServices size:"+gattService.getIncludedServices().size());
            Log.e(TAG,"-->service uuid:"+gattService.getUuid());

            //-----Characteristics的字段信息-----//
            List<BluetoothGattCharacteristic> gattCharacteristics =gattService.getCharacteristics();
            for (final BluetoothGattCharacteristic  gattCharacteristic: gattCharacteristics) {
                Log.e(TAG,"---->char uuid:"+gattCharacteristic.getUuid());

                int permission = gattCharacteristic.getPermissions();
                Log.e(TAG,"---->char permission:"+Utils.getCharPermission(permission));

                int property = gattCharacteristic.getProperties();
                Log.e(TAG,"---->char property:"+Utils.getCharPropertie(property));

                byte[] data = gattCharacteristic.getValue();
                if (data != null && data.length > 0) {
                    Log.e(TAG,"---->char value:"+new String(data));
                }

                if(gattCharacteristic.getUuid().toString().equals(UUID_RESPONSE)) {
                    ResponseCharac = gattCharacteristic;
                }


                //UUID_KEY_DATA是可以跟蓝牙模块串口通信的Characteristic
                if(gattCharacteristic.getUuid().toString().equals(UUID_REQUEST)){
                    //测试读取当前Characteristic数据，会触发mOnDataAvailable.onCharacteristicRead()
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mBLE.readCharacteristic(gattCharacteristic);
                            mHandler.postDelayed(this, 500);

                            mBLE.readRemoteRssi();

                            //BluetoothGattDescriptor descriptor = gattCharacteristic.getDescriptor(HD_Profile.UUID_CHAR_NOTIFY_DIS)
                        }
                    }, 500);

                    //接受Characteristic被写的通知,收到蓝牙模块的数据后会触发mOnDataAvailable.onCharacteristicWrite()
                    mBLE.setCharacteristicNotification(gattCharacteristic, true);

                    //设置数据内容
                    //gattCharacteristic.setValue("hi");
                    //往蓝牙模块写入数据
                  // mBLE.writeCharacteristic(gattCharacteristic);
                }




                //-----Descriptors的字段信息-----//
                List<BluetoothGattDescriptor> gattDescriptors = gattCharacteristic.getDescriptors();
                for (BluetoothGattDescriptor gattDescriptor : gattDescriptors) {
                    Log.e(TAG, "-------->desc uuid:" + gattDescriptor.getUuid());
                    int descPermission = gattDescriptor.getPermissions();
                    Log.e(TAG,"-------->desc permission:"+ Utils.getDescPermission(descPermission));

                    byte[] desData = gattDescriptor.getValue();
                    if (desData != null && desData.length > 0) {
                        Log.e(TAG, "-------->desc value:"+ new String(desData));
                    }
                }
            }
        }//

    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_ble_client_main, menu);
        return true;
    }
/*
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
*/






    private static IntentFilter pairIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
       /* reserved for other usages */
/*
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        */
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        intentFilter.addAction("com.bluetooth.device.action.FOUND");
        intentFilter.setPriority(Integer.MAX_VALUE);

        return intentFilter;
    }

/*
    private final BroadcastReceiver searchDevices = new BroadcastReceiver() {

        String pin = "1234";  //此处为你要连接的蓝牙设备的初始密钥，一般为1234或0000

        //广播接收器，当远程蓝牙设备被发现时，回调函数onReceiver()会被执行
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction(); //得到action
            Log.e("action1=", action);
            BluetoothDevice btDevice=null;  //创建一个蓝牙device对象
            // 从Intent中获取设备对象
            btDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if(action.equals("com.bluetooth.device.action.FOUND")){  //发现设备


                Log.e("发现设备:", "["+btDevice.getName()+"]"+":"+btDevice.getAddress());

                if(btDevice.getName().contains("UAES"))//HC-05设备如果有多个，第一个搜到的那个会被尝试。
                {
                    if (btDevice.getBondState() == BluetoothDevice.BOND_NONE) {

                        Log.e("ywq", "attemp to bond:"+"["+btDevice.getName()+"]");
                        try {
                            //通过工具类ClsUtils,调用createBond方法
                            ClsUtils.createBond(btDevice.getClass(), btDevice);


                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }else
                    Log.e("error", "Is faild");
            }else if(action.equals("android.bluetooth.device.action.PAIRING_REQUEST")) //再次得到的action，会等于PAIRING_REQUEST
            {
                Log.e("action2=", action);
                if(btDevice.getName().contains("HC-05"))
                {
                    Log.e("here", "OKOKOK");

                    try {

                        //1.确认配对
                        ClsUtils.setPairingConfirmation(btDevice.getClass(), btDevice, true);
                        //2.终止有序广播
                        Log.i("order...", "isOrderedBroadcast:"+isOrderedBroadcast()+",isInitialStickyBroadcast:"+isInitialStickyBroadcast());
                        abortBroadcast();//如果没有将广播终止，则会出现一个一闪而过的配对框。
                        //3.调用setPin方法进行配对...
                        boolean ret = ClsUtils.setPin(btDevice.getClass(), btDevice, pin);

                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }else
                    Log.e("提示信息", "这个设备不是目标蓝牙设备");

            }
        }

    };
*/

    public final BroadcastReceiver searchDevices = new BroadcastReceiver() {

        String pin = "1234";  //此处为你要连接的蓝牙设备的初始密钥，一般为1234或0000



        //广播接收器，当远程蓝牙设备被发现时，回调函数onReceiver()会被执行
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction(); //得到action
            Log.e("action1=", action);
            BluetoothDevice btDevice=null;  //创建一个蓝牙device对象
            // 从Intent中获取设备对象
            btDevice = Devices.get(devicePos);
            Log.e("bond", Integer.toString(btDevice.getBondState()));
            Log.e("action",action);

            if(action.equals("com.bluetooth.device.action.FOUND")){  //发现设备

                Log.e("发现设备:", "["+btDevice.getName()+"]"+":"+btDevice.getAddress());

                //HC-05设备如果有多个，第一个搜到的那个会被尝试。
                if(btDevice.getName().contains("UAES")) {

                    if (btDevice.getBondState() == BluetoothDevice.BOND_NONE) {

                        Log.e("ywq", "attemp to bond:"+"["+btDevice.getName()+"]");
                        try {
                            //通过工具类ClsUtils,调用createBond方法
                            ClsUtils.createBond(btDevice.getClass(), btDevice);


                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }else {
                    Log.e("error", "Is faild");
                }
            }else if(BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action))                                                     //再次得到的action，会等于PAIRING_REQUEST
            {
                Log.e("action2=", action);
                if (btDevice.getName().contains("UAES")) {
                    Log.e("here", "OKOKOK");

                    try {

                        //1.确认配对
                        ClsUtils.setPairingConfirmation(btDevice.getClass(), btDevice, true);
                        //2.终止有序广播
                        Log.i("order...", "isOrderedBroadcast:" + isOrderedBroadcast() + ",isInitialStickyBroadcast:" + isInitialStickyBroadcast());
                        abortBroadcast();//如果没有将广播终止，则会出现一个一闪而过的配对框。
                        //3.调用setPin方法进行配对...
                        boolean ret = ClsUtils.setPin(btDevice.getClass(), btDevice, pin);

                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                } else {
                    Log.e("提示信息", "这个设备不是目标蓝牙设备");
                }
            }

            if(btDevice.getBondState() == BluetoothDevice.BOND_BONDED){

                try {
                    mBLE.connect(btDevice.getAddress());
                    Log.e("error", Integer.toString(btDevice.getBondState()));
                }catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }


    };


    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    /***********************************************************************************************************************************
    /* reserved for ble in sdk 23 android 6.0 permission grant
    /**********************************************************************************************************************************/
    public void permissionRequest(){
        if(Build.VERSION.SDK_INT >= 6.0){
            // if((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, MY_REQUEST_PERMISSION);
            //  }
        }
        return;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults){
        switch(requestCode){
            case MY_REQUEST_PERMISSION:{
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {

                }
                return;
            }
        }
    }
/***********************************************************************************************************************************
 /**********************************************************************************************************************************/

}
