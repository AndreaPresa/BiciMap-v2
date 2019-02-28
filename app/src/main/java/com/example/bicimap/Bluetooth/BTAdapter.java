package com.example.bicimap.Bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.bicimap.R;

import java.util.ArrayList;

public class BTAdapter extends RecyclerView.Adapter<BTAdapter.ViewHolder> {
    private ArrayList<BluetoothDevice> mDataset;
    private onItemClickListener mListener;

    public interface onItemClickListener{
        void onItemClick(int position);
    }
    public void setOnItemClickListener( onItemClickListener listener){
        mListener=listener;
    }
    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView name;
        public TextView mac;

        public ViewHolder(View v, final onItemClickListener listener) {
            super(v);
            name= v.findViewById(R.id.bt_Name);
            mac= v.findViewById(R.id.bt_MAC);

            v.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    if (listener!=null){
                        int position=getAdapterPosition();
                        if(position!=RecyclerView.NO_POSITION){
                            listener.onItemClick(position);
                        }
                    }
                }
            });
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public BTAdapter(ArrayList<BluetoothDevice> myDataset) {
        mDataset = myDataset;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public BTAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.bluetooth_adapter, parent, false);

        return new ViewHolder(v, mListener);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        BluetoothDevice device = mDataset.get(position);

        holder.name.setText(device.getName());
        holder.mac.setText(device.getAddress());
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }


}
