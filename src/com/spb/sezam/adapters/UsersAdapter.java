package com.spb.sezam.adapters;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.spb.sezam.R;
import com.spb.sezam.R.drawable;
import com.spb.sezam.R.id;
import com.spb.sezam.R.layout;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class UsersAdapter extends ArrayAdapter<JSONObject> {

	private final Context context;
	private final List<JSONObject> users;
	
	static class ViewHolder {
		 TextView text;
		 ImageView onlineIcon;
		 TextView unreadCount;
	}
	
	public UsersAdapter(Context context, List<JSONObject> users) {
		super(context, R.layout.row_layout, users);
		this.context = context;
		this.users = users;
	}
	
	@Override
	public int getCount() {
		return users.size();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View rowView = convertView;

		// reuse views
		if (rowView == null) {
			LayoutInflater inflater = (LayoutInflater)context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			rowView = inflater.inflate(R.layout.row_layout, null);
			// configure view holder
			ViewHolder viewHolder = new ViewHolder();
			viewHolder.text = (TextView) rowView.findViewById(R.id.user_label);
			viewHolder.onlineIcon = (ImageView) rowView
					.findViewById(R.id.online_status);
			viewHolder.unreadCount = (TextView) rowView
					.findViewById(R.id.has_unread);
			rowView.setTag(viewHolder);
		}

		// fill data
		ViewHolder holder = (ViewHolder) rowView.getTag();
		JSONObject user = users.get(position);
		String username = "";
		int onlineStatus = 0;
		String unreadMessagesCount = null;
		try {
			username = user.getString("first_name") + " "+ user.getString("last_name");
			onlineStatus = user.getInt("online");
			// maybe maybe unreadmessage here
			unreadMessagesCount = user.getString("unread_count");
		} catch (JSONException e) {
			e.printStackTrace();
		}

		holder.text.setText(username);
		
		if (onlineStatus == 1) {
			holder.onlineIcon.setImageResource(R.drawable.online);
		} else {
			holder.onlineIcon.setImageResource(R.drawable.ofline);
		}
		
		//has unread messages
		if (unreadMessagesCount != null && !"0".equals(unreadMessagesCount)) {
			holder.unreadCount.setText("+" + unreadMessagesCount); //number anyway
			holder.unreadCount.setBackgroundResource(R.drawable.unread_message);
		} else {
			//no unread message
			holder.unreadCount.setText("");
			holder.unreadCount.setBackgroundResource(0);
		}
		
		return rowView;
	}

	
}
