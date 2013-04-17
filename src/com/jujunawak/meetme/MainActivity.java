package com.jujunawak.meetme;

import java.net.UnknownHostException;

import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import twitter4j.IDs;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.drawable.Drawable;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.OverlayItem;
import com.mongodb.DBObject;
import com.mongodb.MongoException;

public class MainActivity extends MapActivity  implements GpsStatus.Listener {

	private static final String CALLBACK_URL = "meetme://callback";

	private MapController mapController;

	private MapView map;

	private LocationManager locationManager;

	private boolean first = true;

	//	private static double lat = 35.952967;   // Temporary test values for lat/long
	//	private static double lon = -83.929158 ;
	//	private int latE6;
	//	private int lonE6;
	private long minTime = 15000;
	private float minDistance = 10;




	private MyOverlays itemizedoverlay;
	private MyLocationOverlay myLocationOverlay;

	private Drawable drawable;

	private GeoUpdateHandler geoHandler;


	/* Twitter connection management */
	private static CommonsHttpOAuthConsumer consumer;
	private static DefaultOAuthProvider provider;
	private static Twitter twitter;
	private static String  CONSUMER_KEY="0psdnPSbme1wgGWJBE9Bug";
	private static String CONSUMER_SECRET="0Q0FnooyDOJA0GH9DzoFOOvX1XzlyQjj8lHh2pmyI8";
	//	private OAuthProvider httpOauthprovider = new DefaultOAuthProvider("https://api.twitter.com/oauth/request_token", "https://api.twitter.com/oauth/access_token", "https://api.twitter.com/oauth/authorize");
	//	private CommonsHttpOAuthConsumer httpOauthConsumer = new CommonsHttpOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);



	/* MongoDB Connection */
	private MongoConnection monConnection;





	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.setProperty("http.keepAlive", "false"); 
		super.onCreate(savedInstanceState);

		setContentView(R.layout.mainlayout);

		/** Connection **/

		manageConnection();
		
		manageUnconnection();


