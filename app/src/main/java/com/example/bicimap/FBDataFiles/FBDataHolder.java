package com.example.bicimap.FBDataFiles;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.example.bicimap.R;

import java.util.List;

public class FBDataHolder extends RecyclerView.ViewHolder {

    private int N_PM=1;
    private View mView;

    public FBDataHolder(View itemView) {
        super(itemView);
        mView = itemView;
    }

    public void setLatitud(double lat) {
        TextView field = (TextView) mView.findViewById(R.id.lblLat);
        String latitud = Double.toString(lat);
        field.setText(latitud);
    }

    public void setLongitud(double lon) {
        TextView field1 = (TextView) mView.findViewById(R.id.lblLong);
        String longitud = Double.toString(lon);
        field1.setText(longitud);    }


    public void setPM(List<Integer> pm) {
        TextView field1 = (TextView) mView.findViewById(R.id.lblPM);
        String particulas="";
        if(pm!=null) {
            for (int i = 0; i < N_PM; i++) {
                particulas = particulas + Integer.toString(pm.get(i)) + "  ";
            }
        }
            field1.setText(particulas);

    }

    public void setDH(double speed) {
        TextView field1 = (TextView) mView.findViewById(R.id.lblFecha);
        String sp=String.valueOf(speed);
        field1.setText(sp);    }

}
