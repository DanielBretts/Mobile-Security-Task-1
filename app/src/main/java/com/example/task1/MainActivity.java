package com.example.task1;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private final String TAG = "MAIN";
    private final int SENSOR_DELAY_MICROS = 500000; // Delay between events in microseconds
    private MaterialButton submit_BTN;
    private EditText username_edittext;
    private EditText password_edittext;
    private ShapeableImageView batteryIMG;
    private ShapeableImageView flashlightIMG;
    private ShapeableImageView contactsIMG;
    private ShapeableImageView directionIMG;
    private int NUM_OF_SECRETS = 4;
    private boolean directionNorth = false;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;

    private float[] accelerometerReading = new float[3];
    private float[] magnetometerReading = new float[3];

    private float[] rotationMatrix = new float[9];
    private float[] orientationAngles = new float[3];
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        CmManager.registerFlashlightState(this);
        checkPermissions();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        findViews();
        initViews();

    }

    private void checkPermissions() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_CONTACTS},0);
        }
    }

    private void initViews() {
        submit_BTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int num = 0;
                if(checkPassEqualsBattery()){
                    num++;
                    batteryIMG.setBackgroundColor(getResources().getColor(R.color.correct));
                }
                if(checkContactExists()){
                    num++;
                    contactsIMG.setBackgroundColor(getResources().getColor(R.color.correct));
                }
                if(checkFlashlightOn()){
                    num++;
                    flashlightIMG.setBackgroundColor(getResources().getColor(R.color.correct));
                }if(directionNorth){
                    num++;
                    directionIMG.setBackgroundColor(getResources().getColor(R.color.correct));
                }
                if(num == NUM_OF_SECRETS){
                    openSuccessIntent();
                }
            }
        });
    }

    private void openSuccessIntent() {
        Intent success = new Intent(getApplicationContext(),SuccessActivity.class);
        startActivity(success);
    }

    private boolean checkFlashlightOn() {
        return CmManager.isFlashlightOn;
    }

    private boolean checkContactExists() {
        List<String> contact = new ArrayList<>();
        ContentResolver contentResolver = getContentResolver();
        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        Cursor cursor = contentResolver.query(uri,null,null,null,null);
        if(cursor.getCount() > 0){
            while (cursor.moveToNext()){
                @SuppressLint("Range") String contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                contact.add(contactName);
            }
        }
        return contact.contains(username_edittext.getText().toString());
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SENSOR_DELAY_MICROS);
        sensorManager.registerListener(this, magnetometer, SENSOR_DELAY_MICROS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.length);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.length);
        }

        boolean success = SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading);

        if (success) {
            SensorManager.getOrientation(rotationMatrix, orientationAngles);

            float azimuth = (float) Math.toDegrees(orientationAngles[0]);
            if (azimuth < 0) {
                azimuth += 360;
            }
            Log.d(TAG, "onSensorChanged: "+azimuth);
            if (azimuth >= 0 && azimuth < 22.5 || azimuth >= 337.5 && azimuth < 360 || azimuth >= 292.5 && azimuth < 337.5) {
                // North and North West
                Log.d(TAG, "onSensorChanged: North");
                directionIMG.setBackgroundColor(getResources().getColor(R.color.correct));
                directionNorth = true;
            }else{
                Log.d(TAG, "onSensorChanged: OTHER");
                directionIMG.setBackgroundColor(getResources().getColor(R.color.black));
                directionNorth = false;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private boolean checkPassEqualsBattery() {
        float batteryPct = getBatteryPct();
        if(password_edittext.getText().toString().equals(String.valueOf((int) batteryPct))){
            return true;
        }
        Toast.makeText(this, "Wrong Pass!", Toast.LENGTH_SHORT).show();
        return false;
    }

    private void findViews() {
        submit_BTN = findViewById(R.id.submit_BTN);
        password_edittext = findViewById(R.id.password_edittext);
        username_edittext = findViewById(R.id.username_edittext);
        batteryIMG = findViewById(R.id.batteryIMG);
        flashlightIMG =  findViewById(R.id.flashlightIMG);
        contactsIMG =  findViewById(R.id.contactsIMG);
        directionIMG = findViewById(R.id.directionIMG);
    }

    private float getBatteryPct() {
        IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = this.registerReceiver(null, iFilter);
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        float batteryPct = level * 100 / (float)scale;
        return batteryPct;
    }


}