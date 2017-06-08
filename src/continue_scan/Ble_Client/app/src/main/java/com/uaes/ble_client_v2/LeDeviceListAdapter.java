package com.uaes.ble_client_v2;

import android.app.Activity;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import static android.content.ContentValues.TAG;

/**
 * Created by len on 2016/12/22.
 */
public class LeDeviceListAdapter extends BaseAdapter {

    // Adapter for holding devices found through scanning.

    private ArrayList<adDataClass.adData> mLeDevices;
    private LayoutInflater mInflator;
    private Activity mContext;
    private int targetRssi;
    private int same=0;

    public LeDeviceListAdapter(Activity c) {
        super();
        mContext = c;
        mLeDevices = new ArrayList<adDataClass.adData>();
        mInflator = mContext.getLayoutInflater();
    }

    public void addDevice(adDataClass.adData device, int target_Rssi) {
        if (device == null)
            return;


/*


        if (!mLeDevices.contains(device)) {
            mLeDevices.add(device);
        }
        */

        //mLeDevices.add(device);
        try {
            if (mLeDevices.isEmpty()) {
                mLeDevices.add(device);
            } else {
                for (int i = 0; i < mLeDevices.size(); i++) {
                    String btAddress = mLeDevices.get(i).bluetoothAddress;
                    String btName = mLeDevices.get(i).name;
                    if ((btAddress.equals(device.bluetoothAddress)) && ((btName.equals(device.name)))) {
                        mLeDevices.add(i + 1, device);

                        // size of rssi_temp
                        long size = mLeDevices.get(i + 1).rssi_temp.length;

                        // rssi process
                        for (int j = 0; j < (size - 1); j++) {
                            (mLeDevices.get(i + 1)).rssi_temp[j + 1] = (mLeDevices.get(i)).rssi_temp[j];
                            (mLeDevices.get(i + 1)).rssi_sum += (mLeDevices.get(i)).rssi_temp[j];
                        }


                        (mLeDevices.get(i + 1)).rssi_num_absolute = (mLeDevices.get(i)).rssi_num_absolute;
                        if ((mLeDevices.get(i + 1)).rssi_num_absolute < size) {
                            (mLeDevices.get(i + 1)).rssi_num_absolute++;
                            (mLeDevices.get(i + 1)).rssi = (int) (((mLeDevices.get(i + 1)).rssi_sum) / (mLeDevices.get(i + 1)).rssi_num_absolute);
                        } else {
                            (mLeDevices.get(i + 1)).rssi_num = (mLeDevices.get(i)).rssi_num;
                            if ((mLeDevices.get(i + 1)).rssi_num > (size * 5 / 100)) {
                                (mLeDevices.get(i + 1)).rssi = (int) (((mLeDevices.get(i + 1)).rssi_sum) / size);
                                (mLeDevices.get(i + 1)).rssi_num = 0;
                            } else {
                                (mLeDevices.get(i + 1)).rssi_num++;
                                (mLeDevices.get(i + 1)).rssi = (mLeDevices.get(i)).rssi;
                            }
                        }
                        mLeDevices.remove(i);
                        same = 1;
                    }
                }

                if (same == 0) {
                    mLeDevices.add(device);
                } else {
                    same = 0;
                }

            }


            targetRssi = target_Rssi;
        /*
        for(int i=0;i<mLeDevices.size();i++){
            String btAddress = mLeDevices.get(i).bluetoothAddress;
            if(btAddress.equals(device.bluetoothAddress)){
                mLeDevices.add(i+1, device);
                mLeDevices.remove(i);
                return;
            }
        }
        */
            // mLeDevices.add(device);
        } catch (Exception e){
            Log.e(TAG,"here");

        }
    }

    public adDataClass.adData getDevice(int position) {
        return mLeDevices.get(position);
    }

    public void clear() {
        mLeDevices.clear();
    }

    @Override
    public int getCount() {
        return mLeDevices.size();
    }

    @Override
    public Object getItem(int i) {
        return mLeDevices.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder viewHolder;
        // General ListView optimization code.
        if (view == null) {
            view = mInflator.inflate(R.layout.ble_device_list_item,null);

            viewHolder = new ViewHolder();
            viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
            viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);

            //viewHolder.deviceUUID= (TextView)view.findViewById(R.id.device_beacon_uuid);
            // viewHolder.deviceMajor_Minor=(TextView)view.findViewById(R.id.device_major_minor);
            viewHolder.devicetxPower_RSSI=(TextView)view.findViewById(R.id.device_txPower_rssi);
            viewHolder.devicetxPower_RSSI_sum=(TextView)view.findViewById(R.id.device_txPower_rssi_sum);
            viewHolder.devicetxPower_RSSI_absoult=(TextView)view.findViewById(R.id.device_txPower_rssi_absolute);
           // viewHolder.connectButton = (Button) view.findViewById(R.id.bleConnectButton);
            viewHolder.rssiStatusLED = (ImageView) view.findViewById(R.id.rssiStatusLED);
            viewHolder.rssiStatus =  (TextView)view.findViewById(R.id.rssiStatus);

            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }




        //BluetoothDevice device = mLeDevices.get(i);
        adDataClass.adData device  = mLeDevices.get(i);


        final String deviceName = device.name;
        final String deviceAddress = device.bluetoothAddress;
        if (deviceName != null && deviceName.length() > 0)
            viewHolder.deviceName.setText(deviceName);
        else
            viewHolder.deviceName.setText(R.string.unknown_device);

        viewHolder.deviceAddress.setText(deviceAddress);


        viewHolder.devicetxPower_RSSI.setText("rssi_average:"+device.rssi);
        viewHolder.devicetxPower_RSSI_sum.setText("rssi_sum:"+device.rssi_sum);
        viewHolder.devicetxPower_RSSI_absoult.setText("rssi:"+device.rssi_absoult);


        if(device.rssi<= targetRssi){

            viewHolder.rssiStatus.setText(R.string.Invalid);
            viewHolder.rssiStatus.setTextColor(android.graphics.Color.RED);
            viewHolder.rssiStatusLED.setImageResource(android.R.drawable.ic_notification_overlay);
        }else{
            viewHolder.rssiStatus.setText(R.string.Vaild);
            viewHolder.rssiStatus.setTextColor(Color.GREEN);
            viewHolder.rssiStatusLED.setImageResource(android.R.drawable.presence_online);
        }

        /*
        viewHolder.connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showInfo();
            }
        });
        */


        //viewHolder.devicetxPower_RSSI.setText("txPower:"+device.txPower+",rssi:"+device.rssi);
        return view;
    }

    class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        TextView devicetxPower_RSSI;
        TextView devicetxPower_RSSI_absoult;
        TextView devicetxPower_RSSI_sum;

        TextView rssiStatus;
        ImageView rssiStatusLED;

    }
}

