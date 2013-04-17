package com.jujunawak.meetme;

import java.net.UnknownHostException;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.mongodb.MongoURI;

public final class MongoConnection {



	private static final String meetmeDBURI = "mongodb://jujunawak:jl5881@ds059947.mongolab.com:59947/meetme";
	private Mongo mongo;
	private DB db;
	private static MongoConnection mongoConnection;
	private static long twitterId = -1; 

	protected MongoConnection() {
		
	}

	public static MongoConnection getInstance() throws MongoException, UnknownHostException{
		mongoConnection = new MongoConnection();
		if(mongoConnection.getDb() == null){
			mongoConnection.setMongo(new Mongo(new MongoURI(meetmeDBURI)));
			mongoConnection.setDB(mongoConnection.getMongo().getDB("meetme"));
		}
		
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


}
