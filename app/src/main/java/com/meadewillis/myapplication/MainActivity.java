package com.meadewillis.myapplication;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends Activity {


    public final static UUID UUID_BLE_HM10_RX_TX = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb"); // CHARACTERISTIC UUID FROM SERVICE
    public final static UUID UUID_BLE_HM10_SERVICE = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"); // SERVICE UUID
    
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 2;
    private static final int REQUEST_ENABLE_BT = 1;
    
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private BluetoothGatt mBluetoothGatt;
    BluetoothDevice mBluetoothDevice;
    BluetoothGattCharacteristic mBluetoothGattCharacteristic;
    BluetoothGattService mBluetoothGattService;

    private static boolean mConnected = false;

    private ScanSettings settings;
    private List<ScanFilter> filters;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) Exit("E: BLE Not Supported");
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
            }
            if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_FINE_LOCATION);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
            settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
            filters = new ArrayList<>();
            scanLeDevice();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Exit();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) Exit("Bluetooth needed");
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void scanLeDevice() {
        if (mBluetoothLeScanner == null) mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        mBluetoothAdapter.cancelDiscovery();
        mBluetoothGatt = null;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!mConnected) {
                    mBluetoothLeScanner.stopScan(mScanCallback);
                    scanLeDevice();
                }
            }
        }, 10000);
        mBluetoothLeScanner.startScan(filters, settings, mScanCallback);
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (mBluetoothGatt == null) {
                mBluetoothDevice = result.getDevice();
                if (mBluetoothDevice.getName().equals(CONNECT_TO)) {
                    mBluetoothGatt = mBluetoothDevice.connectGatt(getApplicationContext(), true, mBluetoothGattCallback);
                    mBluetoothLeScanner.stopScan(mScanCallback);
                } else mBluetoothDevice = null;
            }
        }
    };

    private final BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    mConnected = true;
                    gatt.discoverServices();
                    Notify("Connected"); // TOAST MESSAGE ON CONNECT
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    mConnected = false;
                    Notify("Disconnected"); // TOAST MESSAGE ON DISCONNECT
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            mBluetoothGattService = gatt.getService(UUID_BLE_HM10_SERVICE);
            mBluetoothGattCharacteristic = mBluetoothGattService.getCharacteristic(UUID_BLE_HM10_RX_TX);
            gatt.setCharacteristicNotification(mBluetoothGattCharacteristic, true);
            gatt.readCharacteristic(mBluetoothGattCharacteristic);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Notify(new String(characteristic.getValue())); // TOAST MESSAGE ON DATA RECEIVED
        }
    };

    /* EXAMPLE HOW TO SEND STRING
    private void sendMessage(String str) {
        if (mConnected) {
            mBluetoothGattCharacteristic.setValue(str.getBytes());
            mBluetoothGatt.writeCharacteristic(mBluetoothGattCharacteristic);
        }
    }
    */

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (grantResults.length > 0) {
            switch (requestCode) {
                case PERMISSION_REQUEST_COARSE_LOCATION:
                case PERMISSION_REQUEST_FINE_LOCATION: {
                    if (grantResults[0] != PackageManager.PERMISSION_GRANTED) Exit("Permission needed");
                    break;
                }
            }
        }
    }

    public void Notify(final String str) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(),str,Toast.LENGTH_LONG).show();
            }
        });
    }

    public void Exit(String str) {
        Notify(str);
        Exit();
    }

    public void Exit() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
        finishAndRemoveTask();
        System.exit(0);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Exit();
    }
}
