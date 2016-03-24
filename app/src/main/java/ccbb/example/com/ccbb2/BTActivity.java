package ccbb.example.com.ccbb2;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import ccbb.example.com.ccbb2.bluetooth.BluetoothCommandService;


public class BTActivity extends Activity implements View.OnClickListener{
    // Layout view
    private TextView mTitle;

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothCommandService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for Bluetooth Command Service

    private BluetoothCommandService mCommandService = null;
    private Switch switchConnectBT;
    private Button              switchToIR;
    private Button              btnSpeedUp;
    private Button              btnSpeedDown;
    private Button              btnForward;
    private Button              btnBackward;
    private Button              btnLeft;
    private Button              btnRight;

    private static final String TAG = "BT comm:";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bt);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        initComponents();
    }

    private void initComponents() {
        switchConnectBT = (Switch) findViewById(R.id.switch1);
        switchConnectBT.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                try {
                    if (b) {
                        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(BluetoothCommandService.address);
                        // Attempt to connect to the device
                        mCommandService.connect(device);

                    }
                } catch (Exception ex) {
                    switchConnectBT.setChecked(false);
                    Log.e(TAG, "Error connecting to BT device", ex);
                }
            }
        });

        // Set up the custom title
        mTitle = (TextView) findViewById(R.id.textView);
        mTitle.setText("init");

        switchToIR = (Button)findViewById(R.id.buttonIR);
        switchToIR.setOnClickListener(this);

        btnSpeedUp = (Button)findViewById(R.id.SpeedUp);
        btnSpeedUp.setOnClickListener(this);

        btnSpeedDown = (Button) findViewById(R.id.SpeedDown);
        btnSpeedDown.setOnClickListener(this);

        btnForward = (Button) findViewById(R.id.Forward);
        btnForward.setOnClickListener(this);

        btnBackward = (Button) findViewById(R.id.Backward);
        btnBackward.setOnClickListener(this);

        btnLeft = (Button) findViewById(R.id.Left);
        btnLeft.setOnClickListener(this);

        btnRight = (Button) findViewById(R.id.Right);
        btnRight.setOnClickListener(this);

    }

    @Override
    protected void onStart() {
        super.onStart();

        // If BT is not on, request that it be enabled.
        // setupCommand() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
        // otherwise set up the command service
        else {
            if (mCommandService==null)
                setupCommand();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mCommandService != null) {
            if (mCommandService.getState() == BluetoothCommandService.STATE_NONE) {
                mCommandService.start();
            }
        }
    }

    private void setupCommand() {
        // Initialize the BluetoothChatService to perform bluetooth connections
        mCommandService = new BluetoothCommandService(this, mHandler);
    }

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothCommandService.STATE_CONNECTED:
                            mTitle.setText("connected");
                            mTitle.append(mConnectedDeviceName);
                            break;
                        case BluetoothCommandService.STATE_CONNECTING:
                            mTitle.setText("connecting");
                            break;
                        case BluetoothCommandService.STATE_LISTEN:
                        case BluetoothCommandService.STATE_NONE:
                            mTitle.setText("not connected");
                            break;
                    }
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    @Override
    public void onClick(View view) {
        // Forward-a, Left-b, Right-c, Stop-d, Wait-e, SpeedUp-1, SpeedDown-2, None-0
        switch (view.getId()) {
            case R.id.buttonIR:
                Intent intent = new Intent(BTActivity.this, DetectionActivity.class);
                startActivity(intent);
                break;
            case R.id.Forward:
                //mCommandService.write(BluetoothCommandService.VOL_UP);
                mCommandService.write("a".getBytes());
                break;
            case R.id.Left:
                mCommandService.write("b".getBytes());
                //writeData("b");
                break;
            case R.id.Right:
                mCommandService.write("c".getBytes());
                //writeData("c");
                break;
            case R.id.Backward:
                mCommandService.write("d".getBytes());
                //writeData("d");
                break;
            case R.id.SpeedDown:
                mCommandService.write("2".getBytes());
                //writeData("2");
                break;
            case R.id.SpeedUp:
                mCommandService.write("1".getBytes());
                //writeData("1");
                break;
        }
    }
}
