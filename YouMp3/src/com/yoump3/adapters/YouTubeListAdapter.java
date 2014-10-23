package com.yoump3.adapters;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import org.ocpsoft.prettytime.PrettyTime;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.caribelabs.utils.ImageLoader;
import com.caribelabs.utils.Utils;
import com.caribelabs.utils.Validations;
import com.ricenbeans.yoump3.R;
import com.yoump3.dto.YouTubeVideo;
import com.yoump3.threads.GenerateMp3Task;
import com.yoump3.types.DownloadType;
import com.yoump3.utils.Constants;
import com.yoump3.utils.DownloadManagerUtils;

public class YouTubeListAdapter extends ArrayAdapter<YouTubeVideo>{
	private Activity context;
	private int layoutResourceId;
	private List<YouTubeVideo> list;	    
    private Uri uriYouTube;
   
    private ImageView downloadingImage;
    private TextView downloadingTitle;
    private TextView downloadingTime;
    private LinearLayout downloadLayout;
    private boolean downloading;
    private DownloadManagerUtils downloadUtils;
    private Long downloadId;
	public YouTubeListAdapter(Activity context, int layoutResourceId,List<YouTubeVideo> list) {
		super(context, layoutResourceId, list);
		this.context = context;
		this.list = list;
		this.layoutResourceId = layoutResourceId;						        
        this.downloadUtils    = new DownloadManagerUtils(context);               		        		
        
        this.downloadingImage = (ImageView)context.findViewById(R.id.imageDownlading);
        this.downloadingTitle = (TextView)context.findViewById(R.id.titleDownloading);
        this.downloadingTime  = (TextView)context.findViewById(R.id.time);
        this.downloadLayout   = (LinearLayout)context.findViewById(R.id.downloadLayout);
	}
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		final YouTubeVideo tubeVideo = list.get(position);		
		if (row == null) {
			LayoutInflater inflater = context.getLayoutInflater();
			row = inflater.inflate(layoutResourceId, parent, false);
		}
		HolderView holderView = new HolderView();
		holderView.image    		= (ImageView) row.findViewById(R.id.image);
		holderView.duration 		= (TextView) row.findViewById(R.id.duration);
		holderView.title			= (TextView) row.findViewById(R.id.title);
		holderView.timeAgo			= (TextView) row.findViewById(R.id.timeAgo);
		holderView.author			= (TextView) row.findViewById(R.id.author);
		holderView.viewCount		= (TextView) row.findViewById(R.id.viewCount);
		holderView.settingsButton	= (ImageView) row.findViewById(R.id.settingsButton);
		
		PrettyTime p = new PrettyTime(Locale.US);
		//Formating and showing pretty time
		holderView.timeAgo.setText(Utils.capitalize(p.format(tubeVideo.getPublished())));		
		holderView.title.setText(tubeVideo.getTitle());
		holderView.duration.setText(Utils.secondsToMinutes(tubeVideo.getDuration()));
		holderView.author.setText(tubeVideo.getUploader());
		
		//Formatting viewCount
		String viewCount = NumberFormat.getNumberInstance(Locale.US).format(tubeVideo.getViewCount());
		holderView.viewCount.setText(viewCount + " " + context.getString(R.string.views));
		
		ImageLoader imageLoader = new ImageLoader(context,120,70,null);
		imageLoader.DisplayImage(tubeVideo.getThumbnail(), R.drawable.transparent, holderView.image);
		
		holderView.image.setOnClickListener(new OnClickListener() {
			 @Override
		     public void onClick(View arg0) {
				 
				 Dialog videoDialog				= new Dialog(context);
				 videoDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
				 videoDialog.setContentView(R.layout.popup_video);
				 				
				 WebView webView = (WebView) videoDialog.findViewById(R.id.youtubeWV);
				 webView.getSettings().setJavaScriptEnabled(true);
				 webView.getSettings().setPluginState(PluginState.ON);
				 webView.loadUrl("http://www.youtube.com/embed/" + tubeVideo.getId() + "?autoplay=1&vq=small&autoplay=true");

				 webView.setBackgroundColor(0x00000000);
				
				 webView.setKeepScreenOn(true);
				
				 webView.setHorizontalScrollBarEnabled(false);
				 webView.setVerticalScrollBarEnabled(false);
				
				 webView.getSettings().setBuiltInZoomControls(false);
				 webView.setWebChromeClient(new WebChromeClient());				 
				 videoDialog.show();
		         
			 }
		});
		holderView.settingsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				CharSequence colors[] = new CharSequence[] {"Download Mp3"};
				 AlertDialog.Builder builder = new AlertDialog.Builder(context);	                
	                builder.setItems(colors, new DialogInterface.OnClickListener() {
	                    @Override
	                    public void onClick(DialogInterface dialog, int which) {
	                    	SharedPreferences preferences = context.getPreferences(context.MODE_PRIVATE);
					        downloadId = preferences.getLong(Constants.DOWNLOAD_ID, 0); 
							if(downloadId != 0 ){
								DownloadManager.Query q = new DownloadManager.Query();
			                    q.setFilterById(downloadId);

			                    final Cursor cursor = downloadUtils.getDownloadManager().query(q);
			                    
			                    cursor.moveToFirst();
			                    if ((Validations.validateIsNotNull(cursor) && downloadUtils.statusType(cursor).equals(DownloadManager.STATUS_RUNNING))){
									Utils.showToastMessage(context, context.getString(R.string.downloadInProgress));	
									return;
			                    }
							}
		                    GenerateMp3Task generateMp3 = null;
							ImageLoader imageLoader 	= null;
							generateMp3 = new GenerateMp3Task(context, tubeVideo,downloadUtils, DownloadType.MP3);
							generateMp3.execute();
							
							imageLoader = new ImageLoader(context,100,70,null);
							imageLoader.DisplayImage(tubeVideo.getThumbnail(), R.drawable.transparent, downloadingImage);
							
							downloadingTitle.setText(tubeVideo.getTitle());
							downloadingTime.setText(Utils.getNow("hh:mma"));
							
							downloadLayout.setVisibility(View.VISIBLE);
	                        
	                    }
	                });
	                builder.show();	                				
			}			
		});
		
		return row;
	}
	static class HolderView {
		ImageView image;
		TextView duration;
		TextView title;	
		TextView timeAgo;
		TextView author;
		TextView viewCount;
		ImageView settingsButton;
	}
	
	
}

