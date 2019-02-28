package com.example.bicimap;

import android.content.Intent;
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class FirebaseActivity extends AppCompatActivity {

/*
    private static final String TAGLOG = "firebase-db";
*/

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

    private String dateFB = "Sin datos al mostrar";
    private FileWriter writer;
    private String fileName="DataBase.csv";
    private BufferedWriter writer2;
    private File myFile;
    private long count;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firebase);

        //FirebaseApp.initializeApp(FirebaseActivity.this);

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
                                .child("locations").child(dateMaps);
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
                count = System.currentTimeMillis();
                String contador = String.valueOf(count);
                String loc_1 = "loc" + contador;
                writeNewLocation(loc_1, newFBData);


                //VISUALIZACION
                if (dateMaps != null) {

                    mAdapter =
                            new FirebaseRecyclerAdapter<FBData, FBDataHolder>(
                                    FBData.class, R.layout.layout_fb_adapter,
                                    FBDataHolder.class,
                                    dbReference.child(dateMaps)) {

                                @Override
                                public void populateViewHolder(FBDataHolder viewHolder,
                                                               FBData data,
                                                               int position) {
                                    viewHolder.setLatitud(data.getLat());
                                    viewHolder.setLongitud(data.getLon());
                                    viewHolder.setPM(data.getPm());
                                    viewHolder.setDH(data.getDh());

                                }
                            };
                    recycler.setAdapter(mAdapter);
                }

            }
            else if (intent.getBundleExtra("onlyRead") != null) {
                Bundle b = intent.getBundleExtra("onlyRead");
                dateMaps = b.getString("date");
                dateTitle.setText(dateMaps);
                mAdapter =
                        new FirebaseRecyclerAdapter<FBData, FBDataHolder>(
                                FBData.class, R.layout.layout_fb_adapter, FBDataHolder.class, dbReference.child(dateMaps)) {


                            @Override
                            public void populateViewHolder(FBDataHolder viewHolder, FBData data, int position) {
                                viewHolder.setLatitud(data.getLat());
                                viewHolder.setLongitud(data.getLon());
                                viewHolder.setPM(data.getPm());
                                viewHolder.setDH(data.getDh());

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
                count = System.currentTimeMillis();
                String contador = String.valueOf(count);
                String loc_1 = "loc" + contador;
                writeNewLocation(loc_1, newFBData);


            }

            else if (intent.getBundleExtra("onlyRead") != null) {
                Bundle b =
                        intent.getBundleExtra("onlyRead");
                dateMaps = b.getString("date");

                //Si no se ha instanciado el adapter antes
                if (!adapter_Flag) {
                    mAdapter =
                            new FirebaseRecyclerAdapter<FBData, FBDataHolder>(
                                    FBData.class, R.layout.layout_fb_adapter, FBDataHolder.class, dbReference.child(dateMaps)) {


                                @Override
                                public void populateViewHolder(FBDataHolder viewHolder, FBData loc, int position) {
                                    viewHolder.setLatitud(loc.getLat());
                                    viewHolder.setLongitud(loc.getLon());
                                    viewHolder.setPM(loc.getPm());
                                    viewHolder.setDH(loc.getDh());

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
        writeFirebase(locs, loc);
        writeCSV(loc);
    }

    private void writeCSV(FBData loc) {
        //Obtiene ruta de sdcard
        File pathToExternalStorage = Environment.getExternalStorageDirectory();
        //agrega directorio /myFiles
        File appDirectory = new File(pathToExternalStorage.getAbsolutePath()+ "/documents/");
        //Si no existe la estructura, se crea usando mkdirs()
        appDirectory.mkdirs();
        //Crea archivo
        File saveFilePath = new File(appDirectory,"DataBase.csv");

        try {
            FileOutputStream fos = new FileOutputStream(saveFilePath);
            OutputStreamWriter file = new OutputStreamWriter(fos);
            file.append(loc.getDate());
            file.append(";");
            file.append(loc.getTime());
            file.append(";");
            file.append(String.valueOf(loc.getLat()));
            file.append(";");
            file.append(String.valueOf(loc.getLon()));
            file.append(";");
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
        String dateFB = loc.getDh();
        dateFB= dateFB.substring(0,10);
        dbReference.push().child(dateFB);
        dbReference.child(dateFB).push().child(locs);
        dbReference.child(dateFB).child(locs).setValue(loc);
    }


}