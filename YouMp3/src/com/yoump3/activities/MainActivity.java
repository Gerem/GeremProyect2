package com.yoump3.activities;

import java.text.NumberFormat;
import java.util.Locale;

import android.app.ActionBar;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;

import com.caribelabs.utils.ImageLoader;
import com.caribelabs.utils.Utils;
import com.caribelabs.utils.Validations;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.ricenbeans.yoump3.R;
import com.yoump3.dto.YouTubeVideo;
import com.yoump3.threads.GenerateMp3Task;
import com.yoump3.threads.SearchTask;
import com.yoump3.types.DownloadType;
import com.yoump3.utils.Constants;
import com.yoump3.utils.DownloadManagerUtils;

public class MainActivity extends ActionBarActivity {
	
	private Activity context;
	private String searchedText;
	private Long downloadId;
	private ProgressBar downloadingBar;
	private TextView percentView;
	private ImageView cancelDownload;
	private LinearLayout downloadLayout;
	private Button openDownload;
	boolean downloading;
	private Thread loadingThread;
	
	private ImageView downloadingImage;
    private TextView downloadingTitle;
    private TextView downloadingTime;
    
    private SharedPreferences preferences;
    private static Button restartDownload;
	private DownloadManagerUtils downloadUtils;
	private AdView adView;
	private InterstitialAd interstitial;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		final ViewGroup actionBarLayout = (ViewGroup) getLayoutInflater().inflate(R.layout.custom_actionbar,null);
		final ActionBar actionBar = getActionBar();
		actionBar.setCustomView(actionBarLayout);
		loadInterstitial();
		context = this;
		downloading = false;
		this.downloadingImage = (ImageView)context.findViewById(R.id.imageDownlading);
        this.downloadingTitle = (TextView)context.findViewById(R.id.titleDownloading);
        this.downloadingTime  = (TextView)context.findViewById(R.id.time);
		
		downloadUtils		= new DownloadManagerUtils(context);
		this.cancelDownload = (ImageView) context.findViewById(R.id.cancelDownload);
		this.downloadingBar = (ProgressBar)context.findViewById(R.id.downloadingBar);
        this.percentView  = (TextView)context.findViewById(R.id.percentage);
        this.downloadLayout   = (LinearLayout)context.findViewById(R.id.downloadLayout);
        this.openDownload = (Button)context.findViewById(R.id.openDownload);
        this.restartDownload= (Button)context.findViewById(R.id.restartDownload);
		
        adView = new AdView(this);
        adView.setAdUnitId(this.getString(R.string.ad_footer));
        adView.setAdSize(AdSize.BANNER);
        AdRequest adRequest = new AdRequest.Builder().build();

     // el atributo android:id="@+id/mainLayout".
        LinearLayout layout = (LinearLayout)findViewById(R.id.fBanner);

        // Añadirle adView.
        layout.addView(adView);
        // Cargar adView con la solicitud de anuncio.
        adView.loadAd(adRequest);
        
		if(this.getDownloadId() != 0){
			String imgUrl = this.getPreferences().getString(Constants.IMAGE_URL, "");
			String title  = this.getPreferences().getString(Constants.TITLE, "");
			ImageLoader imageLoader = new ImageLoader(context,100,70,null);
			imageLoader.DisplayImage(imgUrl, R.drawable.transparent, downloadingImage);
			
			downloadingTitle.setText(title);
			downloadingTime.setText(Utils.getNow("hh:mma"));
			
			downloadLayout.setVisibility(View.VISIBLE);				
			downloading = true;
			setProgress();
		}
		
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
	  super.onSaveInstanceState(savedInstanceState);
	  // Save UI state changes to the savedInstanceState.
	  // This bundle will be passed to onCreate if the process is
	  // killed and restarted.
	  savedInstanceState.putString(Constants.SEARCHED_TXT, searchedText);
	  
