package com.example.user.locationdemo;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private final String LOG_TAG = "TestApp................";
    private ImageButton addLocation;
    private ImageButton mgooglemapbtn;
    private Context mContext;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private TextView screenbrightness;
    private TextView txtwarning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(LOG_TAG, "Activity create");
        mContext = getApplicationContext();

        // initialize sensor variable
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        screenbrightness = (TextView)findViewById(R.id.txt_screen_brightness);
        txtwarning = (TextView)findViewById(R.id.txt_warning);

        addLocation = (ImageButton)findViewById(R.id.btn_ac_add);

        // Move to Add Location Activity
        addLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), AddLocationActivity.class);
                startActivity(intent);
            }
        });

        // Move to Google Map
        mgooglemapbtn = (ImageButton)findViewById(R.id.btn_map);
        mgooglemapbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), MapsActivity.class);
                startActivity(intent);
            }
        });
    }

    // if Sensor value < 50 or > 1000, show warning text
    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType()==Sensor.TYPE_LIGHT){
            float currentReading = event.values[0];
            screenbrightness.setText(Float.toString(currentReading));
            if (currentReading < 50) {
                txtwarning.setText("This place is a little dark");
            } else if (currentReading > 1000) {
                txtwarning.setText("This place is too bright");
            } else {
                txtwarning.setText("");
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(LOG_TAG, "Activity Pause");
        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(LOG_TAG,"Activity Resume");
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(LOG_TAG, "Activity Start");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(LOG_TAG, "Activity Stop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(LOG_TAG, "Activity Destroy");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.i(LOG_TAG, "Activity Restart");
    }
}
