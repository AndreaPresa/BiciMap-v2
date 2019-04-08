package com.example.bicimap;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.DatePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bicimap.Bluetooth.BluetoothActivity;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.google.maps.android.heatmaps.WeightedLatLng;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static java.lang.Math.toRadians;
import static java.lang.System.currentTimeMillis;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback{

    //mapa
    private GoogleMap mMap;
    private Context mContext;
    private GoogleApiClient client;



    //localizacion
    private FusedLocationProviderClient mFusedLocationClient;

    private LocationManager locationManager;
    private boolean gps_enabled=false;
    private boolean network_enabled=false;

    private Location lastlocation;
    private Location lastlocation2;
    private Location getLastlocation3=null;
    private long time1;
    private long time2;
    double latitud, longitud;

    //botones
    private FloatingActionButton location_onButton;
    private FloatingActionButton fb_onButton;
    private FloatingActionButton BT_onButton;
    private Button start_onButton;
    private boolean mMap_locationFlag = false;
    private boolean FB_flag=false;

    //Bluetooth
    private String MAC_ADDRESS="";
    private char start_PM='p';
    private char read_PM = 'r';
    private char finish_PM='s';
    private boolean PM_flag=false;

    private int N_PM=1;
    private int PM_FB_counter;
    private ArrayList<Integer> PMData;
    private List<ArrayList<Integer>> PMData_array;
    private int PMData_counter;
    private final int PMData_max = 10;
    private boolean save_loc_PM_flag=false;


    //fecha
    private Calendar myCalendar;
    private String today;
    private SimpleDateFormat sdf;
    private String date="fecha";
    private long dateMillis;
    private long DIA_MILLIS=86400000;

    //HeatMap
    List<WeightedLatLng> latLngList=null;
    private TileOverlay mOverlay;
    private DatabaseReference dbLocations=null;
    private DatabaseReference dbLocations1=null;

    //permisos
     private String[] PERMISSIONS = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,

    };



    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        supportMapFragment.getMapAsync(this);
        mContext= MainActivity.this;

        latLngList = new ArrayList<>();

        //PERMISOS

        if(!hasPermissions(this, PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, 1);
        }


        //Preferencias
        SharedPreferences datosUsuario=getSharedPreferences("Preferencias",MODE_PRIVATE);
        SharedPreferences.Editor editor = datosUsuario.edit();


        FB_flag = false;
        //Inicializo PMDataCounter
        PMData_counter=0;
        //Declaro el vector de enteros y lo inicializo
        PMData = new ArrayList<Integer>();
        for(int i=0; i<N_PM; i++) {
            PMData.add(i,0);
        }

        PMData_array = new ArrayList<ArrayList<Integer>>(PMData_max);
        for(int i=0; i<PMData_max; i++) {
            PMData_array.add(i,PMData);
        }



        //Inicializo PM_FB_Counter
        PM_FB_counter=0;

        //Registro el Broadcast para recibir el dato de PM
        LocalBroadcastManager.getInstance(mContext).registerReceiver(mMessageReceiver,
                new IntentFilter("PM_Data"));

        //Recibo intents cuando vuelvo de bluetooth activity
        Intent intent = getIntent();
        intent.getExtras();

        //localizacion

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(mContext);

        //obtener localizacion
        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {

                        if (location != null) {
                            time1=currentTimeMillis();
                            lastlocation=location;
                            LatLng primeraLoc =new LatLng (location.getLatitude(), location.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(primeraLoc, 12));
                        }
                    }
                });

        gestorManager();

        //Temporizador
        final Handler mHandler = new Handler();

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mHandler.postDelayed(this, 2000);
                gestorManager();
                save_location(lastlocation);

            }
        }, 2000);

        //fecha actual
        myCalendar= Calendar.getInstance();
        String myFormat = "dd-MM-yyyy";
        sdf = new SimpleDateFormat(myFormat, Locale.FRANCE);
        today = sdf.format(myCalendar.getTime());


        //BOTONES

        //Boton localizacion
        location_onButton = findViewById(R.id.btn_Loc);
        int id = getResources().getIdentifier("ic_location_off",
                "drawable", "com.example.bicimap");
        location_onButton.setImageResource(id);
        location_onButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {


                    if (mMap_locationFlag) {
                        mMap_locationFlag = !mMap_locationFlag;
                        int id = getResources().getIdentifier("ic_location_on",
                                "drawable", "com.example.bicimap");
                        location_onButton.setImageResource(id);
                        mMap.setMyLocationEnabled(false);


                    } else if (!mMap_locationFlag) {
                        mMap_locationFlag = !mMap_locationFlag;
                        int id = getResources().getIdentifier("ic_location_off",
                                "drawable", "com.example.bicimap");
                        location_onButton.setImageResource(id);
                        mMap.setMyLocationEnabled(true);

                    }

                }
            }
        });

        //boton firebase (solo para verla)
        fb_onButton = findViewById(R.id.btn_fb);
        fb_onButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(MainActivity.this,
                            FirebaseActivity.class);
                    Bundle b = new Bundle();
                    b.putString("date", date);
                    intent.putExtra("onlyRead",b);
                    startActivity(intent);

                }
            });

        //Boton Bluetooth
        BT_onButton = findViewById(R.id.btn_BT);
        BT_onButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    Intent intent = new Intent(MainActivity.this,
                        BluetoothActivity.class);
                    startActivity(intent);
                    finish();
            }
        });


        //Boton comenzar recorrido

        start_onButton = findViewById(R.id.btn_start);
        start_onButton.setVisibility(View.GONE);
        if (intent.getExtras() != null) {
            if (intent.getBundleExtra("BTdevice") != null) {
                start_onButton.setVisibility(View.VISIBLE);
                Bundle bundle = intent.getBundleExtra("BTdevice");
                MAC_ADDRESS = bundle.getString("mac");

                editor.putString("MAC", MAC_ADDRESS);
                editor.commit();

                Log.d("MAC ADDRESS = ",""+MAC_ADDRESS);
                Intent newIntent = new Intent(MainActivity.this,BluetoothService.class);
                startService(newIntent);
            }}

        start_onButton.setOnClickListener(new View.OnClickListener() {
            Intent intent = new Intent(MainActivity.this,
                    BluetoothService.class);
            @TargetApi(21)
            @Override
            public void onClick(View view) {

                if(MAC_ADDRESS!=""){
                    if(!PM_flag) {
                        start_onButton.setBackgroundTintList(ColorStateList.
                                valueOf(getResources().getColor(R.color.colorPrimary)));
                        //start_onButton.setText(R.string.finish);
                        intent.putExtra("PM", start_PM);
                        PM_flag=true;
                        startService(intent);
                        //Bandera que permite la escritura automatica en Firebase
                        FB_flag = true;
                    }

                    else {
                        //start_onButton.setText(R.string.finish2);
                        intent.putExtra("PM", finish_PM);
                        PM_flag=false;
                        startService(intent);
                        FB_flag=false;

                    }

                }}
            });


                //Implementacion de acciones de entrada y salida de barra navegacion
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

    }

    @Override
    protected void onResume(){

        super.onResume();
        FB_flag=false;

    }

    @Override
    protected void onPause() {
        super.onPause();

    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
        LatLng madrid = new LatLng(40.4167754, -3.7037901999999576);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(madrid, 12));


    }

    LocationListener locationListenerNetwork = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            time2=time1;
            lastlocation2=lastlocation;
            time1=currentTimeMillis();
            lastlocation=location;
            latitud = location.getLatitude();
            longitud = location.getLongitude();
            Log.d("lat = ",""+latitud);
            // LatLng latLng = new LatLng(latitud , longitud);
            //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,15));

        }

        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        public void onProviderEnabled(String provider) {

        }

        public void onProviderDisabled(String provider) {

        }


    };

    LocationListener locationListenerGPS = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            time2=time1;
            lastlocation2=lastlocation;
            time1=currentTimeMillis();
            lastlocation=location;
            latitud = location.getLatitude();
            longitud = location.getLongitude();
            Log.d("lat = ",""+latitud);
           // LatLng latLng = new LatLng(latitud , longitud);
            //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,15));

        }

        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        public void onProviderEnabled(String provider) {

        }

        public void onProviderDisabled(String provider) {

        }


    };


    public void gestorManager(){

        if(locationManager == null){
            locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        }

        try{
            gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        }catch(Exception ex){

        }
        try{
            network_enabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        }catch(Exception ex){

        }

        //don't start listeners if no provider is enabled
        if(!gps_enabled && !network_enabled){

        }
        //if available, get gps location
        if(ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if(gps_enabled){

                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        1000,
                        5,
                        locationListenerGPS
                );
            } else if(network_enabled){

                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                        1000,
                        5,
                        locationListenerNetwork
                );
            }

        }

    }

    private void save_location (Location location){
        lastlocation  = location;
        if (FB_flag==true) {
            if (lastlocation == null){
                Toast.makeText(getApplicationContext(), "Localización nula",
                        Toast.LENGTH_SHORT).show();
            }
            else {
                Intent bindIntent = new Intent(mContext, BluetoothService.class);
                bindIntent.putExtra("PM", read_PM);
                startService(bindIntent);
                if(PMData.size()!=0){
                if (PMData.get(0)!= 0 && save_loc_PM_flag) {
                    latitud = location.getLatitude();
                    longitud = location.getLongitude();
                    Bundle b = new Bundle();
                    b.putDouble("latitud", latitud);
                    b.putDouble("longitud", longitud);
                    b.putIntegerArrayList("pm", PMData_array.get(PM_FB_counter)); //Si estoy en modo Firebase o Re
                    b.putString("date", today);
                    b.putDouble("speed", getVelocidad());


                    if (PM_FB_counter == PMData_max - 1) {
                        PM_FB_counter = 0;
                    } else {
                        PM_FB_counter++;
                    }
                    save_loc_PM_flag = false; //Espero hasta la siguiente medida correcta
                    Intent i = new Intent(MainActivity.this, FirebaseActivity.class);
                    i.putExtra("bundleFire", b);
                    startActivity(i);
                }
            }
            }

        }

    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            PMData = intent.getIntegerArrayListExtra("TestData");
                if (PMData.get(0) != 0) {
                    save_loc_PM_flag = true; //He recibido una medida correcta!
                    PMData_array.set(PMData_counter,PMData);
                    //Cuando llega a 10 elementos, sobreeescribo el vector
                    if (PMData_counter == PMData_max - 1) {
                        PMData_counter = 0;
                    } else {
                        PMData_counter++;
                    }

            }

        }


    };