		/** MAP **/
		map = (MapView) findViewById(R.id.mapview1);
		mapController = map.getController();


		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		locationManager.addGpsStatusListener(this);


		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime,
				minDistance, getGeoHandler());
		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTime,
				minDistance, getGeoHandler());


		myLocationOverlay = new MyLocationOverlay(this, map);
		map.getOverlays().add(myLocationOverlay);

		myLocationOverlay.runOnFirstFix(new Runnable() {
			public void run() {
				map.getController().animateTo(
						myLocationOverlay.getMyLocation());
			}
		});

		getDrawable(); 
		getItemizedoverlay();
		createMarker(0f, map.getMapCenter());

		/* MOngoConnection */
		getMongoConnection();

		/* Locate Me  */
		Button locateButton = (Button)findViewById(R.id.buttonLocateMe);
		locateButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				Location loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
				getGeoHandler().onLocationChanged(loc);

			}
		});


		/* Followers */
		final Button followersButton = (Button)findViewById(R.id.buttonFollowers);
		followersButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				try {

					followersButton.setEnabled(false);
					Toast.makeText(getContext(), "Look for followers ...", Toast.LENGTH_SHORT).show();

					IDs ids = getTwitter().getFollowersIDs(-1);
					Log.v("Followers size ",""+ids.getIDs().length);
					Toast.makeText(getContext(), ""+ids.getIDs().length, Toast.LENGTH_SHORT).show();


				} catch (IllegalStateException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Toast.makeText(getContext(), "Not yet connected to Twitter", Toast.LENGTH_LONG).show();
				} catch (TwitterException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Toast.makeText(getContext(), "Not yet connected to Twitter", Toast.LENGTH_LONG).show();
				}finally{
					followersButton.setEnabled(true);
				}

			}
		});
	}




	private void manageUnconnection() {
		
		Button disconnect = (Button)findViewById(R.id.unconnection);
		disconnect.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Toast.makeText(getContext(), "disconnecting", Toast.LENGTH_SHORT).show();
				twitter = null;
				getTwitter();
				SharedPreferences prefs = getContext().getSharedPreferences("your_app_prefs", 0);
				Editor editor = prefs.edit();
				editor.remove("user_key");
				editor.remove("user_secret");
				editor.commit();
				
				Button connect = (Button)findViewById(R.id.button1);
				connect.setEnabled(true);
				connect.setVisibility(View.VISIBLE);
				connect.setClickable(true);
				
			}
		});
		
		
	}




	@Override
	public void onResume() {

		super.onResume();
		Log.v("Meet ME RESUME", "RESUME !!!");
		Log.v("Meet ME RESUME intent", ""+this.getIntent());
		Log.v("Meet ME RESUME twitter", ""+twitter);
		
		if(checkForSavedLogin()) return;
		
		try {  
		
			if (this.getIntent()!=null && this.getIntent().getData()!=null ){//&& first==true){  
				Log.v("Intent", this.getIntent()+"");
				Uri uri = this.getIntent().getData();  
				Log.v("Meet ME RESUME", "OK "+uri);
				if (uri != null && uri.toString().startsWith(CALLBACK_URL)) {  
					String verifier = uri.getQueryParameter(oauth.signpost.OAuth.OAUTH_VERIFIER);
					Log.v("Verifier : ", ""+verifier);

					// this will populate token and token_secret in consumer  
					Log.v("consumer is null ?", (consumer == null)+"");
					Log.v("Resume Consumer Token", ""+getTwitterConsumer().getToken());
					Log.v("Resume Consumer Secret", ""+getTwitterConsumer().getTokenSecret());



					getTwitterProvider().retrieveAccessToken(getTwitterConsumer(), verifier);  

					// Get Access Token and persist it  
					AccessToken a = new AccessToken(getTwitterConsumer().getToken(), getTwitterConsumer().getTokenSecret());  

					getTwitter().setOAuthAccessToken(a);  
					
					Log.v("ON RESUME TWITTER ID :", getTwitter().getId()+"");
					MongoConnection.setTwitterId(getTwitter().getId());
					SharedPreferences prefs = getContext().getSharedPreferences("your_app_prefs", 0);
					Editor editor = prefs.edit();
					editor.putString("user_key", a.getToken());
					editor.putString("user_secret", a.getTokenSecret());
					editor.commit();

					
					Button conn = (Button)findViewById(R.id.button1);
					conn.setClickable(false);
					conn.setEnabled(false);
					conn.setVisibility(View.INVISIBLE);
					startFirstActivity();
				}
			}

		} catch (Exception e) {  
			//Log.e(APP, e.getMessage());  
			e.printStackTrace();  

		}  


	}



	private LocationListener getGeoHandler() {
		if(geoHandler == null){
			geoHandler = new GeoUpdateHandler();
		}
		return geoHandler;
	}

	public class GeoUpdateHandler implements LocationListener {

		@Override
		public void onLocationChanged(Location location) {

			int lat = (int) (location.getLatitude() * 1E6); 
			int lng = (int) (location.getLongitude() * 1E6);
			Log.i("MeetMe Update", lat+" "+lng);
			GeoPoint point = new GeoPoint(lat, lng);

			mapController.animateTo(point); // mapController.setCenter(point);

			mapController.setZoom(17);


			createMarker(location.getSpeed(), point);

		}

		@Override
		public void onProviderDisabled(String provider) {
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}
	}

	private void createMarker(float f, GeoPoint p) {
		//GeoPoint p = map.getMapCenter();
		OverlayItem overlayitem = new OverlayItem(p, "Speed : "+f, "s");
		itemizedoverlay.addOverlay(overlayitem);
		if (itemizedoverlay != null) {
			map.getOverlays().add(itemizedoverlay);
		}
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	/* Connection */

	private void manageConnection() {

		if(checkForSavedLogin()) return;

		final Button connetionButton = (Button)findViewById(R.id.button1);
		connetionButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				
				LayoutInflater factory = LayoutInflater.from(getContext());
				final View view = factory.inflate(R.layout.connectionview, null);




				Builder dial = new AlertDialog.Builder(getContext());
				dial.setView(view);

				dial.setPositiveButton("OK", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {

						try {  
							getTwitterConsumer();
							getTwitterProvider();

							Log.v("before Consumer Token", ""+getTwitterConsumer().getToken());
							Log.v("before Consumer Secret", ""+getTwitterConsumer().getTokenSecret());
							String authUrl = getTwitterProvider().retrieveRequestToken(getTwitterConsumer(), CALLBACK_URL);
							Log.v("after Consumer Token", ""+getTwitterConsumer().getToken());
							Log.v("after Consumer Secret", ""+getTwitterConsumer().getTokenSecret());
							Toast.makeText(getContext(), "Please authorize this app!", Toast.LENGTH_LONG).show();  

							startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)));
							Toast.makeText(getContext(), "Connected", Toast.LENGTH_SHORT).show();							

						} catch (Exception e) {  
							Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();  
						}  
					}

				});

				dial.show();
			}
		});
	}



	private CommonsHttpOAuthConsumer getTwitterConsumer() {
		if(consumer == null){
			consumer = new CommonsHttpOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);
		}
		return consumer;
	}


	private DefaultOAuthProvider getTwitterProvider() {
		if(provider == null){
			provider = new DefaultOAuthProvider("https://api.twitter.com/oauth/request_token",
					"https://api.twitter.com/oauth/access_token",
					"https://api.twitter.com/oauth/authorize");  
		}
		return provider;
	}
	private boolean checkForSavedLogin() {  
		// Get Access Token and persist it  
		getTwitterConsumer();
		getTwitterProvider();
		getTwitter();
		AccessToken a = getAccessToken();  
		Log.v("MEET ME saved login", a+" ");
		if (a==null) return false; //if there are no credentials stored then return to usual activity  



		getTwitter().setOAuthAccessToken(a);  


		startFirstActivity();
		
		return true;

	}  


	private void startFirstActivity() {


		
		try {
			final long twitterId = getTwitter().getId();
			Log.v("TWITTER ID",twitterId+"");
			DBObject account = getMongoConnection().retrieveMeetMeAccount(twitterId);

			Log.v("MeetMe MongoDB", account.get("name")+" is retrieved "+ twitterId);

			/* If the user has not create his account yet.*/
			if( account.get("name").equals("notset")||account.get("age").equals("-1")){


				Intent formIntent = new Intent(getContext(), FormGenerationActivity.class);
				startActivity(formIntent);

			}

		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TwitterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}




	private MongoConnection getMongoConnection() {
		try {
			this.monConnection = MongoConnection.getInstance();	
		} catch (MongoException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return this.monConnection;
	}




	private AccessToken getAccessToken() {
		Log.v("Access Token stored :", "---");
		AccessToken res = null;

		SharedPreferences prefs = getContext().getSharedPreferences("your_app_prefs", 0);
		String userKey = prefs.getString("user_key", "");
		String userSecret = prefs.getString("user_secret", "");
		Log.v("Access Token stored :", userKey+" "+userSecret);
		if(!"".equals(userKey) && !"".equals(userSecret)){
			res = new AccessToken(userKey, userSecret);
		}

		return res;

	}





	public Twitter getTwitter(){
		if(twitter == null){
			Button but = (Button)findViewById(R.id.button1);
			but.setEnabled(true);
			twitter = new TwitterFactory().getInstance();
			twitter.setOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);
		}
		return twitter;
	}


	/* GPS Facilities */
	@Override
	public void onGpsStatusChanged(int event) {
		switch (event) 
		{
		case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
			break;
		case GpsStatus.GPS_EVENT_FIRST_FIX:   // this means you  found GPS Co-ordinates
			//			Log.i("MeetMe GPS", "GPS coordinates are ready ...");
			//			locationManager.removeUpdates(getGeoHandler());
			//			locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTime,
			//					minDistance, getGeoHandler());
			//			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime,
			//					minDistance, new GeoUpdateHandler());
			break;
		case GpsStatus.GPS_EVENT_STARTED:
			Log.i("MeetMe GPS", "GPS started");
			break;
		case GpsStatus.GPS_EVENT_STOPPED:
			//			Log.i("MeetMe GPS", "GPS stopped...");
			//			locationManager.removeUpdates(getGeoHandler());
			//			locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTime,
			//					minDistance, getGeoHandler());
			break;
		}

	}
	/* Getters */

	public MyOverlays getItemizedoverlay() {
		if(itemizedoverlay==null){
			itemizedoverlay = new MyOverlays(this, drawable);
		}
		return itemizedoverlay;
	}

	public Drawable getDrawable() {
		if(drawable == null){
			drawable = this.getResources().getDrawable(R.drawable.ic_launcher);
		}
		return drawable;
	}


	public Context getContext(){
		return this;
	}
}
