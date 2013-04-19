package com.jujunawak.meetme;

import java.net.UnknownHostException;

import android.os.AsyncTask;
import android.util.Log;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.mongodb.MongoURI;

public final class MongoConnection extends AsyncTask<String, Void, Boolean>{



	private static final String meetmeDBURI = "mongodb://jujunawak:jl5881@ds059947.mongolab.com:59947/meetme";
	private static Mongo mongo;
	private static DB db;
	private static MongoConnection mongoConnection;
	private static long twitterId = -1; 
	private static String name = "???";

	protected MongoConnection() {

	}

	public static MongoConnection getInstance() throws MongoException, UnknownHostException{
		mongoConnection = new MongoConnection();

		
		return mongoConnection;
	}




	public  Mongo getMongo() {
		return mongo;
	}
	public void setMongo(Mongo m){
		this.mongo = m;
	}


	public DB getDb() {
		return db;
	}

	private void setDB(DB db2) {
		this.db = db2;

	}

	public DBObject retrieveMeetMeAccount(long id){
		twitterId = id;
		DBObject ref = new BasicDBObject("twitterId", id);
		DBCursor res = db.getCollection("users").find(ref);
		DBObject account = null;
		if(res != null && res.hasNext()){
			account = res.next();

		}else{
			DBObject newAccount = new BasicDBObject("name", "notset")
			.append("sex", "notset")
			.append("age", "-1")
			.append("twitterId", id);

			db.getCollection("users").insert(newAccount);
			account = db.getCollection("users").find(ref).next();

		}
		return account;

	}

	public static long getTwitterId() {
		return twitterId;
	}

	public static void setTwitterId(long id){
		twitterId = id;
	}

	public static String getName() {
		return name;
	}

	public static void setName(String name) {
		MongoConnection.name = name;
	}

	@Override
	protected Boolean doInBackground(String... params) {
		Log.v("MongoConnection", "1");
		if(mongoConnection.getDb() == null){
			Log.v("MongoConnection", "2");
			try {
				mongoConnection.setMongo(new Mongo(new MongoURI(meetmeDBURI)));
				Log.v("MongoConnection", "4");
				mongoConnection.setDB(mongoConnection.getMongo().getDB("meetme"));
				Log.v("MongoConnection", "3");
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}
			
		}
		return true;
	}


}
