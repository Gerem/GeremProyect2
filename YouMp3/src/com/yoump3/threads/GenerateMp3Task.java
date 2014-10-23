package com.yoump3.threads;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.json.JSONObject;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.caribelabs.utils.Validations;
import com.ricenbeans.yoump3.R;
import com.yoump3.activities.MainActivity;
import com.yoump3.dto.YouTubeVideo;
import com.yoump3.types.DownloadType;
import com.yoump3.utils.CallServices;
import com.yoump3.utils.Constants;
import com.yoump3.utils.DownloadManagerUtils;
import com.yoump3.utils.YouMp3Utils;

public class GenerateMp3Task extends AsyncTask<String, Integer, String>{
	private static Activity context;
	
	private ProgressBar loadingBar;
	private YouTubeVideo youtubeVideo;
	private static DownloadManagerUtils downloadUtils;
	private long lastDownload=-1L;
	private static ProgressBar downloadingBar;
	private static TextView percentView;
	private static ImageView cancelDownload;
	private static LinearLayout downloadLayout;
	private static Button openDownload;
	private static Button restartDownload;
	static boolean downloading;
	private DownloadType downloadType;	
	private List<Exception> exceptions = new ArrayList<Exception>();
	public GenerateMp3Task(Activity context, YouTubeVideo youtubeVideo,DownloadManagerUtils downloadUtils, DownloadType downloadType){
		this.context		= context;		
		this.youtubeVideo 	= youtubeVideo;
		
		if(Validations.validateIsNotNull(downloadUtils))
			this.downloadUtils	= downloadUtils;
		else 
			this.downloadUtils = new DownloadManagerUtils(context);
		
		this.cancelDownload = (ImageView) context.findViewById(R.id.cancelDownload);
		this.downloadingBar = (ProgressBar)context.findViewById(R.id.downloadingBar);
        this.percentView  	= (TextView)context.findViewById(R.id.percentage);
        this.downloadLayout = (LinearLayout)context.findViewById(R.id.downloadLayout);
        this.openDownload 	= (Button)context.findViewById(R.id.openDownload);
        this.restartDownload= (Button)context.findViewById(R.id.restartDownload);
        this.downloadType 	= downloadType;        
        downloadingBar.setIndeterminate(true);
		this.resetComponents();
	}
	private static void resetComponents() {
		openDownload.setVisibility(View.GONE);
		percentView.setVisibility(View.VISIBLE);
		percentView.setText("");
		downloadingBar.setVisibility(View.VISIBLE);
		downloadingBar.setProgress(0);
		cancelDownload.setVisibility(View.GONE);
		restartDownload.setVisibility(View.GONE);
		downloading = true;
	}
	private static void hideComponents(){
		downloadLayout.setVisibility(View.GONE);
	}
	@Override
	protected String doInBackground(String... params) {
		try {
			if(DownloadType.MP3.equals(downloadType)){
				JSONObject json  = CallServices.callService(Constants.MP3_API + Constants.YOUTUBE_URL + youtubeVideo.getId());
				youtubeVideo.setDownloadUrl(json.getString(Constants.LINK));
			}else if(DownloadType.VIDEO.equals(downloadType)){
				
			}
		}catch(Exception e){
			exceptions.add(e);			
		}
		return null;
	}
	@Override
	protected void onPostExecute(String result) {		
		for (Exception e : exceptions) {			
			YouMp3Utils.showCustomToast(context, context.getString(R.string.privacyPoliciMsg), Color.RED);	
			this.hideComponents();
			resetComponents();
			return;			
	    }
		downloadUtils.setTitle(youtubeVideo.getTitle() + ".mp3");
		downloadUtils.setDescription(context.getString(R.string.app_name));
		downloadUtils.setDownloadUrl(youtubeVideo.getDownloadUrl());		
				
		downloadUtils.onAction(onNotificationClick, DownloadManager.ACTION_NOTIFICATION_CLICKED);
		
		downloadUtils.startDownload();
		cancelDownload.setVisibility(View.VISIBLE);

		if(Validations.validateIsNull(downloadUtils.getDownloadUrl())){
			downloadLayout.setVisibility(View.GONE);	
			resetComponents();
			return;
		}
		cancelDownload.setOnClickListener(new OnClickListener() {
						
			@Override
			public void onClick(View v) {				
				close();
				downloadUtils.getDownloadManager().remove(downloadUtils.getDownloadId());
				downloadLayout.setVisibility(View.GONE);
				resetComponents();								
			}
		});
		// Store values between instances here
		SharedPreferences preferences = context.getPreferences(context.MODE_PRIVATE);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putLong(Constants.DOWNLOAD_ID, downloadUtils.getDownloadId()); // value to store
		editor.putString(Constants.TITLE, youtubeVideo.getTitle()); // value to store
		editor.putString(Constants.DESCRIPTION, youtubeVideo.getDescription()); // value to store				
		editor.putString(Constants.IMAGE_URL, youtubeVideo.getThumbnail()); // value to store
		editor.putString(Constants.YOUTUBE_ID, youtubeVideo.getId()); // value to store
		// Commit to storage
		editor.commit();
		this.setProgress();
		
	}
	public void setProgress(){
		
		new Thread(null,new Runnable() {
			
            @Override
            public void run() {
                while (downloading) {
                    
                    DownloadManager.Query q = new DownloadManager.Query();
                    q.setFilterById(downloadUtils.getDownloadId());

                    final Cursor cursor = downloadUtils.getDownloadManager().query(q);
                    
                    cursor.moveToFirst();
                    try{
                    	Log.d("Downloading Thread", downloadUtils.statusMessage(cursor));
                    }catch(Exception e){
                  	  	close();	
        				downloadUtils.getDownloadManager().remove(downloadUtils.getDownloadId());
        				
        				downloadLayout.setVisibility(View.GONE);
        				resetComponents();        				
             		    break;
                    }
                    if(!downloading)
                    	break;
                    if (Validations.validateIsNotNull(cursor) && downloadUtils.statusType(cursor).equals(DownloadManager.STATUS_SUCCESSFUL)){
                  	    close();              		                 		   
                    }
                   
                    
                    int bytes_downloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                    int bytes_total = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                    
                    final String bytesDownloaded = NumberFormat.getNumberInstance(Locale.US).format(bytes_downloaded/1000);
                    final String totalBytes	   = NumberFormat.getNumberInstance(Locale.US).format(bytes_total/1000);
                    final int progress = (int) (bytes_downloaded*100.0/bytes_total);  
                    
  	                    context.runOnUiThread(new Runnable() {
  	
  	                        @Override
  	                        public void run() {	  
  	                        	downloadingBar.setIndeterminate(false);
  	                        	downloadingBar.setProgress(progress);
  	                        	if(progress != -1 && progress > 0){
  	                        		percentView.setText(bytesDownloaded + "Kb / "  + totalBytes + "Kb");
  	                        	}
  	                        									
								if (Validations.validateIsNotNull(cursor) && downloadUtils.statusType(cursor).equals(DownloadManager.STATUS_FAILED)){
									downloading = false;
									restartDownload.setVisibility(View.VISIBLE);
									openDownload.setVisibility(View.GONE);
									percentView.setText(context.getString(R.string.failed));
									downloadingBar.setVisibility(View.GONE);
									cancelDownload.setVisibility(View.GONE);
									return;
								}
								
								if(!downloading){ // Validating if status is successful
										
									downloadingBar.setVisibility(View.GONE);
								   	openDownload.setVisibility(View.VISIBLE);
								   	cancelDownload.setVisibility(View.GONE);
								   	percentView.setText(context.getString(R.string.complete));
								}
  	                        }
  	                    });                    
                    cursor.close();      
                    try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                }

            }
        }).start();
	}
	
	BroadcastReceiver onNotificationClick=new BroadcastReceiver() {
	    public void onReceive(Context ctxt, Intent intent) {
	    	intent= new Intent(ctxt, MainActivity.class);
	    	ctxt.startActivity(intent);
	    }
	  };
	  public void close(){
		downloading = false;
		SharedPreferences preferences = context.getPreferences(context.MODE_PRIVATE);
		SharedPreferences.Editor editor = preferences.edit();
		editor.remove(Constants.DOWNLOAD_ID);
		editor.remove(Constants.TITLE);
		editor.remove(Constants.DESCRIPTION);
		editor.remove(Constants.IMAGE_URL);
		editor.commit(); 
	  }	
}
