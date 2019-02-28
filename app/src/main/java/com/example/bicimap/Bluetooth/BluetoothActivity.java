package com.example.bicimap.Bluetooth;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.PermissionRequest;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.example.bicimap.BluetoothService;
import com.example.bicimap.MainActivity;
import com.example.bicimap.R;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BluetoothActivity extends AppCompatActivity {
    BluetoothAdapter mBluetoothAdapter;
    Button btn_find;
    ArrayList<BluetoothDevice> MyDevicesFound=new ArrayList<>();
    private int DeviceCounter=0;
    private RecyclerView recyclerView;
    private BTAdapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;
    Context mcontext;
    private String MAC_ADDRESS="";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);
        mcontext=this;

        recyclerView = (RecyclerView) findViewById(R.id.lst_BTDevices);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


        //Enciendo Bluetooth
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, RESULT_OK);
        }

        IntentFilter filter = new IntentFilter();

        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        registerReceiver(mReceiver, filter);


        //boton buscar dispositivos
        btn_find = findViewById(R.id.btn_find);
        btn_find.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBluetoothAdapter.startDiscovery();

            }
        });


    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i("Bluetooth", "got action " + action);
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceMAC = device.getAddress();
                DeviceCounter++;
                MyDevicesFound.add(device);
                //String[] devicesFound = MyDevicesFound.toArray(new String[0]);

                mAdapter = new BTAdapter(MyDevicesFound);
                recyclerView.setAdapter(mAdapter);

                //listener de Recycler View
                mAdapter.setOnItemClickListener(new BTAdapter.onItemClickListener(){
                    @Override
                    public void onItemClick( int position){
                        MAC_ADDRESS = MyDevicesFound.get(position).getAddress();
                        mBluetoothAdapter.cancelDiscovery();
                        Intent intent = new Intent(BluetoothActivity.this,
                                MainActivity.class);
                        Bundle b = new Bundle();
                        b.putString("mac", MAC_ADDRESS);
                        intent.putExtra("BTdevice",b);
                        startActivity(intent);
                    }
                });

                Log.i("Bluetooth", "got device " + deviceName );
            }
        }
    };



}