//Navigation bar
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        final DatePickerDialog.OnDateSetListener newdate = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear,
                                  int dayOfMonth) {
                myCalendar.set(Calendar.YEAR, year);
                myCalendar.set(Calendar.MONTH, monthOfYear);
                myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateLabel();
            }

        };

        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.map_normal) {
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            removeHeatmap();

        } else if (id == R.id.map_satelite) {
            mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
            removeHeatmap();

        } else if (id == R.id.map_hibrido)  {
            mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
            removeHeatmap();

        } else if (id == R.id.contaminacionmap)  {

            new DatePickerDialog(MainActivity.this, R.style.MyDialogTheme,
                    newdate, myCalendar.get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                    myCalendar.get(Calendar.DAY_OF_MONTH)).show();

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void addHeatMap(){

        if(latLngList.size()!=0) {
            HeatmapTileProvider mProvider = new HeatmapTileProvider.Builder()
                    .weightedData(latLngList)
                    .radius(25)
                    .build();
            mOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));
            mOverlay.setVisible(true);
        }


    }

    private void removeHeatmap(){
        if(latLngList.size()!=0){
            mOverlay.remove();
        }
    }

    private void updateLabel() {

        date = sdf.format(myCalendar.getTime());

        dateMillis=DatetoMillis(date);

        //Actualizo el valor de la referencia cada vez que se cambia de fecha

        dbLocations1 = FirebaseDatabase.getInstance().getReference().child("locations");

        dbLocations1.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {

                    String mac = postSnapshot.getKey();

                    dbLocations = FirebaseDatabase.getInstance().getReference().child("locations").child(mac);

                    dbLocations.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if(latLngList!=null){
                            latLngList.clear();
                            }

                            for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                                com.example.bicimap.FBDataFiles.FBData loc;
                                loc = postSnapshot.getValue(com.example.bicimap.FBDataFiles.FBData.class);

                                Long name = Long.parseLong(postSnapshot.getKey());

                                if(name<(dateMillis+DIA_MILLIS)&&(name>dateMillis)) {
                                    LatLng latLng = new LatLng(loc.getLat(), loc.getLon());
                                    List<Integer> PMlist = new ArrayList<>();
                                    PMlist = loc.getPMlist();
                                    int pm = PMlist.get(0);
                                    WeightedLatLng data = new WeightedLatLng(latLng, pm);
                                    latLngList.add(data);
                                }
                            }

                            if(latLngList.size()== 0){
                                Toast.makeText(mContext, "No hay datos para la fecha elegida", Toast.LENGTH_LONG).show(); } else {
                                addHeatMap();
                                }

                        }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                    }
                    });

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private long DatetoMillis(String date){
        long Millis;
        long mes1=0;
        int año=Integer.parseInt(date.substring(6,10));
        int mes=Integer.parseInt(date.substring(3,5));
        int dia=Integer.parseInt(date.substring(0,2));
        int diasbisiestos= (año-1970)/4;
        switch (mes) {
            case 1: mes1=0; break;
            case 2: mes1=31; break;
            case 3: mes1=59; break;
            case 4: mes1= 90; break;
            case 5: mes1=120; break;
            case 6: mes1=151; break;
            case 7: mes1=181; break;
            case 8: mes1=212; break;
            case 9: mes1= 243; break;
            case 10: mes1=273; break;
            case 11: mes1=304; break;
            case 12: mes1=334; break;
        }

        Millis=((año-1970)*365+mes1+dia+diasbisiestos-1)*DIA_MILLIS;
    return Millis;}

    private double getVelocidad(){
        if((lastlocation2!=null) && (lastlocation2!=getLastlocation3)) {
            getLastlocation3=lastlocation2;
            double r = 6378100;
            double v_lat = toRadians(lastlocation.getLatitude()-lastlocation2.getLatitude());
            double v_lon = toRadians( lastlocation.getLongitude()- lastlocation2.getLongitude());
            double a  = sin(v_lat/2)*sin(v_lat/2) + cos(toRadians(lastlocation.getLatitude())) * cos(toRadians(lastlocation2.getLatitude())) *sin(v_lon/2)*sin(v_lon/2);
            double c= 2 * Math.atan2(sqrt(a), sqrt(1-a));
            double distancia = r * c;
            double velocidad = distancia / ((time1 - time2) * 0.001);

            return velocidad ;
        }else{
            return 0;}

    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }


}
