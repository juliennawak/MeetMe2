package com.jujunawak.meetme;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;

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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.OverlayItem;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoException;

public class MainActivity extends MapActivity  implements GpsStatus.Listener {

	private static final String CALLBACK_URL = "meetme://callback";

	private MapController mapController;

	private MapView map;

	private LocationManager locationManager;

	private static IDs ids = null;

	private long minTime = 15000;

	private float minDistance = 10;

	private static long lastUpdate = 0;


	private MyOverlays itemizedoverlay;
	private MyLocationOverlay myLocationOverlay;

	private Drawable drawable;

	private GeoUpdateHandler geoHandler;


	/* Twitter connection management */
	private static CommonsHttpOAuthConsumer consumer;
	private static DefaultOAuthProvider provider;
	private static Twitter twitter;




	private static String  CONSUMER_KEY;
	private static String CONSUMER_SECRET;




	/* MongoDB Connection */
	private MongoConnection monConnection;

	private static boolean savedLogin = false;





	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.setProperty("http.keepAlive", "false"); 
		super.onCreate(savedInstanceState);

		Properties props = new Properties();
		try {
			props.load(getContext().getAssets().open("key.properties"));
			CONSUMER_KEY = props.getProperty("consumer_key");
			CONSUMER_SECRET = props.getProperty("consumer_secret");
		} catch (Exception e1) {
			Log.v("meet me", "OnCreate failed (key.properties not found ... you have to find a valid pair/secret token from Twitter)... "+e1.getLocalizedMessage());

		} 


		setContentView(R.layout.mainlayout);

		/** Connection **/

		manageConnection();

		manageDisconnection();


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
		myLocationOverlay.enableCompass();
		myLocationOverlay.enableMyLocation();
		myLocationOverlay.runOnFirstFix(new Runnable() {
			public void run() {
				map.getController().animateTo(
						myLocationOverlay.getMyLocation());

			}
		});

		getDrawable(); 
		getItemizedoverlay().init();
		getItemizedoverlay().getOverlays();


		/* MongoConnection */
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
					Toast.makeText(getContext(), "Look for followers ...", Toast.LENGTH_LONG).show();
					Log.v("Followers ", "retrieve them ...");
					IDs ids = getIds();
					Log.v("Followers size ",""+ids.getIDs().length);
					Toast.makeText(getContext(), ""+ids.getIDs().length, Toast.LENGTH_SHORT).show();
					
