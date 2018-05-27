package com.spb.sezam;

import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKScope;
import com.vk.sdk.VKSdk;
import com.vk.sdk.VKSdkListener;
import com.vk.sdk.VKUIHelper;
import com.vk.sdk.api.VKError;
import com.vk.sdk.dialogs.VKCaptchaDialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;

public class BaseActivity extends ActionBarActivity {

	private static final String[] myScope = new String[] {
        VKScope.FRIENDS,
        VKScope.MESSAGES,
        VKScope.OFFLINE
	 };
	
	private final VKSdkListener sdkListener = new VKSdkListener() {
		@Override
		public void onCaptchaError(VKError captchaError) {
			new VKCaptchaDialog(captchaError).show();
		}

		@Override
		public void onTokenExpired(VKAccessToken expiredToken) {
			VKSdk.authorize(myScope);
		}

		@Override
		public void onAccessDenied(final VKError authorizationError) {
			new AlertDialog.Builder(VKUIHelper.getTopActivity()).setMessage(
					authorizationError.toString()).show();
		}

		@Override
		public void onReceiveNewToken(VKAccessToken newToken) {
			Log.e("Entering the app", "Entering the app! New token: "
					+ newToken.userId);
			startActivity(MessageActivity.class);
		}

		@Override
		public void onAcceptUserToken(VKAccessToken token) {
			Log.e("onAcceptUserToken", "onAcceptUserToken don't know when");
			startActivity(MessageActivity.class);
		}

		@Override
		public void onRenewAccessToken(VKAccessToken token) {
			Log.e("onRenewAccessToken", "onRenewAccessToken don't know when");
		}
	};
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		VKUIHelper.onActivityResult(this, requestCode, resultCode, data);
	}

	void startActivity(Class<? extends Activity> a) {
		Intent startNewActivityOpen = new Intent(this, a);
		startActivityForResult(startNewActivityOpen, 0);
	}

	void initVKSdk() {
		VKSdk.initialize(sdkListener, "4619202");
	}
	
	static void authorize(){
		VKSdk.authorize(myScope);
	}
}
