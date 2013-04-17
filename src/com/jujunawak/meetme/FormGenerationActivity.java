package com.jujunawak.meetme;

import java.net.UnknownHostException;
import java.util.Map.Entry;

import makemachine.android.formgenerator.FormActivity;
import makemachine.android.formgenerator.FormWidget;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoException;

public class FormGenerationActivity extends FormActivity {


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		generateForm(FormActivity.parseFileToString(this, "expert.json"));


		setContentView(_container);

		Button ok = new Button(_container.getContext());
		ok.setText("OK");
		ok.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Log.v("form generation","clicked ok");
				try {
					
					
					BasicDBObject query = new BasicDBObject().append("twitterId", MongoConnection.getTwitterId());
					BasicDBObject newDoc = new BasicDBObject();
					BasicDBObject toUpdate = new BasicDBObject();
					
					for(Entry<String, FormWidget> entry : _map.entrySet()){
						toUpdate.append(entry.getKey(), entry.getValue().getValue()); 
					}
					
					
					newDoc.append("$set", toUpdate);
					MongoConnection.getInstance().getDb().getCollection("users").update(query, newDoc);
				} catch (MongoException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		});
		_container.addView(ok);
		
		
		Button cancel = new Button(_container.getContext());
		cancel.setText("Cancel");
		cancel.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Log.v("form generation","clicked cancel");

			}
		});
		_container.addView(cancel);


		//Builder dial = new AlertDialog.Builder(_container.getContext());
		//	dial.setView(_container);
		//		
		//		dial.setPositiveButton("OK", new OnClickListener() {
		//			
		//			@Override
		//			public void onClick(DialogInterface dialog, int which) {
		//				
		//				Log.v("form generation","clicked ok");
		//			}
		//		});
		//		dial.show();

	}




}
