package com.spb.sezam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vk.sdk.VKSdk;
import com.vk.sdk.VKUIHelper;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class VKActivity extends BaseActivity {
	 
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vk);
        
        Logger log = LoggerFactory.getLogger(VKActivity.class); 
        log.debug("Oncreate VKActivity");
        
        VKUIHelper.onCreate(this);
        //initialize Vk SDK
        initVKSdk();
        
        Button b = (Button)findViewById(R.id.sign_in_button);
        //predefined in .xml as �����
        if (VKSdk.wakeUpSession()) {
        	Log.e("wakeUp","wakeUp");
            startActivity(MessageActivity.class);
            //
            return;
        }
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            	if (VKSdk.isLoggedIn()){
            		Log.e("logged in"," Loged in");
            	}
            	authorize();
            }
        });
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
		if (VKSdk.isLoggedIn()){
			Log.e("Uje logged in","Uje Loged in destroyi vaxt");
        } 
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		VKUIHelper.onActivityResult(this, requestCode, resultCode, data);
	}
}