	  // etc.
	}
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
	  super.onRestoreInstanceState(savedInstanceState);	  
	  // Restore UI state from the savedInstanceState.
	  // This bundle has also been passed to onCreate.
	  searchedText = savedInstanceState.getString(Constants.SEARCHED_TXT);
	  if(Validations.validateIsNotNullAndNotEmpty(searchedText)){		  
		  SearchTask searchTask = new SearchTask(context, Constants.YOUTUBE_API + searchedText);
		  searchTask.execute();
	  }
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);

		// Get the SearchView and set the searchable configuration
	    SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
	    SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
	    
	    // Assumes current activity is the searchable activity
	    searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
	    searchView.setIconifiedByDefault(true); 
	    searchView.setOnQueryTextListener(new OnQueryTextListener() {
			
			@Override
			public boolean onQueryTextSubmit(String query) {
				query = query.replace(" ", "%20");
				searchedText = query;
				SearchTask searchTask = new SearchTask(context, Constants.YOUTUBE_API + query);
				searchTask.execute();				
				return false;
			}
			
			@Override
			public boolean onQueryTextChange(String newText) {
				// TODO Auto-generated method stub
				return false;
			}
		});
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.search) {
			onSearchRequested();
			return true;
		}
		if (id == R.id.downloads) {
				openDownloads(item.getActionView());
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	@Override
	public void onPause(){
		super.onPause();
		
	}
	@Override
	public void onDestroy(){
		super.onDestroy();
		downloading = false;
		displayInterstitial();
		
	}
	public void setProgress(){
		downloadId = getDownloadId();
		loadingThread = new Thread(new Runnable() {

            @Override
            public void run() {
                while (downloading) {
                    
                    DownloadManager.Query q = new DownloadManager.Query();
                    q.setFilterById(downloadId);

                    final Cursor cursor = downloadUtils.getDownloadManager().query(q);
                    
                    cursor.moveToFirst();
                    try{
                    	Log.d("INSIDE THREAD", downloadUtils.statusMessage(cursor));
                    }catch(Exception e){
                    	downloading = false;	
        				downloadUtils.getDownloadManager().remove(downloadId);
        				downloadLayout.setVisibility(View.GONE);
        				openDownload.setVisibility(View.GONE);
        				percentView.setVisibility(View.VISIBLE);
        				SharedPreferences preferences = context.getPreferences(context.MODE_PRIVATE);
        			    SharedPreferences.Editor editor = preferences.edit();
        			    editor.remove(Constants.DOWNLOAD_ID);
        			    editor.remove(Constants.TITLE);
        			    editor.remove(Constants.DESCRIPTION);
        			    editor.remove(Constants.IMAGE_URL);
        			    editor.commit();
             		    break;
                    }
                    if(!downloading || Validations.validateIsNull(cursor))
                    	break;
                    if (Validations.validateIsNotNull(cursor) && downloadUtils.statusType(cursor).equals(DownloadManager.STATUS_SUCCESSFUL)){
             		   	downloading = false;  
             		    SharedPreferences preferences = context.getPreferences(context.MODE_PRIVATE);
             		    SharedPreferences.Editor editor = preferences.edit();
             		    editor.remove(Constants.DOWNLOAD_ID);
             		    editor.remove(Constants.TITLE);
             		    editor.remove(Constants.DESCRIPTION);
             		    editor.remove(Constants.IMAGE_URL);
             		    editor.commit();
                    }
                    int bytes_downloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                    int bytes_total = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                    
                    final String bytesDownloaded = NumberFormat.getNumberInstance(Locale.US).format(bytes_downloaded/1000);
                    final String totalBytes	   = NumberFormat.getNumberInstance(Locale.US).format(bytes_total/1000);
                    final int progress = (int) (bytes_downloaded*100.0/bytes_total);  
                    if(progress != -1){
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
                    }
                    cursor.close();
                    try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                }

            }
        });
		loadingThread.start();
		
		
		cancelDownload.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {				
				downloading = false;	
				downloadUtils.getDownloadManager().remove(downloadId);
				downloadLayout.setVisibility(View.GONE);
				openDownload.setVisibility(View.GONE);
				percentView.setVisibility(View.VISIBLE);
				SharedPreferences preferences = context.getPreferences(context.MODE_PRIVATE);
			    SharedPreferences.Editor editor = preferences.edit();
			    editor.remove(Constants.DOWNLOAD_ID);
			    editor.remove(Constants.TITLE);
			    editor.remove(Constants.DESCRIPTION);
			    editor.remove(Constants.IMAGE_URL);
			    editor.commit();
			}
		});
	}
	public void cancelDownload(){
		downloading = false;	
		downloadUtils.getDownloadManager().remove(downloadId);
		downloadLayout.setVisibility(View.GONE);
		openDownload.setVisibility(View.GONE);
		percentView.setVisibility(View.VISIBLE);
		  
	    SharedPreferences.Editor editor = this.getPreferences().edit();
	    editor.remove(Constants.DOWNLOAD_ID);
	    editor.remove(Constants.TITLE);
	    editor.remove(Constants.DESCRIPTION);
	    editor.remove(Constants.IMAGE_URL);
	    editor.commit();
	}
	public void openDownloads(View v){
		Intent i = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS);
        startActivity(i);
	}
	public void restartDownload(View v){
		GenerateMp3Task generateMp3 = null;
		ImageLoader imageLoader 	= null;
		// Getting tube video
		YouTubeVideo tubeVideo		= new YouTubeVideo();
		tubeVideo.setTitle(this.getPreferences().getString(Constants.TITLE, ""));
		tubeVideo.setThumbnail(this.getPreferences().getString(Constants.IMAGE_URL, ""));
		tubeVideo.setId(this.getPreferences().getString(Constants.YOUTUBE_ID, ""));
		generateMp3 = new GenerateMp3Task(context, tubeVideo,null, DownloadType.MP3);
		generateMp3.execute();
		
		imageLoader = new ImageLoader(context,100,70,null);
		imageLoader.DisplayImage(tubeVideo.getThumbnail(), R.drawable.transparent, downloadingImage);
		
		downloadingTitle.setText(tubeVideo.getTitle());
		downloadingTime.setText(Utils.getNow("hh:mma"));
		
		downloadLayout.setVisibility(View.VISIBLE);
	}
	public Long getDownloadId(){
		// Store values between instances here
		SharedPreferences preferences = context.getPreferences(context.MODE_PRIVATE);
		return preferences.getLong(Constants.DOWNLOAD_ID, 0);	
	}
	
	public SharedPreferences getPreferences() {
		return context.getPreferences(context.MODE_PRIVATE);
	}
	public void loadInterstitial(){
    	// Create the interstitial.
        interstitial = new InterstitialAd(this);
        interstitial.setAdUnitId(this.getString(R.string.ad_fullscreen));

        // Create ad request.
        AdRequest interstitialRequest = new AdRequest.Builder().build();

        // Begin loading your interstitial.
        interstitial.loadAd(interstitialRequest);        
    }
 // Invoke displayInterstitial() when you are ready to display an interstitial.
    public void displayInterstitial() {
	  if (interstitial.isLoaded()) {		  
		  interstitial.show();
	  }
    }
}
