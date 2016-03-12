package ccbb.example.com.ccbb2.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by gil on 17/02/2016.
 */
public class BlueToothMgr {
    private static BlueToothMgr blueToothMgr = new BlueToothMgr();
    private static final String TAG = "BTMgr:";
    private BluetoothDevice btDevice;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;
    private InputStream inStream = null;
    private static final String address = "98:D3:31:50:22:ED";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BlueToothMgr() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public static BlueToothMgr getInstance(){
        return blueToothMgr;
    }
    public boolean isConnected(){
        return btSocket.isConnected();
    }

    public void disconnect() {
        try {
            btSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean connect() {
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        try {
            btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            btSocket.connect();
            Log.d(TAG, "Connection made.");
        } catch (IOException e) {
            Log.e(TAG, "Socket creation failed",e);
            try {
                btSocket.close();
                return false;
            } catch (IOException e2) {
                Log.e(TAG, "Unable to end the connection", e2);
                return false;
            }
        }
        return true;
    }

    public boolean sendMsgToDevice(String msg) {
        try {
            outStream = btSocket.getOutputStream();
        } catch (Exception e) {
            Log.e(TAG, "error getting output stream",e);
            return false;
        }

        try {
            outStream.write(msg.getBytes());
        } catch (Exception e) {
            Log.e(TAG, "error write into output stream",e);
            return false;
        }
        return true;
    }
}