					FollowersTask t = new FollowersTask();
					t.start();

				} catch (Exception e) {
					e.printStackTrace();
					Toast.makeText(getContext(), "Problem ..."+e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
				} finally{
					followersButton.setEnabled(true);
				}

			}

			private IDs getIds() {
				if(ids == null){
					try {
						ids = getTwitter().getFollowersIDs(-1);
					} catch (TwitterException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				return ids;
			}
		});
	}


	protected class FollowersTask extends Thread{
		@Override
		public void run() {
			super.run();
			try{
				
				DBCollection collUsers = getMongoConnection().getDb().getCollection("users");

				for(int i=0 ; i < ids.getIDs().length ; i++){
					DBCursor userC = collUsers.find(new BasicDBObject("twitterId", ids.getIDs()[i]));

					if(userC.hasNext()){
						DBObject user = userC.next();

						if(user.containsField("lat") && user.containsField("lon")){
							int lattitude = (int) (((Integer) user.get("lat")));
							int longitude = (int) (((Integer) user.get("lon")));
							GeoPoint point = new GeoPoint(lattitude, longitude);
							Log.v("Follower", ""+user.get("name"));

							String url = getTwitter().showUser(ids.getIDs()[i]).getProfileImageURL();

							Drawable marker = drawableFromUrl(url);

							Log.v("Follower", marker+"");
							createMarker((String) user.get("name"), point,  Long.valueOf((user.get("twitterId").toString())), marker);

						}

					}
				}
				Toast.makeText(getContext(), "Followers ok, see them now ...", Toast.LENGTH_LONG).show();
				
				
			}catch (Exception e) {
				e.printStackTrace();
			}

		}
	}

	public  Drawable drawableFromUrl(String url) throws IOException {
		Bitmap x;

		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
		connection.connect();
		InputStream input = connection.getInputStream();

		x = BitmapFactory.decodeStream(input);
		
		BitmapDrawable xx = new BitmapDrawable(x);

		//xx.setBounds(new Rect(-x.getWidth()/2, -x.getHeight()/2, x.getWidth()/2, x.getHeight()/2));
		xx.setBounds(0, 0, x.getWidth(), x.getHeight());
		//xx.setBounds(x.getWidth(), x.getHeight(), 0, 0);
		return xx;
	}


	private void manageDisconnection() {

		Button disconnect = (Button)findViewById(R.id.unconnection);
		disconnect.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Toast.makeText(getContext(), "disconnecting", Toast.LENGTH_SHORT).show();

				SharedPreferences prefs = getContext().getSharedPreferences("your_app_prefs", 0);
				Editor editor = prefs.edit();
				editor.remove("user_key");
				editor.remove("user_secret");
				editor.commit();

				twitter = null;
				getTwitter();

				savedLogin = false;
				getMongoConnection();
				MongoConnection.setTwitterId(-1);
				MongoConnection.setName("???");
				ids = null;


				enableConnectionButton(true);

				finish();

			}


		});


	}


	private void enableConnectionButton(boolean b) {
		Log.v("connection button :", b+"");
		Button connect = (Button)findViewById(R.id.button1);
		if(b){
			connect.setVisibility(View.VISIBLE);
		}else{
			connect.setVisibility(View.INVISIBLE);
		}
		connect.setClickable(b);
		connect.setEnabled(b);
	}

	@Override
	public void onResume() {

		super.onResume();

		Log.v("Meet ME RESUME intent", ""+this.getIntent());


		//if(checkForSavedLogin()) return;
		if(savedLogin) return;
		try {  

			if (this.getIntent()!=null && this.getIntent().getData()!=null ){//&& first==true){  

				Uri uri = this.getIntent().getData();  

				if (uri != null && uri.toString().startsWith(CALLBACK_URL)) {  
					String verifier = uri.getQueryParameter(oauth.signpost.OAuth.OAUTH_VERIFIER);
					Log.v("Verifier : ", ""+verifier);

					// this will populate token and token_secret in consumer  
					Log.v("consumer is null ?", (consumer == null)+"");

					getTwitterProvider().retrieveAccessToken(getTwitterConsumer(), verifier);  

					// Get Access Token and persist it  
					AccessToken a = new AccessToken(getTwitterConsumer().getToken(), getTwitterConsumer().getTokenSecret());  


					getTwitter().setOAuthAccessToken(a);  


					getItemizedoverlay().removeOverlay(-1);


					initUser();
					SharedPreferences prefs = getContext().getSharedPreferences("your_app_prefs", 0);
					Editor editor = prefs.edit();
					editor.putString("user_key", a.getToken());
					editor.putString("user_secret", a.getTokenSecret());
					editor.commit();


					setAvatar();

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


	private void initUser() {
		try {
			MongoConnection.setTwitterId(getTwitter().getId());
			MongoConnection.setName(getTwitter().showUser(MongoConnection.getTwitterId()).getName());
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TwitterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}




	public void setAvatar() {
		try{
			URL avatarURL= new URL(getTwitter().showUser(getTwitter().getId()).getBiggerProfileImageURL());
			ImageView image = (ImageView)findViewById(R.id.imageView1);
			Drawable avatar = Drawable.createFromStream(avatarURL.openStream(), "avatar");
			image.setImageDrawable(avatar);
		}catch (Exception e) {
			Log.w("avatar", "problem getting avatar "+e.getLocalizedMessage());
		}

	}



	private LocationListener getGeoHandler() {
		if(geoHandler == null){
			geoHandler = new GeoUpdateHandler();
		}
		return geoHandler;
	}



	private void createMarker(String title, GeoPoint p, long twitterId, Drawable marker) {
		//GeoPoint p = map.getMapCenter();

		OverlayItem overlayitem = new OverlayItem(p, title, "s");
		overlayitem.setMarker(marker);

		getItemizedoverlay().addOverlayItem(twitterId,overlayitem);
		if (getItemizedoverlay() != null) {
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



							String authUrl = getTwitterProvider().retrieveRequestToken(getTwitterConsumer(), CALLBACK_URL);


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
		if (a==null) {
			savedLogin = false;
			return false; //if there are no credentials stored then return to usual activity  
		}

		savedLogin = true;

		getTwitter().setOAuthAccessToken(a);  

		setAvatar();
		initUser();
		enableConnectionButton(false);
		startFirstActivity();

		return true;

	}  


	private void startFirstActivity() {



		try {
			final long twitterId = getTwitter().getId();

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
			if(this.monConnection.getDb() == null){
				AsyncTask<String, Void, Boolean> task = this.monConnection.execute("");
				if(!task.get()) throw new MongoException("Connection failed");
			}

		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			Log.e("Mongo Connection", "failed "+e1.getLocalizedMessage());
		}
		return this.monConnection;
	}




	private AccessToken getAccessToken() {

		AccessToken res = null;

		SharedPreferences prefs = getContext().getSharedPreferences("your_app_prefs", 0);
		String userKey = prefs.getString("user_key", "");
		String userSecret = prefs.getString("user_secret", "");

		if(!"".equals(userKey) && !"".equals(userSecret)){
			res = new AccessToken(userKey, userSecret);
		}

		return res;

	}





	public Twitter getTwitter(){
		if(twitter == null){
			enableConnectionButton(true);
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

	public class GeoUpdateHandler implements LocationListener {

		@Override
		public void onLocationChanged(Location location) {

			int lat = (int) (location.getLatitude() * 1E6); 
			int lng = (int) (location.getLongitude() * 1E6);
			Log.i("MeetMe Update", lat+" "+lng);
			GeoPoint point = new GeoPoint(lat, lng);


			mapController.animateTo(point); // mapController.setCenter(point);

			//mapController.setZoom(17);
			if(MongoConnection.getName().equals("???")) return;

			try {
				createMarker( MongoConnection.getName(), point, MongoConnection.getTwitterId(), drawable);

				if(System.currentTimeMillis()-lastUpdate > 60000){

					BasicDBObject query = new BasicDBObject().append("twitterId", MongoConnection.getTwitterId());
					BasicDBObject newDoc = new BasicDBObject();
					BasicDBObject toUpdate = new BasicDBObject();
					Log.v("geo update", point+"");
					toUpdate.append("lat", point.getLatitudeE6()).append("lon", point.getLongitudeE6());
					newDoc.append("$set", toUpdate);
					getMongoConnection().getDb().getCollection("users").update(query, newDoc);
					lastUpdate = System.currentTimeMillis();
				}

			} catch (MongoException e) {

				e.printStackTrace();
			} 
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
}
