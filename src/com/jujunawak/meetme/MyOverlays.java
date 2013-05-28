package com.jujunawak.meetme;

import java.util.HashMap;
import java.util.Map.Entry;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.Toast;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

public class MyOverlays extends ItemizedOverlay<OverlayItem> {


	private static HashMap<Integer, OverlayItem> overlays;
	private static HashMap<Integer, Long> overlaysTwitterId;
	private static int cpt=0;
	private Context context;
	private OverlayItem previousoverlay;


	public MyOverlays(Context context, Drawable defaultMarker) {
		super(boundCenterBottom(defaultMarker));
		this.context = context;
		getOverlays();

	}

	@Override
	protected OverlayItem createItem(int i) {
		
			return getOverlays().get(i);
		
	}

	public void removeOverlay(long twitterId){
		getOverlays().remove(twitterId);
		overlaysTwitterId.remove(twitterId);
	}

	public void addOverlayItem(long twitterId, OverlayItem overlay) {
		
		/* TODO make a getter for OverlaysTwitterId to avoid this line */
		getOverlays();

		if(!overlaysTwitterId.values().contains(twitterId)){

			getOverlays().put(cpt,overlay);
			overlaysTwitterId.put(cpt++,twitterId);
			Log.v("overlay ", "new item");

		}else{
			for(Entry<Integer, Long> e : overlaysTwitterId.entrySet()){
				if(e.getValue().equals(twitterId)){
					getOverlays().put(e.getKey(), overlay);
					Log.v("overlay ", "update item");
					break;
				}
			}
		}
		Log.v("map overlays : ", overlays+"");
		Log.v("map twitters : ", overlaysTwitterId+"");

		if(getOverlays().size()>0){
			populate();
		}

		this.previousoverlay = overlay;
	}

	protected boolean onTap(int index) {
		OverlayItem overlayItem = getOverlays().get(index);
		Builder builder = new AlertDialog.Builder(context);
		builder.setMessage(""+overlayItem.getTitle());
		builder.setCancelable(true);
		builder.setPositiveButton("meet", new OkOnClickListener());
		builder.setNegativeButton("cancel", new CancelOnClickListener());
		AlertDialog dialog = builder.create();
		dialog.show();
		return true;
	};

	private final class CancelOnClickListener implements
	DialogInterface.OnClickListener {
		public void onClick(DialogInterface dialog, int which) {
			//			Toast.makeText(context, "", Toast.LENGTH_LONG)
			//			.show();
		}
	}

	private final class OkOnClickListener implements
	DialogInterface.OnClickListener {
		public void onClick(DialogInterface dialog, int which) {
			Toast.makeText(context, "sending request ...", Toast.LENGTH_LONG).show();
		}
	}

	public void init(){

		overlays = null;
		overlaysTwitterId = null;
	}

	public HashMap<Integer, OverlayItem> getOverlays() {
		if(overlays == null){
			overlays = new HashMap<Integer, OverlayItem>();
			overlaysTwitterId = new HashMap<Integer, Long>();

		}
		return overlays;
	}

	@Override
	public int size() {

		return overlays.size();
	}

	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		if(!shadow && (this.overlays.size()>0)){
			super.draw(canvas, mapView, shadow);
		}
	}

}
