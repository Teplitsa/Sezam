package com.spb.sezam;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.spb.sezam.adapters.UsersAdapter;
import com.spb.sezam.utils.ErrorUtil;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.VKRequest.VKRequestListener;

import android.support.v7.app.ActionBarActivity;
import android.app.Activity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

/**
 * Fragment used for managing interactions for and presentation of a navigation drawer.
 * See the <a href="https://developer.android.com/design/patterns/navigation-drawer.html#Interaction">
 * design guidelines</a> for a complete explanation of the behaviors implemented here.
 */
public class NavigationDrawerFragment extends Fragment {

    /**
     * Remember the position of the selected item.
     */
    private static final String STATE_SELECTED_POSITION = "selected_navigation_drawer_position";

    /**
     * Per the design guidelines, you should show the drawer on launch until the user manually
     * expands it. This shared preference tracks this.
     */
    private static final String PREF_USER_LEARNED_DRAWER = "navigation_drawer_learned";

    /**
     * A pointer to the current callbacks instance (the Activity).
     */
    private NavigationDrawerCallbacks mCallbacks;

    /**
     * Helper component that ties the action bar to the navigation drawer.
     */
    private ActionBarDrawerToggle mDrawerToggle;

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerListView;
    private View mFragmentContainerView;

    private int mCurrentSelectedPosition = -1;
    private boolean mUserLearnedDrawer;
    
    private List<JSONObject> users = new ArrayList<>();
    private UsersAdapter usersAdapter = null;
    private JSONObject testUser = null;
    private Map<String, String> usersUnreadMsCount = new HashMap<>();
	private int unReadDialogsCount = 0;
    
    private Runnable usersInfoRunnable = null;
    private Runnable checkUnreadMessagesRunnable = null;
	private final Handler handler = new Handler();
    
    private Menu menu;
    
    private static Logger logger = LoggerFactory.getLogger(NavigationDrawerFragment.class);
    
    //-----------------------VK listeners-----------------------------//
    private VKRequestListener loadFriendsListener = new VKRequestListener() {

		@Override
		public void onComplete(VKResponse response) {
			try {
				updateUsers(response.json.getJSONObject("response").getJSONArray("items"));
				//we have adapter with users already
				//and in this line we have new users
				updateUnreadeMessagesCounts();
				//usersAdapter.notifyDataSetChanged();
			} catch (JSONException e) {
				e.printStackTrace();
				ErrorUtil.showError(getActivity(), "Error on Friends load");
			}
		}

		@Override
		public void onError(VKError error) {
			Log.e("Friends load", "Error on Friends load");
			ErrorUtil.showError(getActivity(), error);
		}
	};
	
