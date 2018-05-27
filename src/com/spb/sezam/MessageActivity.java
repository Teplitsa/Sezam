package com.spb.sezam;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.spb.sezam.NavigationDrawerFragment.NavigationDrawerCallbacks;
import com.spb.sezam.adapters.GridViewAdapter;
import com.spb.sezam.adapters.GridViewHolder;
import com.spb.sezam.adapters.GroupAdapter;
import com.spb.sezam.management.ElementType;
import com.spb.sezam.management.GroupPictogram;
import com.spb.sezam.management.NameManager;
import com.spb.sezam.management.Pictogram;
import com.spb.sezam.management.PictogramManager;
import com.spb.sezam.utils.ErrorUtil;
import com.vk.sdk.VKScope;
import com.vk.sdk.VKSdk;
import com.vk.sdk.VKUIHelper;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKRequest.VKRequestListener;
import com.vk.sdk.api.VKResponse;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.LinearLayout;

public class MessageActivity extends BaseActivity implements NavigationDrawerCallbacks, IPictogramHolder{

	private static final String ICON_SPLIT_SYMBOLS = "|_";
	
	private static final int MESSAGE_RECIEVE_COUNT = 7;
	
	private List<String> messageToSend = new ArrayList<>();
	private JSONArray allMessages = new JSONArray();
	private String activeUserName = null;
	private int activeUserId;
	
	private GroupAdapter firstLevelGroupAdapter = null;
	private GroupAdapter subGroupAdapter = null;
	private GridViewAdapter gridViewAdapter = null;
	
	private GridView subGroupsView = null;
	private GridView pictogramsGridView = null;
	//in pixels
	private int historyImageSize = 0;
	
	private Runnable recieveMessagesRunnable = null;
	/** For all Users */
	private final Handler handler = new Handler();

	private View.OnClickListener onPictogramClickListener ;
	
	//--------------------------------VK listeners-----------------------------//
	private VKRequestListener messageSendListener  = new VKRequestListener(){

		@Override
		public void onComplete(VKResponse response) {
			LinearLayout formLayout = (LinearLayout) findViewById(R.id.linearLayout1);
			formLayout.removeAllViews();
			messageToSend.clear();
			Toast showSent = Toast.makeText(getApplicationContext(), "Сообщение отправлено", Toast.LENGTH_SHORT);
			showSent.show();
			recieveMessageHistory(MESSAGE_RECIEVE_COUNT);
		}

		@Override
		public void onError(VKError error) {
			ErrorUtil.showError(MessageActivity.this, error);
		}
		
	};
	
