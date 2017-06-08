package com.uaes.ble_client_v2;

import android.bluetooth.BluetoothDevice;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by len on 2016/12/25.
 */
public class adDataClass {

    static public  class adData implements Serializable{
        public String name;
        public String bluetoothAddress;
        public byte txPower;
        public int rssi;
        public int[] rssi_temp = new int[30];
        public int rssi_absoult;
        public long rssi_sum;
        public int rssi_num;
        public int rssi_num_absolute = 1;
        public int flag;
        public List<UUID> uuids = new ArrayList<UUID>() ;
        public String localName;
        public Short manufacturer;
        public boolean packageValidity = true;
        public int bondState;

    }
    public static adData fromScanData(BluetoothDevice device, int rssi, byte[] scanData) {


        adData adData = new adData();

        adData.rssi_absoult = rssi;
        adData.rssi_temp[0] = adData.rssi_absoult;
        adData.rssi_sum = rssi;

        ByteBuffer buffer = ByteBuffer.wrap(scanData).order(ByteOrder.LITTLE_ENDIAN);   // save scan in buffer
        while (buffer.remaining() > 2) {
            byte length = buffer.get(); // read first byte in buffer
            if (length == 0)    // no valid data
                break;

            byte type = buffer.get();       // get type of following data
            length -= 1;                       // read one byte
            switch (type) {
                case 0x01: // Flags show whether BLE can be connected and how
                    adData.flag = buffer.get();
                    length--;
                    break;
                case 0x02: // Partial list of 16-bit UUIDs
                case 0x03: // Complete list of 16-bit UUIDs
                case 0x14: // List of 16-bit Service Solicitation UUIDs
                    while (length >= 2) {
                        adData.uuids.add(UUID.fromString(String.format(
                               "%08x-0000-1000-8000-00805f9b34fb", buffer.getShort())));
                       //int t = buffer.getShort();
                        length -= 2;
                    }
                    break;
                case 0x04: // Partial list of 32 bit service UUIDs
                case 0x05: // Complete list of 32 bit service UUIDs
                    while (length >= 4) {
                          adData.uuids.add(UUID.fromString(String.format(
                                "%08x-0000-1000-8000-00805f9b34fb", buffer.getInt())));
                        length -= 4;
                    }
                    break;
                case 0x06: // Partial list of 128-bit UUIDs
                case 0x07: // Complete list of 128-bit UUIDs
                case 0x15: // List of 128-bit Service Solicitation UUIDs
                    while (length >= 16) {
                          long lsb = buffer.getLong();
                         long msb = buffer.getLong();
                        UUID X = new UUID(msb,lsb);
                        adData.uuids.add(X);
                        length -= 16;
                    }
                    break;
                case 0x08: // Short local device name
                case 0x09: // Complete local device name
                    byte sb[] = new byte[length];
                    buffer.get(sb, 0, length);
                    length = 0;
                    adData.localName = new String(sb).trim();
                    break;
                case (byte) 0x0A: // Manufacturer Specific Data
                    //parsedAd.manufacturer = buffer.getShort();
                    adData.txPower =  buffer.get();
                    length -= 1;
                    break;
                case (byte) 0xFF: // Manufacturer Specific Data
                    adData.manufacturer = buffer.getShort();
                    length -= 2;
                    break;
                default: // skip
                    adData.packageValidity = false;         // content not supported
                    break;

            }
            if(adData.packageValidity == false)
            {
                buffer.position(61);
            }else{
                if (length > 0) {
                    buffer.position(buffer.position() + length);
                }
            }

        }

        if (device != null) {
            adData.bluetoothAddress = device.getAddress();
            if(device.getName() != null) {
                adData.name = device.getName();
            }
            else {
                adData.name = "Unknown";
            }
        }

        adData.bondState = device.getBondState();

        return adData;
    }


}
