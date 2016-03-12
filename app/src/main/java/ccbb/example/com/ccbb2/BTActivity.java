package ccbb.example.com.ccbb2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import ccbb.example.com.ccbb2.bluetooth.BlueToothMgr;


public class BTActivity extends Activity implements View.OnClickListener{

    private Switch              connectBT;
    private Button              switchToIR;
    private Button              btnSpeedUp;
    private Button              btnSpeedDown;
    private Button              btnForward;
    private Button              btnBackward;
    private Button              btnLeft;
    private Button              btnRight;

    private BlueToothMgr blueToothMgr;
    private static final String TAG = "BT comm:";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bt);

        initComponents();
    }

    private void initComponents() {
        connectBT = (Switch) findViewById(R.id.switch1);
        connectBT.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                try{
                    if(b){
                        blueToothMgr = BlueToothMgr.getInstance();
                        blueToothMgr.connect();
                        if(blueToothMgr.isConnected()){
                            Toast.makeText(getApplicationContext(), "BlueTooth connected!", Toast.LENGTH_SHORT).show();
                        }else{
                            Toast.makeText(getApplicationContext(), "BlueTooth NOT connected!", Toast.LENGTH_SHORT).show();
                        }
                    }else{
                        blueToothMgr.disconnect();
                    }
                }catch (Exception ex){
                    connectBT.setChecked(false);
                    Log.e(TAG, "Error connecting to BT device", ex);
                }

            }
        });
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
    public void onClick(View view) {
        // Forward-a, Left-b, Right-c, Stop-d, Wait-e, SpeedUp-1, SpeedDown-2, None-0
        switch (view.getId()) {
            case R.id.buttonIR:
                Intent intent = new Intent(BTActivity.this, DetectionActivity.class);
                startActivity(intent);
                break;
            case R.id.Forward:
                writeData("a");
                break;
            case R.id.Left:
                writeData("b");
                break;
            case R.id.Right:
                writeData("c");
                break;
            case R.id.Backward:
                writeData("d");
                break;
            case R.id.SpeedDown:
                writeData("2");
                break;
            case R.id.SpeedUp:
                writeData("1");
                break;
        }
    }

    private void writeData(String data) {
        if(connectBT.isChecked()){
            if(!blueToothMgr.isConnected()){
                blueToothMgr.connect();
            }
            if(!blueToothMgr.sendMsgToDevice(data)){
                Log.i(TAG, "message send FAILED" + data);
            }else{
                Log.i(TAG,"message send Successfully " + data);
            }

        }

    }

}
