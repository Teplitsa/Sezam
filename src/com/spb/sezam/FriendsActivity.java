package com.spb.sezam;

import java.io.Serializable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.spb.sezam.utils.ErrorUtil;
import com.vk.sdk.VKUIHelper;
import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKRequest.VKRequestListener;
import com.vk.sdk.api.VKResponse;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

public class FriendsActivity extends Activity {

	private final static String EXTRA_MESSAGE = "com.spbu.sezam.MESSAGE";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_friends);
		
		VKRequest request = VKApi.friends().get(VKParameters.from(VKApiConst.FIELDS, "id,first_name,last_name,sex,bdate"));
		
		VKRequestListener loadFriendsListener = new VKRequestListener() {

			@Override
			public void onComplete(VKResponse response) {
				try {
					showFriends(response.json.getJSONObject("response").getJSONArray("items"));
				} catch (JSONException e) {
					e.printStackTrace();
					ErrorUtil.showError(FriendsActivity.this	, "Error happened!");
				}
			}

			@Override
			public void onError(VKError error) {
				ErrorUtil.showError(FriendsActivity.this	, error);
			}
		};
		
		request.executeWithListener(loadFriendsListener);
	}
	
	private void showFriends(JSONArray friendsJson) throws JSONException {
		//create Sezam Bot for test messages
		JSONObject sezamBot = new JSONObject();

		//shift array
		friendsJson.put(friendsJson.length(),"");
		int count = friendsJson.length();
		for(int i = count-1; i > 0; i--){
			friendsJson.put(i, friendsJson.get(i-1));
		}
		
		//add sezamBot
		friendsJson.put(0, sezamBot);
		
		Button[] btnArray = new Button[count];
		LinearLayout friends = (LinearLayout)findViewById(R.id.friends);
		for (int i = 0; i < count; i++) {
			btnArray[i] = new Button(this);
			friends.addView(btnArray[i]);
		}
		updateFirends(friendsJson);
	}
	
	//in first step assume that friends count is the same
	private void updateFirends(JSONArray friendsJson) throws JSONException {
		LinearLayout friends = (LinearLayout)findViewById(R.id.friends);
		int count = friends.getChildCount();
		Button b = null;
		String textToShow = null;
		String onlineStatus = "";
		//JSONObject friend = null;  
		for (int i = 0; i < count; i++) {
		    b = (Button)friends.getChildAt(i);
		    final JSONObject friend = friendsJson.getJSONObject(i);
			onlineStatus = (friend.getInt("online") == 1) ? " (online)" : "";
		    textToShow = friend.getString("first_name") + " " + friend.getString("last_name") + onlineStatus;
		   
		    b.setText(textToShow);
		    b.setId(friend.getInt("id"));
			b.setOnClickListener(new View.OnClickListener() {
	            @Override
	            public void onClick(View view) {
	            	startActivityWithData(MessageActivity.class, friend.toString());
	            }
	        });
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		VKUIHelper.onResume(this);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		VKUIHelper.onDestroy(this);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		VKUIHelper.onActivityResult(this, requestCode, resultCode, data);
	}
	
	private void startActivityWithData(Class<? extends Activity> a, Serializable data){
	     Intent startNewActivityOpen = new Intent(this, a);
	     startNewActivityOpen.putExtra(EXTRA_MESSAGE, data);
	     startActivityForResult(startNewActivityOpen, 0);
	}
}
