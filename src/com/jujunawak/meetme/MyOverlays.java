package com.jujunawak.meetme;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.widget.Toast;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;

public class MyOverlays extends ItemizedOverlay<OverlayItem> {


	private List<OverlayItem> overlays;


	private Context context;
	private OverlayItem previousoverlay;


	public MyOverlays(Context context, Drawable defaultMarker) {
		super(boundCenterBottom(defaultMarker));
		this.context = context;

	}

	@Override
	protected OverlayItem createItem(int i) {

		return getOverlays().get(i);
	}



	public void addOverlay(OverlayItem overlay) {
		//if(getOverlays().size()==0){
			getOverlays().add(overlay);
		//}else{
		//	getOverlays().set(0, overlay);
		//}
		populate();

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

	public List<OverlayItem> getOverlays() {
		if(overlays == null){
			this.overlays = new ArrayList<OverlayItem>();

		}
		return overlays;
	}

	@Override
	public int size() {

		return this.overlays.size();
	}


}
