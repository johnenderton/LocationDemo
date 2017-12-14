package com.example.user.locationdemo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.location.Address;
import android.location.Geocoder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.util.List;

public class AddLocationActivity extends AppCompatActivity {
    private final String LOG_TAG = "TestApp................";

    // Firebase variables
    private FirebaseDatabase mFirebaseDatabse;
    private DatabaseReference mMessageDatabaseReference;
    private ChildEventListener mChileEventListener;

    private Button mSendButton;
    private Button mgeneratell;
    private EditText name;
    private EditText type;
    private EditText lat;
    private EditText lng;
    private EditText address;
    private boolean validLatLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_location);

        // Initialize Firebase
        mFirebaseDatabse = FirebaseDatabase.getInstance();
        mMessageDatabaseReference = mFirebaseDatabse.getReference().child("markers");

        mSendButton = (Button)findViewById(R.id.btn_add);
        mgeneratell = (Button)findViewById(R.id.btn_generate_ll);
        name = (EditText)findViewById(R.id.editText_Name);
        type = (EditText)findViewById(R.id.editText_Type);
        lat = (EditText)findViewById(R.id.editText_Lat);
        lng = (EditText)findViewById(R.id.editText_Long);
        address = (EditText)findViewById(R.id.editText_Address);

        // if there are text in address editText, the "mgenerate" button is set to be clickable
        if (address.getText().length() > 0) {
            mgeneratell.setEnabled(true);
        } else {
            mgeneratell.setEnabled(false);
        }


        address.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() > 0) {
                    mgeneratell.setEnabled(true);
                } else {
                    mgeneratell.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        // generate latitude and longtitude according to address
        mgeneratell.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lat.setText("");
                lng.setText("");
                String location = address.getText().toString();
                List<Address> addressList = null;

                if (location != null || !location.equals("")) {
                    Geocoder geocoder = new Geocoder(v.getContext());
                    try {
                        addressList = geocoder.getFromLocationName(location, 1);
                        validLatLng = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                        validLatLng = false;
                        AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                        builder.setTitle("Location Error");
                        builder.setMessage("Invalid Location");
                        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });
                        builder.show();
                    }
                    Address address = addressList.get(0);
                    lat.setText(Double.toString(address.getLatitude()));
                    lng.setText(Double.toString(address.getLongitude()));
                }
            }
        });

        // the "mSendButton" is clickable if validLatLng is set true and editText "name" has text
        if (validLatLng && name.getText().length() > 0) {
            mSendButton.setEnabled(true);
        } else {
            mSendButton.setEnabled(false);
        }

        // send data to firebase
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String myaddress = address.getText().toString();
                if (myaddress.matches("")) {
                    location mylocation = new location(
                            Double.parseDouble(lat.getText().toString()),
                            Double.parseDouble(lng.getText().toString()),
                            name.getText().toString(),
                            type.getText().toString(),
                            null);
                    mMessageDatabaseReference.push().setValue(mylocation);
                    lat.setText("");
                    lng.setText("");
                } else {
                    location mylocation = new location(
                            Double.parseDouble(lat.getText().toString()),
                            Double.parseDouble(lng.getText().toString()),
                            name.getText().toString(),
                            type.getText().toString(),
                            address.getText().toString());
                    mMessageDatabaseReference.push().setValue(mylocation);
                    lat.setText("");
                    lng.setText("");
                    address.setText("");
                }

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(LOG_TAG, "Activity start");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(LOG_TAG, "Activity stop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(LOG_TAG, "Activity Destroy");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(LOG_TAG, "Activity Pause");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(LOG_TAG, "Activity Resume");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.i(LOG_TAG, "Activity Restart");
    }
}
