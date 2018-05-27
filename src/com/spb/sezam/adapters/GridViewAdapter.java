package com.spb.sezam.adapters;

import java.util.ArrayList;
import java.util.List;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.spb.sezam.IPictogramHolder;
import com.spb.sezam.R;
import com.spb.sezam.management.Pictogram;
import com.spb.sezam.widged.GridViewItem;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class GridViewAdapter extends BaseAdapter {

	private final Context context;
	private List<Pictogram> pictograms = new ArrayList<>();
	private final IPictogramHolder picHolder;
	
	
	
	public GridViewAdapter(Context context, IPictogramHolder picHolder,  List<Pictogram> pictograms){
		this.pictograms = pictograms;
		this.context = context;
		this.picHolder = picHolder;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View elementView = convertView;
		
		// reuse views
		if(elementView == null){
			LayoutInflater inflater = (LayoutInflater)context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			elementView = inflater.inflate(R.layout.pictogram_layout, parent, false);
			GridViewHolder viewHolder = new GridViewHolder();
			viewHolder.pictogramIcon =  (GridViewItem)elementView.findViewById(R.id.pictogramIcon);
			elementView.setTag(viewHolder);
		}
		
		// fill data
		GridViewHolder holder = (GridViewHolder) elementView.getTag();
		
		Pictogram pic = getItem(position);
		holder.setPictogram(pic);
		//holder.pictogramIcon.setContentDescription(pic.getPath());
		
		ImageLoader imageLoader = ImageLoader.getInstance();
		imageLoader.displayImage(pic.getPathWithAssests(), holder.pictogramIcon);
		
		//was a niche method to use :)
		holder.pictogramIcon.setOnClickListener(picHolder.getOnPictogramClickListener());
		return elementView;
		
	}
	
	public void updateView(List<Pictogram> pictograms) {
        this.pictograms = pictograms;
        notifyDataSetChanged();
    }

	@Override
	public int getCount() {
		return pictograms.size();
	}

	@Override
	public Pictogram getItem(int position) {
		return pictograms.get(position);
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}
}

