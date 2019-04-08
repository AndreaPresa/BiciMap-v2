package com.example.bicimap;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bicimap.Bluetooth.BluetoothActivity;
import com.example.bicimap.FBDataFiles.FBData;
import com.example.bicimap.FBDataFiles.FBDataHolder;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

import static java.lang.Math.sqrt;
import static java.lang.System.currentTimeMillis;

public class FirebaseActivity extends AppCompatActivity implements  SensorEventListener {


    private int CONEXION_WIFI=1;
    private int CONEXION_DATOS_MOVILES=2;
    private int SIN_CONEXION=0;

    private int N_PM=1;

    //sensor
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private double aceleracion=0;
    private int cont_ac=0;

    private int contador_locs = 0;
    private RecyclerView recycler;
    private DatabaseReference dbReference;

    private boolean adapter_Flag=false;


    private Calendar calendar; //Para recoger fecha y hora
    private SimpleDateFormat df;
    private String formattedDate;

    private String dateMaps="Sin datos al escribir";
    private TextView numberPM;
    private TextView numberSpeed;

    private FirebaseRecyclerAdapter mAdapter;

    private long count;

    private String filePath="";

    private String MAC_ADDRESS="";
    private Context mContext;

    //fecha
    private Calendar myCalendar;
    private String today;
    private SimpleDateFormat sdf;

    //progress bar
    ProgressBar myProgressBar;

