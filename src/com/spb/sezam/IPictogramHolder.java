package com.spb.sezam;

import com.spb.sezam.management.GroupPictogram;

import android.view.View;
import android.widget.Button;

/**
 * Must be implemented by classes who would be able to send pictograms(images) in messages
 * @author Serob
 *
 */
public interface IPictogramHolder {
	
	View.OnClickListener getOnPictogramClickListener();
}
