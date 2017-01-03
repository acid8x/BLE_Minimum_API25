# BLE_Minimum_API25

Minimum needed to use BLE with API25 and HM-10 CC2540 4.0 BLE bluetooth adapter

---

Values to change:

String CONNECT_TO -> BLE device name

UUID UUID_BLE_HM10_RX_TX -> BLE device serial characteristic

UUID UUID_BLE_HM10_SERVICE -> BLE device service

---

Data received line 153:

Data received in byte[] -> characteristic.getValue()

---

Data send line 158-163:

Data to send in String -> str

mBluetoothGattCharacteristic.setValue(str.getBytes());
mBluetoothGatt.writeCharacteristic(mBluetoothGattCharacteristic);
