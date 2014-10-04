package com.yoump3.utils;

import com.caribelabs.utils.Validations;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;

public class DownloadManagerUtils {
	private String title;
	private String description;
	private String dirPath;
	private DownloadManager downloadManager;
	private long downloadId=-1L;
	private Activity context;
	private String downloadUrl;
	
	public DownloadManagerUtils(Activity context){
		this.context		= context;		
		downloadManager 	= (DownloadManager)context.getSystemService(context.DOWNLOAD_SERVICE);
		
	}
	
	public void startDownload(){
		Uri uri = Uri.parse(downloadUrl);
		
		Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).mkdirs();
		DownloadManager.Request request = new DownloadManager.Request(uri);
		request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI |
                					   DownloadManager.Request.NETWORK_MOBILE);
		request.setAllowedOverRoaming(false);
		request.setTitle(title);
		request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
		request.setDescription(description);
		
		if(Validations.validateIsNull(dirPath))
			request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,title);
		else
			request.setDestinationInExternalPublicDir(dirPath,title);
		
		downloadId = downloadManager.enqueue(request);
        					
	}
	public Integer statusType(Cursor c) {	    
		Integer response= 0;
		try{
			response = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
		}catch(Exception e){
			e.printStackTrace();
		}
		return response;
	}
	public String statusMessage(Cursor c) {
	    String msg = "???";

	    switch (c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS))) {
	    case DownloadManager.STATUS_FAILED:
	        msg = "Download failed!";
	        break;

	    case DownloadManager.STATUS_PAUSED:
	        msg = "Download paused!";
	        break;

	    case DownloadManager.STATUS_PENDING:
	        msg = "Download pending!";
	        break;

	    case DownloadManager.STATUS_RUNNING:
	        msg = "Download in progress!";
	        break;

	    case DownloadManager.STATUS_SUCCESSFUL:
	        msg = "Download complete!";
	        break;

	    default:
	        msg = "Download is nowhere in sight";
	        break;
	    }

	    return (msg);
	}
	public void onAction(BroadcastReceiver br, String action){
		context.registerReceiver(br,new IntentFilter(action));
		
	}
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Activity getContext() {
		return context;
	}

	public void setContext(Activity context) {
		this.context = context;
	}

	public String getDownloadUrl() {
		return downloadUrl;
	}

	public void setDownloadUrl(String url) {
		this.downloadUrl = url;
	}

	public long getDownloadId() {
		return downloadId;
	}

	public DownloadManager getDownloadManager() {
		return downloadManager;
	}

	public void setDownloadManager(DownloadManager downloadManager) {
		this.downloadManager = downloadManager;
	}

	public String getDirPath() {
		return dirPath;
	}

	public void setDirPath(String dirPath) {
		this.dirPath = dirPath;
	}
}