    //observaciones
    private CheckBox humos, olores, trafico, semaforos, viento, lluvia, vegetacion, cruce, obras;
    private EditText otros;
    private String observaciones = "";
    private Button btn_otros;
    private Button btn_end;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progressbar);

        mContext= FirebaseActivity.this;

        //Progress bar
        myProgressBar = (ProgressBar) findViewById(R.id.progress_bar);

        //formato velocidad
        DecimalFormat precision = new DecimalFormat("0.00");

        //declacion de checkbox
        humos=(CheckBox)findViewById(R.id.humos);
        olores=(CheckBox)findViewById(R.id.olor);
        trafico=(CheckBox)findViewById(R.id.trafico);
        semaforos=(CheckBox)findViewById(R.id.semaforo);
        viento=(CheckBox)findViewById(R.id.viento);
        lluvia=(CheckBox)findViewById(R.id.lluvia);
        vegetacion=(CheckBox)findViewById(R.id.vegetacion);
        cruce=(CheckBox)findViewById(R.id.cruce);
        obras=(CheckBox)findViewById(R.id.obras);
        otros=(EditText)findViewById(R.id.otros);

        btn_otros=(Button)findViewById(R.id.btn_otros);
        btn_end=(Button)findViewById(R.id.btn_end);


        //Listeners
        btn_otros.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                observaciones = observaciones + otros.getText();
                otros.setText("");
            }
        });
        btn_end.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                startActivity(new Intent(getBaseContext(), MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                finish();

            }
        });

                //Sensor
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        //Preferencias, para obtener MAC_ADDRESS
        SharedPreferences datosUsuario=getSharedPreferences("Preferencias",MODE_PRIVATE);
        SharedPreferences.Editor editor = datosUsuario.edit();
        MAC_ADDRESS = datosUsuario.getString("MAC", MAC_ADDRESS);

        //fecha actual
        myCalendar= Calendar.getInstance();
        String myFormat = "dd-MM-yyyy";
        sdf = new SimpleDateFormat(myFormat, Locale.FRANCE);
        today = sdf.format(myCalendar.getTime());

        //Archivo CSV
            //Obtiene ruta de sdcard
            File pathToExternalStorage = Environment.getExternalStorageDirectory();
            //agrega directorio /myFiles
            File appDirectory = new File(pathToExternalStorage.getAbsolutePath() + "/biciMAP/");
            //Crea archivo
            File saveFilePath = new File(appDirectory, today + ".csv");

            if(saveFilePath.exists()) {
                filePath = datosUsuario.getString("CSV", filePath);
            }else{
                //Si no existe el directorio, se crea usando mkdirs() y se guarda en SharedPrefenrences
                appDirectory.mkdirs();
                filePath = saveFilePath.toString();
                editor.putString("CSV", filePath);
                editor.commit();
            }

            //Doy valor a los textview
        numberPM=(TextView)findViewById(R.id.number_pm);
        numberSpeed=(TextView)findViewById(R.id.speed);

        //Creo el formato para apuntar la fecha y la hora del experimento en FB
        df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

        //Recibo el primer intent al crear la actividad
        Intent intent = getIntent();
        intent.getExtras();

        //Referencia Firebase
        dbReference =
                FirebaseDatabase.getInstance().getReference()
                        .child("locations");



        //Creo el bundle que recibira la nueva localizacion para apuntarla
        if (intent.getExtras() != null) {
            if (intent.getBundleExtra("bundleFire") != null) {
                Bundle bundle = intent.getBundleExtra("bundleFire");
                double lat = bundle.getDouble("latitud");
                double lon = bundle.getDouble("longitud");
                double vel = bundle.getDouble("speed");

                List<Integer> pm =  bundle.getIntegerArrayList("pm");
                dateMaps = bundle.getString("date");
                //Para escribir la hora tambien
                calendar = Calendar.getInstance();
                if(calendar!=null){
                    formattedDate = df.format(calendar.getTime());}
                else {formattedDate = " ";}

                //Actualizo Observaciones
                checkObservaciones();

                FBData newFBData = new FBData();
                newFBData.setLat(lat);
                newFBData.setLon(lon);
                newFBData.setPMlist(pm);
                newFBData.setDh(formattedDate);
                newFBData.setSpeed(vel);
                newFBData.setAceleracion(aceleracion);
                newFBData.setObservaciones(observaciones);
                observaciones = "";

                //Identificador de tiempo en milisegundos
                count = currentTimeMillis();
                String contador = String.valueOf(count);
                writeNewLocation(contador, newFBData);

                myProgressBar.setProgress(pm.get(0));
                numberPM.setText(String.valueOf(pm.get(0)));
                numberSpeed.setText(precision.format(vel));
            }

        }
    }


    //Este metodo recoge el intent que se le envia de nuevo al ser LaunchMode=SingleTop



    protected void onNewIntent(Intent intent){

        DecimalFormat precision = new DecimalFormat("0.00");
        intent.getExtras();

        if (intent.getExtras()!=null) {
            if (intent.getBundleExtra("bundleFire") != null) {

                //Recibimos datos de MainActivity
                Bundle bundle = intent.getBundleExtra("bundleFire");
                double lat = bundle.getDouble("latitud");
                double lon = bundle.getDouble("longitud");
                List<Integer> pm =  bundle.getIntegerArrayList("pm");
                double vel = bundle.getDouble("speed");
                calendar = Calendar.getInstance();
                if(calendar!=null){
                    formattedDate = df.format(calendar.getTime());}
                else {formattedDate = " ";}

                dateMaps = bundle.getString("date");

                //Actualizamos observaciones
                checkObservaciones();

                FBData newFBData = new FBData();
                newFBData.setLat(lat);
                newFBData.setLon(lon);
                newFBData.setPMlist(pm);
                newFBData.setDh(formattedDate);
                newFBData.setSpeed(vel);
                newFBData.setAceleracion(aceleracion);
                newFBData.setObservaciones(observaciones);
                observaciones = "";

                //Identificador de tiempo en milisegundos
                count = currentTimeMillis();
                String contador = String.valueOf(count);
                writeNewLocation(contador, newFBData);
                myProgressBar.setProgress(pm.get(0));
                numberPM.setText(String.valueOf(pm.get(0)));
                numberSpeed.setText(precision.format(vel));

            }



            }
        }





    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }
    @Override
    protected void onResume(){
        super.onResume();
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    private void writeNewLocation(String locs, FBData loc) {
        int connectionState=isNetworkConnected(mContext);
        if(connectionState==CONEXION_WIFI){
            writeFirebase(locs, loc);
            writeCSV(loc);
        }else if(connectionState==CONEXION_DATOS_MOVILES){
            writeFirebase(locs, loc);
            writeCSV(loc);
        }else{
            writeCSV(loc);
        }
    }

    private void writeCSV(FBData loc) {
        try {
            List<Integer> pm=loc.getPMlist();
            FileOutputStream fos = new FileOutputStream(filePath, true);
            OutputStreamWriter file = new OutputStreamWriter(fos);
            file.append(String.valueOf(currentTimeMillis()));
            file.append(",");
            file.append(loc.dataDate());
            file.append(",");
            file.append(loc.dataTime());
            file.append(",");
            file.append(String.valueOf(loc.getLat()));
            file.append(",");
            file.append(String.valueOf(loc.getLon()));
            file.append(",");
            file.append(String.valueOf(loc.getSpeed()));
            file.append(",");
            file.append(String.valueOf(loc.getAceleracion()));
            file.append(",");
            file.append(String.valueOf(pm.get(0)));
            file.append("\n");
            file.flush();
            file.close();
            Log.i("File found","DataBase edited");
        } catch (FileNotFoundException e) {
            Log.i("File not found",e.toString());
        } catch (IOException e) {
            Log.i("Error",e.toString());
        }
    }

    private void writeFirebase(String locs, FBData loc){
        //el metodo push() permite crear nuevos hijos sin sobreescribirlos
        dbReference.push().child(MAC_ADDRESS);
        dbReference.child(MAC_ADDRESS).push().child(locs);
        dbReference.child(MAC_ADDRESS).child(locs).setValue(loc);
        cont_ac=0;
    }

    public static int isNetworkConnected(Context context) {
        final ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            if (Build.VERSION.SDK_INT < 23) {
                final NetworkInfo ni = cm.getActiveNetworkInfo();

                if (ni != null) {
                    if (ni.isConnected() && (ni.getType() == ConnectivityManager.TYPE_WIFI)) {
                        return 1;
                    } else if ((ni.getType() == ConnectivityManager.TYPE_MOBILE)) {
                        return 2;
                    }
                }
            } else {
                final Network n = cm.getActiveNetwork();
                if (n != null) {
                    final NetworkCapabilities nc = cm.getNetworkCapabilities(n);
                    if (nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)){
                        return 2;
                    }else if(nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)){
                        return 1;
                    }
                }
            }
        }

        return 0;
    }

    private void CSVtoFB(){
        String time="";
        String lat="";
        String lon="";
        String vel="";
        String acel="";
        String pm="";
        List<Integer> PMlist =new ArrayList<>();
        for(int i=0; i<N_PM; i++) {
            PMlist.add(i,0);
        }
        String aux="";
        int c;
        int contador=0;
        try {

        //BufferedReader reader = new BufferedReader(new FileReader(filePath));
            FileReader reader = new FileReader(filePath);
             while ((c=reader.read())!=-1){
                 if(!(((char)c)==('\n'))) {
                     if (((char)c )==(',')) {
                         if (contador == 0) {
                             time = aux;
                             aux = "";
                         } else if (contador == 1) {
                             aux = "";
                         } else if (contador == 2) {
                             aux = "";
                         } else if (contador == 3) {
                             lat = aux;
                             aux = "";
                         } else if (contador == 4) {
                             lon = aux;
                             aux = "";
                         }else if (contador == 5) {
                             vel= aux;
                             aux = "";
                         }else if (contador == 6) {
                             acel = aux;
                             aux = "";
                         }
                         contador++;
                     } else {
                         aux = aux + (char)c ;
                     }
                 } else {
                     pm = aux;
                     aux = "";
                     for(int i=0; i<N_PM; i++) {
                         PMlist.set(i,Integer.parseInt(pm));
                     }
                     FBData newFBData = new FBData();
                     newFBData.setLat(Double.parseDouble(lat));
                     newFBData.setLon(Double.parseDouble(lon));
                     newFBData.setSpeed(Double.parseDouble(vel));
                     newFBData.setAceleracion(Double.parseDouble(acel));
                     newFBData.setPMlist(PMlist);

                     writeFirebase(time, newFBData);

                     contador = 0;
                 }
             }

            //File file = new File(filePath);
            //file.delete();

            Toast.makeText(getApplicationContext(), "Tu información ha sido añadida a la base de datos",
                    Toast.LENGTH_SHORT).show();

        } catch (FileNotFoundException e) {
            Log.i("File not found",e.toString());
            Toast.makeText(getApplicationContext(), "No hay información nueva para añadir a la base de datos",
                    Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.i("Error",e.toString());
        }



    }

    private void checkObservaciones(){

            if (trafico.isChecked()) {
                observaciones = observaciones + "trafico ";
            }
            if (semaforos.isChecked()) {
                observaciones = observaciones + "semaforos ";
            }
            if (obras.isChecked()) {
                observaciones = observaciones + "obras ";
            }
            if (humos.isChecked()) {
                observaciones = observaciones + "humos ";
            }
            if (olores.isChecked()) {
                observaciones = observaciones + "olores ";
            }
            if (cruce.isChecked()) {
                observaciones = observaciones + "cruce ";
            }
            if (vegetacion.isChecked()) {
                observaciones = observaciones + "vegetacion ";
            }
            if (lluvia.isChecked()) {
                observaciones = observaciones + "lluvia ";
            }
            if (viento.isChecked()) {
                observaciones = observaciones + "viento ";
            }

                trafico.setChecked(false);
                semaforos.setChecked(false);
                obras.setChecked(false);
                humos.setChecked(false);
                olores.setChecked(false);
                cruce.setChecked(false);
                vegetacion.setChecked(false);
                lluvia.setChecked(false);
                viento.setChecked(false);

            /*    String otras_obs = otros.getText().toString();
                if(otras_obs!="Otros"){
                    observaciones = observaciones + otras_obs;
                    otros.setText("Otros");
                }
                */
    }


    //SENSORES
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (cont_ac == 0) {
            aceleracion = sqrt(event.values[0]*event.values[0]+event.values[1]*event.values[1]+event.values[2]*event.values[2]);
            cont_ac++;
        }else{
            aceleracion = (aceleracion + sqrt(event.values[0]*event.values[0]+event.values[1]*event.values[1]+event.values[2]*event.values[2]))/(cont_ac+1);
            cont_ac++;

        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

}