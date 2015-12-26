package ccbb.example.com.ccbb2;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;


public class BTActivity extends Activity implements View.OnClickListener{

    private Button              btnConnect;
    private Button              btnIRActivity;
    private Button              btnForward;
    private Button              btnBackward;
    private Button              btnLeft;
    private Button              btnRight;

    private Switch              schLed;
    private boolean             onOffLed = false;

    private static final String TAG = "BT comm:";
    private BluetoothDevice     btDevice;
    private BluetoothAdapter    mBluetoothAdapter = null;
    private BluetoothSocket     btSocket = null;
    private OutputStream        outStream = null;
    private InputStream         inStream = null;
    private static String       address = "98:D3:31:50:22:ED";
    private static final UUID   MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private Handler handler = new Handler();
    private byte delimiter = 10;
    private boolean stopWorker = false;
    private int readBufferPosition = 0;
    private byte[] readBuffer = new byte[1024];
    private String              dataToSend;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bt);

        initComponents();
        initBTComponents();

    }



    private void initComponents() {
        btnConnect = (Button)findViewById(R.id.buttonConnect);
        btnConnect.setOnClickListener(this);

        btnIRActivity = (Button) findViewById(R.id.buttonIR);
        btnIRActivity.setOnClickListener(this);

        btnForward = (Button) findViewById(R.id.Forward);
        btnForward.setOnClickListener(this);

        btnBackward = (Button) findViewById(R.id.Backward);
        btnBackward.setOnClickListener(this);

        btnLeft = (Button) findViewById(R.id.Left);
        btnLeft.setOnClickListener(this);

        btnRight = (Button) findViewById(R.id.Right);
        btnRight.setOnClickListener(this);

        schLed = (Switch) findViewById(R.id.switchLed);
        schLed.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                onOffLed = b;
                writeData(onOffLed ? "1" : "0");
                Toast.makeText(getApplicationContext(),onOffLed ? "LED ON" : "LED OFF",Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initBTComponents() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.buttonConnect:
                connect();
                break;
            case R.id.buttonIR:
                Intent intent = new Intent(BTActivity.this, DetectionActivity.class);
                startActivity(intent);
                break;
            case R.id.Forward:
                writeData("f");
                break;
            case R.id.Backward:
                writeData("b");
                break;
            case R.id.Left:
                writeData("l");
                break;
            case R.id.Right:
                writeData("r");
                break;
        }
    }

    private void writeData(String data) {
        try {
            outStream = btSocket.getOutputStream();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(),"Error in : outStream",Toast.LENGTH_SHORT).show();
        }

        String message = data;
        byte[] msgBuffer = message.getBytes();

        try {
            outStream.write(msgBuffer);
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(),"Error in : outStream.write",Toast.LENGTH_SHORT).show();
        }
    }

    private void connect() {
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        try {
            btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            btSocket.connect();
            Log.d(TAG, "Connection made.");
            Toast.makeText(getApplicationContext(), "Connected",Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) {
                Log.d(TAG, "Unable to end the connection");
            }
            Log.d(TAG, "Socket creation failed");
            Toast.makeText(getApplicationContext(), "Connection Failed",Toast.LENGTH_LONG).show();
        }
    }
}
