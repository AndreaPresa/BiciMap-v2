package com.example.bicimap.FBDataFiles;

import android.view.View;

public class FBData {private double lat;
    private double lon;
    private int pm;
    private String dh;
    private View mView;
    public FBData(){

    }

    public FBData(double lat, double lon, int pm, String dh)
    {
        this.lat = lat;
        this.lon = lon;
        this.pm  = pm;
        this.dh = dh;
    }

    public double getLat(){
        return lat;
    }

    public void setLat(double lat){
        this.lat = lat;
    }

    public double getLon(){ return lon;}

    public void setLon(double lon){
        this.lon = lon;
    }

    public int getPm() { return pm;}

    public void setPm(int pm) { this.pm = pm; }

    public String getDh() { return dh;}

    public void setDh(String dh) {this.dh = dh;}

    public String getDate(){
        String Date= getDh().substring(0,9);
        return Date;
    }

    public String getTime(){
        String Time= getDh().substring(11,18);
        return Time;

    }


    @Override
    public String toString() {
        return "Localizacion{" +
                ", latitud='" + lat + '\'' +
                ", longitud=" + lon +
                '}';
    }


}