	private VKRequestListener messageRecieveListener  = new VKRequestListener(){
		@Override
		public void onComplete(VKResponse response) {    
	        //List<String[]> ourMessages = null; 
	        try {
	        	JSONArray messages = response.json.getJSONObject("response").getJSONArray("items");
	        	JSONArray newMessages = findNewMessages(allMessages, messages);
	        	int length = newMessages.length();
				if (length != 0) {
					showHistory(newMessages);
					scorllDown((ScrollView) findViewById(R.id.scrollView1));
					//mark received new messages as read
					//should be some trick here to prevent mark as read when drawer is opened
					StringBuilder messagsIds = new StringBuilder();
					//this approach is not good for first call
					for (int i = 0; i < length; i++) {
						int messId = newMessages.getJSONObject(i).getInt("id");
						messagsIds.append(messId);
						if (i != (length - 1)) {
							messagsIds.append(",");
						}
					}
					VKRequest request = new VKRequest("messages.markAsRead", VKParameters.from(
							"message_ids", messagsIds.toString()));
					request.executeWithListener(markAsReadListener);

				}
				allMessages = messages;
	            //
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onError(VKError error) {
			Log.e("Message recieve", "Error on Message recieve");
			ErrorUtil.showError(MessageActivity.this, error);
		}
	};
	
	private VKRequestListener markAsReadListener  = new VKRequestListener(){
		
		@Override
		public void onError(VKError error) {
		//	ActivityUtil.showError(MessageActivity.this, error);
		}
	};
	//---------------------------End of VK listenres---------------------------//
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		VKUIHelper.onCreate(this);
		//if not initialized in other activity (VKActivity ;)) then init
		if(VKSdk.instance() == null){
			initVKSdk();
	        VKSdk.wakeUpSession();
		}
		setContentView(R.layout.activity_message);
		subGroupsView = (GridView)findViewById(R.id.subGroups_view);
		pictogramsGridView = (GridView)findViewById(R.id.gridView1);
		historyImageSize = (int)(getResources().getDimension(R.dimen.new_message_height)/1.2);
		
		new ManagersInitializer().execute();
		initImageLoader();
		View view = findViewById(R.id.container);
		view.setVisibility(View.INVISIBLE); //or gone
		initOnPictogramClickListener();

		NavigationDrawerFragment mNavigationDrawerFragment = (NavigationDrawerFragment)
				getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
		
		mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_exit:
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage("Вы уверены?").setPositiveButton("Да", dialogClickListener).setNegativeButton("Нет", dialogClickListener).show();
				return true;
			case R.id.action_email:
				Intent i = new Intent(Intent.ACTION_SEND);
				i.setType("text/plain");
				i.putExtra(Intent.EXTRA_EMAIL, new String[]{"support@sezamapp.ru"});
				i.putExtra(Intent.EXTRA_SUBJECT, "Письмо администратору");
				i.putExtra(Intent.EXTRA_TEXT, "\nОтправлено с приложения Sezam");
				try {
					startActivity(Intent.createChooser(i, "Send mail..."));
				} catch (android.content.ActivityNotFoundException ex) {
					Toast.makeText(MessageActivity.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
				}
				return true;
			case R.id.action_about:
				AlertDialog.Builder aboutBuilder = new AlertDialog.Builder(this);
				aboutBuilder.setMessage(R.string.about_app)
						.setCancelable(false)
						.setPositiveButton("OK", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
							}
						});
				AlertDialog alert = aboutBuilder.create();
				alert.show();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	private DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
	    @Override
	    public void onClick(DialogInterface dialog, int which) {
	        switch (which){
	        case DialogInterface.BUTTON_POSITIVE:
	        	Log.e("token=", VKSdk.getAccessToken().userId);
	        	VKSdk.logout();
				handler.removeCallbacks(recieveMessagesRunnable);
	        	startActivity(VKActivity.class);
	        	setContentView(R.layout.activity_vk);

				Button b = (Button) findViewById(R.id.sign_in_button);
				// predefined in .xml as Войти
				if (VKSdk.wakeUpSession()) {
					Log.e("wakeUp", "wakeUp");
					b.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							VKSdk.logout();
							((Button) view).setText("Войти");
						}
					});
					return;
				}