	private VKRequestListener recieveDialogsListener = new VKRequestListener() {
		
		@Override
		public void onComplete(VKResponse response) {
			try {
				JSONArray dialogs = response.json.getJSONObject("response").getJSONArray("items");
				JSONArray messages = new JSONArray();
				
				for(int i=0; i<dialogs.length(); i++){
					JSONObject dialog = (JSONObject)dialogs.get(i);
					//exclude chats
					try{
						dialog.getJSONObject("message").getInt("chat_id");
					} catch (JSONException e) {
						//if not in chat
						if(isInUsersList(dialog.getJSONObject("message").getInt("user_id"))){
							//and message is sent by friend(user)
							messages.put(dialog);
						}
					}
				}
				unReadDialogsCount = messages.length();
				
				JSONObject dialogInfo = null;
				JSONObject message = null;
				String unreadCount = null;
				String userId = null;
				usersUnreadMsCount.clear();
				for (int i = 0; i < unReadDialogsCount; i++) {
					dialogInfo = messages.getJSONObject(i);
					unreadCount =  dialogInfo.getString("unread");
					message = dialogInfo.getJSONObject("message");
					userId = message.getString("user_id");
					usersUnreadMsCount.put(userId, unreadCount);
				}
				updateUnreadeMessagesCounts();
				switch (messages.length()) {
				case 0:
					menu.getItem(0).setIcon(R.drawable.count_0);
					break;
				case 1:
					menu.getItem(0).setIcon(R.drawable.count_1);
					break;
				case 2:
					menu.getItem(0).setIcon(R.drawable.count_2);
					break;
				case 3:
					menu.getItem(0).setIcon(R.drawable.count_3);
					break;
				default:
					menu.getItem(0).setIcon(R.drawable.count_many);
					break;
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public void onError(VKError error) {
			Log.e("Recieve dialogs", "Error on recieve Dialogs");
			ErrorUtil.showError(getActivity(), error);
		}
	};
	//-------------------------------------------------------//

	private boolean isInUsersList(int userId) throws JSONException{
		for(JSONObject user : users){
			if(user.getInt("id") == userId){
				return true;
			}
		}
		return false;
	}
	
    public NavigationDrawerFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        logger.debug("Creating NavigationDrawer");
        
        // Read in the flag indicating whether or not the user has demonstrated awareness of the
        // drawer. See PREF_USER_LEARNED_DRAWER for details.
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mUserLearnedDrawer = sp.getBoolean(PREF_USER_LEARNED_DRAWER, false);
    }
    
    private void receiveUsersInfoPeriodicaly(){
    	usersInfoRunnable = new Runnable() {
			@Override
			public void run() {
				VKRequest request = VKApi.friends().get(VKParameters.from("order", "name", VKApiConst.FIELDS, "id,first_name,last_name,sex,bdate"));
				request.setPreferredLang("ru");
				request.executeWithListener(loadFriendsListener); 
				handler.postDelayed(this, 1000*60*5); //every 5 minutes
				//adapter notify in listener
			}
		};
		handler.post(usersInfoRunnable);
    }
    
    /**
     * Changes users form JSON to list, adds test user in first position, and 
     * adds '0' as 'unread_count' for each user
     * @param usersJson Initial users
     * @throws JSONException for wrong users
     */
    private void updateUsers(JSONArray usersJson) throws JSONException {
    	//in good way we need to run all over the list and find
    	//if there is a different user, and change its values (especially unread_count)
    	users.clear();
    	
    	int count = usersJson.length();
    	addTestUser(users);
		for(int i = 1; i <= count; i++){
			users.add(usersJson.getJSONObject(i-1).put("unread_count", "0"));
		}
	}
    
    private void addTestUser(List<JSONObject> users) throws JSONException {
    	if(testUser == null){
			//create Sezam Bot for test messages
			testUser = new JSONObject();
			testUser.put("last_name", "");
			testUser.put("first_name", "КОММУНИКАТОР");
			testUser.put("id", "287378130");
			testUser.put("online", "0");
			testUser.put("unread_count", "0");
    	}
    	users.add(testUser);
    }
    
    private void updateUnreadeMessagesCounts() throws JSONException{
    	String unreadCount = null;
    	for(JSONObject user :users){
    		unreadCount = usersUnreadMsCount.get(user.getString("id"));
    		if(unreadCount != null){
    			user.put("unread_count", unreadCount);
    		} else {
    			user.put("unread_count", "0");
    		}
    	}
    	usersAdapter.notifyDataSetChanged();
    }
    
    private void checkUnreadeMessages(){
		VKRequest request = new VKRequest("messages.getDialogs", VKParameters.from("unread", "1", "preview_length", "20"));
		request.executeWithListener(recieveDialogsListener);
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
		Date date = new Date();
		Log.w("Time", "Dialogs recive at " + sdf.format(date));
	}
    
    private void checkUnreadeMessagesPeriodicly() {
    	//let it as was before (not like reciveMessage) cause need interval (800) 
    	//and be really periodically , even if app is in background
		checkUnreadMessagesRunnable = new Runnable() {
			public void run() {
				checkUnreadeMessages();
				handler.postDelayed(this, 5000);
			}
		};
		
		unReadDialogsCount = 0;
		handler.postDelayed(checkUnreadMessagesRunnable, 800);
	}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mDrawerListView = (ListView) inflater.inflate(
        		R.layout.fragment_navigation_drawer, container, false);
        mDrawerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectItem(position);
            }
        }); 
        mDrawerListView.setActivated(false);
        
        if (savedInstanceState != null) {
            mCurrentSelectedPosition = savedInstanceState.getInt(STATE_SELECTED_POSITION);
            ArrayList<String> stringUsers = savedInstanceState.getStringArrayList("users");
            users = stringListToJsonList(stringUsers);
        }
        setAdapterForListView(users);
        return mDrawerListView;
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Indicate that this fragment would like to influence the set of actions in the action bar.
        setHasOptionsMenu(true);
        
        // Select either the default item (no item) or the last selected item.
        if(mCurrentSelectedPosition >= 0){
        	logger.debug("mDrawerListView is " + mDrawerListView);
        	logger.debug("position="+mCurrentSelectedPosition);
        	selectItem(mCurrentSelectedPosition);
        } else {
        	//selectItem has checkUnreadeMessagesPeriodicly() in it
        	checkUnreadeMessagesPeriodicly();
        }
        receiveUsersInfoPeriodicaly();
    }
    
    private void setAdapterForListView(List<JSONObject> friendsArr){
    	usersAdapter = new UsersAdapter(getActionBar().getThemedContext(), friendsArr);
    	mDrawerListView.setAdapter(usersAdapter);
        //mDrawerListView.setItemChecked(mCurrentSelectedPosition, true);
    }

    private boolean isDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mFragmentContainerView);
    }
    
    public void closeDrawer(){
    	if(isDrawerOpen()){
    		mDrawerLayout.closeDrawer(mFragmentContainerView);
    	}
    }
    
    private void openDrawer(){
    	if(!isDrawerOpen()){
    		mDrawerLayout.openDrawer(mFragmentContainerView);
    	}
    }

    /**
     * Users of this fragment must call this method to set up the navigation drawer interactions.
     *
     * @param fragmentId   The android:id of this fragment in its activity's layout.
     * @param drawerLayout The DrawerLayout containing this fragment's UI.
     */
    public void setUp(int fragmentId, DrawerLayout drawerLayout) {
        mFragmentContainerView = getActivity().findViewById(fragmentId);

        mDrawerLayout = drawerLayout;Bar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the navigation drawer and the action bar app icon.
        mDrawerToggle = new ActionBarDrawerToggle(
                getActivity(),                    /* host Activity */
                mDrawerLayout,                    /* DrawerLayout object */
                R.drawable.ic_drawer,             /* nav drawer image to replace 'Up' caret */
                R.string.navigation_drawer_open,  /* "open drawer" description for accessibility */
                R.string.navigation_drawer_close  /* "close drawer" description for accessibility */
        ) {

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                if (!isAdded()) {
                    return;
                }

                if (!mUserLearnedDrawer) {
                    // The user manually opened the drawer; store this flag to prevent auto-showing
                    // the navigation drawer automatically in the future.
                    mUserLearnedDrawer = true;
                    SharedPreferences sp = PreferenceManager
                            .getDefaultSharedPreferences(getActivity());
                    sp.edit().putBoolean(PREF_USER_LEARNED_DRAWER, true).apply();
                }
            }
        };

        // Defer code dependent on restoration of previous instance state.
        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });

        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    private void selectItem(int position) {
    	
        mCurrentSelectedPosition = position;
        JSONObject user = null;
        if (mDrawerListView != null) {
            mDrawerListView.setItemChecked(position, true);
            user = (JSONObject)mDrawerListView.getItemAtPosition(position);
        }
        
        
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(mFragmentContainerView);
        }
        if (mCallbacks != null) {
        	logger.debug("user " + user);
            mCallbacks.onNavigationDrawerItemSelected(user);
            handler.removeCallbacks(checkUnreadMessagesRunnable);
            checkUnreadeMessagesPeriodicly();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallbacks = (NavigationDrawerCallbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement NavigationDrawerCallbacks.");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }
    
    @Override
    public void onDestroy(){
    	super.onDestroy();
    	handler.removeCallbacks(usersInfoRunnable);
    	handler.removeCallbacks(checkUnreadMessagesRunnable);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_POSITION, mCurrentSelectedPosition);
        outState.putStringArrayList("users", jsonListToStringList(users));
    }
    
    private ArrayList<String> jsonListToStringList(List<JSONObject> jsonList){
    	ArrayList<String> stringList = new ArrayList<>();
    	for(JSONObject json : jsonList){
    		stringList.add(json.toString());
    	}
    	return stringList;
    }
    
    private ArrayList<JSONObject> stringListToJsonList(List<String> stringList){
    	ArrayList<JSONObject> jsonList = new ArrayList<>();
    	for(String str : stringList){
    		try {
				jsonList.add(new JSONObject(str));
			} catch (JSONException e) {
				e.printStackTrace();
			}
    	}
    	return jsonList;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Forward the new configuration the drawer toggle component.
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

   @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
	   this.menu = menu;
        super.onCreateOptionsMenu(menu, inflater);
    }

	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        } else if(item.getItemId() == R.id.action_message ){
        	if(unReadDialogsCount > 0){
        		openDrawer();
        	}
			return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Per the navigation drawer design guidelines, updates the action bar to show the global app
     * 'context', rather than just what's in the current screen.
     */
    private void showGlobalContextActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setTitle(R.string.app_name);
    }

    private ActionBar getActionBar() {
        return ((ActionBarActivity) getActivity()).getSupportActionBar();
    }

    /**
     * Callbacks interface that all activities using this fragment must implement.
     */
    public static interface NavigationDrawerCallbacks {
        /**
         * Called when an item in the navigation drawer is selected.
         */
        void onNavigationDrawerItemSelected(JSONObject user);
    }
}
