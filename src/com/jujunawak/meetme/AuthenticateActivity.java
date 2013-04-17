package com.jujunawak.meetme;

import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.json.JSONArray;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

public class AuthenticateActivity extends Activity {

	private String CALLBACKURL = "meetme://callback";
	private String consumerKey = "0psdnPSbme1wgGWJBE9Bug";
	private String consumerSecret = "0Q0FnooyDOJA0GH9DzoFOOvX1XzlyQjj8lHh2pmyI8";

	private OAuthProvider httpOauthprovider = new DefaultOAuthProvider("https://api.twitter.com/oauth/request_token", "https://api.twitter.com/oauth/access_token", "https://api.twitter.com/oauth/authorize");
	private CommonsHttpOAuthConsumer httpOauthConsumer = new CommonsHttpOAuthConsumer(consumerKey, consumerSecret);


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.v("authActivity", "onCreate");
	
//		Button oauth = (Button)findViewById(R.id.button1);
//		oauth.setOnClickListener(new View.OnClickListener() {
//			public void onClick(View v) {
//				try {
//					String authUrl = httpOauthprovider.retrieveRequestToken(httpOauthConsumer, CALLBACKURL);
//					Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl));
//					v.getContext().startActivity(intent);
//				} catch (Exception e) {
//					Log.w("oauth fail", e);
//					Toast.makeText(v.getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
//				}
//			}
//		});

	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		Log.w("redirect-to-app", "going to save the key and secret");

		Uri uri = intent.getData();
		if (uri != null && uri.toString().startsWith(CALLBACKURL)) {

			String verifier = uri.getQueryParameter(oauth.signpost.OAuth.OAUTH_VERIFIER);

			try {
				// this will populate token and token_secret in consumer

				httpOauthprovider.retrieveAccessToken(httpOauthConsumer, verifier);
				String userKey = httpOauthConsumer.getToken();
				String userSecret = httpOauthConsumer.getTokenSecret();

				// Save user_key and user_secret in user preferences and return
				SharedPreferences settings = getBaseContext().getSharedPreferences("your_app_prefs", 0);
				SharedPreferences.Editor editor = settings.edit();
				editor.putString("user_key", userKey);
				editor.putString("user_secret", userSecret);
				editor.commit();


				/* TODO MOVE */
				HttpGet get = new HttpGet("http://api.twitter.com/1/statuses/home_timeline.json");
				HttpParams params = new BasicHttpParams();
				HttpProtocolParams.setUseExpectContinue(params, false);
				get.setParams(params);
				 
				try {
				   settings = getBaseContext().getSharedPreferences("your_app_prefs", 0);
				   userKey = settings.getString("user_key", "");
				   userSecret = settings.getString("user_secret", "");
				 
				  httpOauthConsumer.setTokenWithSecret(userKey, userSecret);
				  httpOauthConsumer.sign(get);
				 
				  DefaultHttpClient client = new DefaultHttpClient();
				  String response = client.execute(get, new BasicResponseHandler());
				  JSONArray array = new JSONArray(response);
				  for(int i=0 ; i< array.length() ; i++){
					  Log.v("JSON", array.get(i).toString());
				  }
				} catch (Exception e) { 
				  // handle this somehow
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			// Do something if the callback comes from elsewhere
			Log.w("Meet Me Auth", "not a callback intent");
		}
	}
}