				b.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						 String[] myScope = new String[] {
					         VKScope.FRIENDS,
					         VKScope.MESSAGES,
					         VKScope.OFFLINE
						 };						
						VKSdk.authorize(myScope);
					}
				});
	            
	            //'Yes' button clicked
	            break;
	        case DialogInterface.BUTTON_NEGATIVE:
	            //No button clicked
	            break;
	        }
	    }
	};	
	
	@Override
	public void onBackPressed() {
	    moveTaskToBack(true);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    // Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.activity_message_actions, menu);
	    return super.onCreateOptionsMenu(menu);
	}
	
	private void showHistory(JSONArray messages) throws JSONException{
		String messageString;
		JSONObject messageJson;
		String[] messageArr;
		LinearLayout historyLayout = (LinearLayout)findViewById(R.id.messageHistory);
		
		for (int i = messages.length() - 1; i >= 0; i--) {
			messageJson = messages.getJSONObject(i);
			TextView nameView = new TextView(MessageActivity.this);
			if(messageJson.getInt("out") == 1){
				nameView.setText("Я");
				nameView.setTextColor(Color.BLACK);
			} else {
				nameView.setText(activeUserName);
				nameView.setTextColor(Color.BLUE);
			}
			nameView.setTypeface(null, Typeface.BOLD);
			historyLayout.addView(nameView);
			
			messageString = messageJson.getString("body").trim();
			messageArr =  messageString.split("\\" + ICON_SPLIT_SYMBOLS);
			
			LinearLayout messageLinerLayout = new LinearLayout(MessageActivity.this);
			HorizontalScrollView messageScrollView = new HorizontalScrollView(MessageActivity.this);
			historyLayout.addView(messageScrollView);
			messageScrollView.addView(messageLinerLayout);
			
			//Analyze message parts
			for(String text : messageArr){
				showTextWithImages(text, messageLinerLayout);
			}
		}
	}
	
	/**
	 * Shows text into the history layout as it is. Should be called if there is no info about image icon in the text.
	 * @param text Text to be shown
	 * @param lLayout {@link TextView} with {@code 'text'} parameter will be added to this layout
	 */
	private void showTextAsString(String text, LinearLayout lLayout) {
		//if there is no info about image icon in the text
		if(text == null || "".equals(text)){
			return;
		}
		TextView textView = new TextView(MessageActivity.this);
		textView.setText(text);
		lLayout.addView(textView);
	}
	
	@Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
    	Log.w("Entered", "Entered here");
    }
	
	private void showTextWithImages(String text, LinearLayout lLayout){
		if(text == null || "".equals(text)){
			return;
		}
		
		ImageView image = new ImageView(MessageActivity.this);
		setImageViewSize(image, historyImageSize, 2);
		
		String path = NameManager.getInstance().getFileEngName(text);
		if(path != null){
			String pathWithAssets = "assets://" + PictogramManager.BASE_FOLDER + File.separator + path ;
			ImageLoader.getInstance().displayImage(pathWithAssets, image);
			lLayout.addView(image);
		} else {
			showTextAsString(text, lLayout);
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		VKUIHelper.onResume(this);
	}
	
	public void backButton(View v){
        LinearLayout formLayout = (LinearLayout)findViewById(R.id.linearLayout1);
        if(messageToSend.size() != 0){
        	formLayout.removeViewAt(formLayout.getChildCount() - 1 );
        	messageToSend.remove(messageToSend.size() - 1);
        	HorizontalScrollView hView = (HorizontalScrollView)findViewById(R.id.new_mess_scroll);
        	scrollRight(hView);
        }
	}	
	
	public void sendMessage(View v){
		if(messageToSend.size() > 0){
	        StringBuilder messageString = new StringBuilder();
	        for(String msg : messageToSend){
	        	messageString.append(msg);
	        }
	        long guId = new Date().getTime();
	        VKRequest request = new VKRequest("messages.send", VKParameters.from(
		        		"user_id", String.valueOf(activeUserId), 
		        		"message", messageString.toString(), "guid", guId));
			request.executeWithListener(messageSendListener);
		}
	}
	
	private void recieveMessageHistory(int messagesCount){
		if(messagesCount > 0){
			VKRequest request = new VKRequest("messages.getHistory", VKParameters.from("user_id", activeUserId, "count", messagesCount));
			request.executeWithListener(messageRecieveListener);
			
			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
			Date date = new Date();
			Log.i("Time messages", "Messages recive at " + sdf.format(date));
		}
	}
	
	private void addImageNameToSendMessages(String imageName){
		messageToSend.add(ICON_SPLIT_SYMBOLS + imageName + ICON_SPLIT_SYMBOLS); 
	}

	private void scorllDown(final ScrollView view) {
		setScrollViewDirection(view, ScrollView.FOCUS_DOWN);
	}
	
	private void scrollRight(final HorizontalScrollView view){
		setScrollViewDirection(view, ScrollView.FOCUS_RIGHT);
	}
	
	private void setScrollViewDirection(final ScrollView view, final int direction){
		view.post(new Runnable() {
	        @Override
	        public void run() {
	        	view.fullScroll(direction);
	        }
	    });
	}
	
	private void setScrollViewDirection(final HorizontalScrollView view, final int direction){
		view.post(new Runnable() {
	        @Override
	        public void run() {
	        	view.fullScroll(direction);
	        }
	    });
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		handler.removeCallbacks(recieveMessagesRunnable);
	}

	private void removeMessageHistory(){
		LinearLayout historyLayout = (LinearLayout)findViewById(R.id.messageHistory);
		historyLayout.removeAllViews();
	}
	
	private void recieveMessagePeriodicly() {
		recieveMessagesRunnable = new Runnable() {
			public void run() {
				recieveMessageHistory(MESSAGE_RECIEVE_COUNT);
				handler.postDelayed(this, 5000);
			}
		};
		handler.postDelayed(recieveMessagesRunnable, 5000);
	}
	
	private JSONArray findNewMessages(JSONArray oldList, JSONArray newList) throws JSONException{
		if(newList == null || newList.length() == 0){
			return new JSONArray();
		}
		if(oldList == null || oldList.length() == 0){
			return newList;
		}
		//need to find oldList[0] in newList
		JSONArray onlyNew = new JSONArray();
		for(int i = 0; i < newList.length(); i++){
			//we believe in VK API that every message should have its unique id...
			JSONObject messageInNew = newList.getJSONObject(i);
			if(messageInNew.getInt("id") == oldList.getJSONObject(0).getInt("id")){
				break;
			} else {
				onlyNew.put(messageInNew);
			}
		}
		return onlyNew;
	}
	
	/**
	 * Returns {@code true} if there is any received message in the list, otherwise returns {@code false} 
	 * @param messages The list of messages to be check
	 * @return {@code boolean}
	 * @throws JSONException when wrong messages
	 */
	private boolean isThereRecieved(JSONArray messages) throws JSONException{
		JSONObject message = null;
		for (int i = 0; i < messages.length(); i++) {
			message = messages.getJSONObject(i);
			if(message.getInt("out") == 0){
				return true;
			}
		}
		return false;
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		VKUIHelper.onDestroy(this);
	}
	
	//activity should implement 
	/// NavigationDrawerFragment.NavigationDrawerCallbacks class
	@Override
    public void onNavigationDrawerItemSelected(JSONObject user) {
		String incomingUserName = null;
		int incomingUserId = -1;
		try {
			incomingUserName = user.getString("first_name") + 
					" " + user.getString("last_name");
			incomingUserId = user.getInt("id");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		if(incomingUserId != activeUserId){
			initForUser(user);
			activeUserName = incomingUserName;
			activeUserId = incomingUserId;
			setTitle(activeUserName);
			//first time call with more messages
			recieveMessageHistory(50); 
			//then as written in recieveMessagePeriodicly
			recieveMessagePeriodicly();
		}
    }
    
	private void initForUser(JSONObject user){
		LinearLayout historyLayout = (LinearLayout)findViewById(R.id.messageHistory);
		historyLayout.removeAllViews();
		LinearLayout formLayout = (LinearLayout)findViewById(R.id.linearLayout1);
		formLayout.removeAllViews();
		
		messageToSend.clear();
		
		//method is called first time
		if(activeUserName == null){
			View view = findViewById(R.id.container);
			view.setVisibility(View.VISIBLE);
			
			View helloView = findViewById(R.id.helloView);
			helloView.setVisibility(View.GONE);
		} else{
			handler.removeCallbacks(recieveMessagesRunnable);
		}
	}
	
	/**
	 * For square images
	 * @param image
	 * @param size
	 */
	private void setImageViewSize(ImageView image, int size, int margin){
		LinearLayout.LayoutParams par = new LinearLayout.LayoutParams(size, size);
		par.setMargins(margin, margin, margin, margin);
		//TODO: not affect in bottom margin
		image.setLayoutParams(par);
	}
	
	@Override
	public OnClickListener getOnPictogramClickListener() {
		return onPictogramClickListener;
	}
	
	private void initOnPictogramClickListener() {
		onPictogramClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				
				final ImageView image = new ImageView(MessageActivity.this);
				//translated to pixels
				int size = (int)getResources().getDimension(R.dimen.new_message_height)-5;
				setImageViewSize(image, size, 2);
				
				Pictogram pic = ((GridViewHolder)view.getTag()).getPictogram(); //was set in adapter
				String picRuName  = NameManager.getInstance().getFileRuName(pic.getPath());
				addImageNameToSendMessages(picRuName);
				
				final LinearLayout piktogramsLayout = (LinearLayout) findViewById(R.id.linearLayout1);
				ImageLoader imageLoader = ImageLoader.getInstance();
				imageLoader.displayImage(pic.getPathWithAssests(), image, new SimpleImageLoadingListener() {
					@Override
					public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
						piktogramsLayout.addView(image);
						HorizontalScrollView hView = (HorizontalScrollView)findViewById(R.id.new_mess_scroll);
						scrollRight(hView);
					}
				});
				
			}
		};
	}

	private class ManagersInitializer extends AsyncTask<Void, String, Void>{

		@Override
		protected Void doInBackground(Void... params) {
			//init NameManager
			XmlPullParser parser = getResources().getXml(R.xml.catalog);
			NameManager.getInstance().init(parser);
			//init PictogramManager
			PictogramManager.getInstance().init(getAssets());
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			PictogramManager pManager = PictogramManager.getInstance();
			NameManager nManager = NameManager.getInstance();
			GridView firstLevelGorups = (GridView)findViewById(R.id.firstLevelGroups_view);
			updateFirstLevelGroupAdapter(firstLevelGorups, pManager.getFirstLevelGroups());
		}
	}

	private void updateFirstLevelGroupAdapter(final GridView groupView, List<? extends Pictogram> pictograms){
		if(firstLevelGroupAdapter == null){
			firstLevelGroupAdapter = new GroupAdapter(MessageActivity.this, pictograms);
			groupView.setAdapter(firstLevelGroupAdapter);
			groupView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view,int position, long id) {
					GroupPictogram gp = (GroupPictogram)groupView.getItemAtPosition(position);
					updateAdapters(gp.getInnerPictograms());
				}
			});
			//clicks first element
			groupView.performItemClick(groupView.getAdapter().getView(0, null, null), 
					0, groupView.getAdapter().getItemId(0));
		} else {
			firstLevelGroupAdapter.updateView(pictograms);
			
		}
	}

	private void updateAdapters(List<Pictogram> pictograms) {
		View subgroupsContainer = findViewById(R.id.subGroups_container);
		if(pictograms.size() == 0){
			updateSubGroupAdapter(pictograms);
			updatedateGridViewAdapter(pictograms);
		} else {
			//assume all other should have the same type
			if(pictograms.get(0).getType() == ElementType.FILE){
				subgroupsContainer.setVisibility(View.GONE);
				updateSubGroupAdapter(new ArrayList<Pictogram>());
				updatedateGridViewAdapter(pictograms);
			} else if(pictograms.get(0).getType() == ElementType.GROUP){
				subgroupsContainer.setVisibility(View.VISIBLE);
				updateSubGroupAdapter(pictograms);
				subGroupsView.setItemChecked(0, true);
				updatedateGridViewAdapter(((GroupPictogram)pictograms.get(0)).getInnerPictograms());
			}
		}
	}
	
	private void updateSubGroupAdapter(List<Pictogram> pictograms){
		if(subGroupAdapter == null){
			subGroupAdapter = new GroupAdapter(MessageActivity.this, pictograms, true);
			subGroupsView.setAdapter(subGroupAdapter);
			subGroupsView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view,int position, long id) {
					GroupPictogram gp = (GroupPictogram)subGroupsView.getItemAtPosition(position);
					updatedateGridViewAdapter(gp.getInnerPictograms());
				}
			});
		} else {
			subGroupAdapter.updateView(pictograms);
		}
	}

	private void updatedateGridViewAdapter(List<Pictogram> pictograms) {
		if (gridViewAdapter == null) {
			gridViewAdapter = new GridViewAdapter(MessageActivity.this, MessageActivity.this, pictograms);
			pictogramsGridView.setAdapter(gridViewAdapter);
		} else {
			gridViewAdapter.updateView(pictograms);
		}
		pictogramsGridView.post(new Runnable() {
			@Override
			public void run() {
				pictogramsGridView.setSelection(0);
			}
		});
	}

	private void initImageLoader() {
		//Get the imageloader.
		ImageLoader imageLoader = ImageLoader.getInstance();

		//Create image options.
		DisplayImageOptions options = new DisplayImageOptions.Builder()
				.cacheInMemory(true)
				.imageScaleType(ImageScaleType.EXACTLY) //Only need for group buttons, need to be changed
				.bitmapConfig(Bitmap.Config.ALPHA_8) //because our images are black/white
				.build();

		//Create a config with those options.
		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getApplicationContext())
				.defaultDisplayImageOptions(options)
				.build();

		//Initialize the imageloader.
		imageLoader.init(config);
	}
	
}
