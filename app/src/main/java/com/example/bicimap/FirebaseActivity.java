package com.example.bicimap;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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
import java.text.SimpleDateFormat;
import java.util.Calendar;

import static java.lang.System.currentTimeMillis;

public class FirebaseActivity extends AppCompatActivity {


    private int CONEXION_WIFI=1;
    private int CONEXION_DATOS_MOVILES=2;
    private int SIN_CONEXION=0;

    private int contador_locs = 0;
    private RecyclerView recycler;
    private DatabaseReference dbReference;

    private boolean adapter_Flag=false;

    private FloatingActionButton clear_button;
    private Calendar calendar; //Para recoger fecha y hora
    private SimpleDateFormat df;
    private String formattedDate;

    private String dateMaps="Sin datos al escribir";
    private TextView dateTitle;

    private FirebaseRecyclerAdapter mAdapter;

    private long count;

    private String filePath="";

    private String MAC_ADDRESS="";
    private Context mContext;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firebase);
        mContext= FirebaseActivity.this;

        //Preferencias, para obtener MAC_ADDRESS
        SharedPreferences datosUsuario=getSharedPreferences("Preferencias",MODE_PRIVATE);
        SharedPreferences.Editor editor = datosUsuario.edit();
        MAC_ADDRESS = datosUsuario.getString("MAC", MAC_ADDRESS);

        //Archivo CSV
            //Obtiene ruta de sdcard
            File pathToExternalStorage = Environment.getExternalStorageDirectory();
            //agrega directorio /myFiles
            File appDirectory = new File(pathToExternalStorage.getAbsolutePath() + "/documents/");
            //Crea archivo
            File saveFilePath = new File(appDirectory, "DataBase.csv");

            if(saveFilePath.exists()) {
                filePath = datosUsuario.getString("CSV", filePath);
            }else{
                //Si no existe el directorio, se crea usando mkdirs() y se guarda en SharedPrefenrences
                appDirectory.mkdirs();
                filePath = saveFilePath.toString();
                editor.putString("CSV", filePath);
                editor.commit();
            }


        //Creo el formato para apuntar la fecha y la hora del experimento en FB
        df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

        //Recibo el primer intent al crear la actividad
        Intent intent = getIntent();
        intent.getExtras();

        dateTitle = (TextView) findViewById(R.id.dateTitle);

        //Referencia Firebase
        dbReference =
                FirebaseDatabase.getInstance().getReference()
                        .child("locations");

        //BOTONES
        clear_button = (FloatingActionButton) findViewById(R.id.btn_backToMap);
        clear_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dbReference =
                        FirebaseDatabase.getInstance().getReference()
                                .child("locations").child(MAC_ADDRESS);
                //dbLocations.removeValue();
                Intent i = new Intent(FirebaseActivity.this, MainActivity.class);
