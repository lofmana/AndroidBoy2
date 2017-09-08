package com.example.mdpgroup9.androidboy;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;

import java.util.List;

/**
 * Created by Jeremy Wong on 7/9/2017.
 */

public class GridAdapter extends ArrayAdapter<MapGrid> {
    public GridAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull List<MapGrid> objects) {
        super(context, resource, objects);
    }
 private class ViewHolder{
     LinearLayout grid;
 }
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        MapGrid dataModel = getItem(position); //retrieve pos info
        ViewHolder viewHolder;

        if (convertView == null) {
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.map_adapter, parent, false); //Apply inflate for each layout item
            viewHolder.grid = (LinearLayout) convertView.findViewById(R.id.gdisplay);

            convertView.setTag(viewHolder);
        } else
            viewHolder = (ViewHolder) convertView.getTag();
        return convertView;
    }
}