/*
                          //Los modos de Flag son necesarios para que FB_flag no cambie de valor solo...
                          //Lo que se hace es que se crea una nueva task y se eliminan las demás actividades
*/
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                onPause();
            }
        });


        RecyclerView recycler = findViewById(R.id.lstLocations);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setHasFixedSize(true);


        //Creo el bundle que recibira la nueva localizacion para apuntarla
        if (intent.getExtras() != null) {
            if (intent.getBundleExtra("bundleFire") != null) {
                Bundle bundle = intent.getBundleExtra("bundleFire");
                double lat = bundle.getDouble("latitud");
                double lon = bundle.getDouble("longitud");
                int pm = bundle.getInt("pm");
                dateMaps = bundle.getString("date");
                dateTitle.setText(dateMaps);
                //Para escribir la hora tambien
                calendar = Calendar.getInstance();
                if(calendar!=null){
                    formattedDate = df.format(calendar.getTime());}
                else {formattedDate = " ";}

                FBData newFBData = new FBData();
                newFBData.setLat(lat);
                newFBData.setLon(lon);
                newFBData.setPm(pm);
                newFBData.setDh(formattedDate);

                //Identificador de tiempo en milisegundos
                count = currentTimeMillis();
                String contador = String.valueOf(count);
                writeNewLocation(contador, newFBData);


                //VISUALIZACION
                if (MAC_ADDRESS != null) {

                    mAdapter =
                            new FirebaseRecyclerAdapter<FBData, FBDataHolder>(
                                    FBData.class, R.layout.layout_fb_adapter,
                                    FBDataHolder.class,
                                    dbReference.child(MAC_ADDRESS)) {

                                @Override
                                public void populateViewHolder(FBDataHolder viewHolder,
                                                               FBData data,
                                                               int position) {
                                    viewHolder.setLatitud(data.getLat());
                                    viewHolder.setLongitud(data.getLon());
                                    viewHolder.setPM(data.getPm());
                                    viewHolder.setDH(data.dataDh());

                                }
                            };
                    recycler.setAdapter(mAdapter);
                }

            }
            else if (intent.getBundleExtra("onlyRead") != null) {

                int connectionState=isNetworkConnected(mContext);
                if(connectionState==CONEXION_WIFI||connectionState==CONEXION_DATOS_MOVILES) {
                    CSVtoFB();
                }

                Bundle b = intent.getBundleExtra("onlyRead");
                dateMaps = b.getString("date");
                dateTitle.setText(dateMaps);
                mAdapter =
                        new FirebaseRecyclerAdapter<FBData, FBDataHolder>(
                                FBData.class, R.layout.layout_fb_adapter, FBDataHolder.class, dbReference.child(MAC_ADDRESS)) {


                            @Override
                            public void populateViewHolder(FBDataHolder viewHolder, FBData data, int position) {
                                viewHolder.setLatitud(data.getLat());
                                viewHolder.setLongitud(data.getLon());
                                viewHolder.setPM(data.getPm());
                                viewHolder.setDH(data.dataDh());

                            }
                        };

                recycler.setAdapter(mAdapter);
                adapter_Flag=true;



            }

        }
    }


    //Este metodo recoge el intent que se le envia de nuevo al ser LaunchMode=SingleTop



    protected void onNewIntent(Intent intent){


        intent.getExtras();

        if (intent.getExtras()!=null) {
            if (intent.getBundleExtra("bundleFire") != null) {


                Bundle bundle = intent.getBundleExtra("bundleFire");
                double lat = bundle.getDouble("latitud");
                double lon = bundle.getDouble("longitud");
                int pm = bundle.getInt("pm");
                calendar = Calendar.getInstance();
                if(calendar!=null){
                    formattedDate = df.format(calendar.getTime());}
                else {formattedDate = " ";}

                dateMaps = bundle.getString("date");
                dateTitle.setText(dateMaps);

                FBData newFBData = new FBData();
                newFBData.setLat(lat);
                newFBData.setLon(lon);
                newFBData.setPm(pm);
                newFBData.setDh(formattedDate);

                //Identificador de tiempo en milisegundos
                count = currentTimeMillis();
                String contador = String.valueOf(count);
                writeNewLocation(contador, newFBData);


            }

            else if (intent.getBundleExtra("onlyRead") != null) {

                int connectionState=isNetworkConnected(mContext);
                if(connectionState==CONEXION_WIFI||connectionState==CONEXION_DATOS_MOVILES) {
                    CSVtoFB();
                }

                Bundle b =
                        intent.getBundleExtra("onlyRead");
                dateMaps = b.getString("date");

                //Si no se ha instanciado el adapter antes
                if (!adapter_Flag) {
                    mAdapter =
                            new FirebaseRecyclerAdapter<FBData, FBDataHolder>(
                                    FBData.class, R.layout.layout_fb_adapter, FBDataHolder.class, dbReference.child(MAC_ADDRESS)) {


                                @Override
                                public void populateViewHolder(FBDataHolder viewHolder, FBData loc, int position) {
                                    viewHolder.setLatitud(loc.getLat());
                                    viewHolder.setLongitud(loc.getLon());
                                    viewHolder.setPM(loc.getPm());
                                    viewHolder.setDH(loc.dataDh());

                                }
                            };
                    recycler.setAdapter(mAdapter);
                }
            }
        }
    }

    @Override
    protected void onResume(){
        super.onResume();

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    private void writeNewLocation(String locs, FBData loc) {
        int connectionState=isNetworkConnected(mContext);
        if(connectionState==CONEXION_WIFI){
            writeFirebase(locs, loc);
        }else if(connectionState==CONEXION_DATOS_MOVILES){
            writeFirebase(locs, loc);
        }else{
            writeCSV(loc);
        }
    }

    private void writeCSV(FBData loc) {
        try {
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
            file.append(String.valueOf(loc.getPm()));
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
        String pm="";
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
                         }
                         contador++;
                     } else {
                         aux = aux + (char)c ;
                     }
                 } else {
                     pm = aux;
                     aux = "";
                     FBData newFBData = new FBData();
                     newFBData.setLat(Double.parseDouble(lat));
                     newFBData.setLon(Double.parseDouble(lon));
                     newFBData.setPm(Integer.parseInt(pm));

                     writeFirebase(time, newFBData);

                     contador = 0;
                 }
             }

            File file = new File(filePath);
            file.delete();

        } catch (FileNotFoundException e) {
            Log.i("File not found",e.toString());
        } catch (IOException e) {
            Log.i("Error",e.toString());
        }

        Toast.makeText(getApplicationContext(), "Tu información ha sido añadida a la base de datos",
                Toast.LENGTH_SHORT).show();

    }

}