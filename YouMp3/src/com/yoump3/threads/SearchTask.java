package com.yoump3.threads;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.os.AsyncTask;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.caribelabs.utils.Utils;
import com.caribelabs.utils.Validations;
import com.ricenbeans.yoump3.R;
import com.yoump3.adapters.YouTubeListAdapter;
import com.yoump3.dto.YouTubeVideo;
import com.yoump3.utils.CallServices;

public class SearchTask extends AsyncTask<String, Integer, String>{
	private Activity context;
	private String query;
	private ProgressBar loadingBar;
	private ListView listView;
	private LinearLayout searchLayout;
	private List<YouTubeVideo> youTubeLst;
	private LinearLayout notFound;
	private TextView textView;
	public SearchTask(Activity context, String query){
		this.context	= context;
		this.query   	= query;
		this.textView	= (TextView) context.findViewById(R.id.introMsg);
		this.youTubeLst = new ArrayList<YouTubeVideo>();
		this.listView     	= (ListView) 	  context.findViewById(R.id.list);
		this.loadingBar = (ProgressBar) context.findViewById(R.id.loadingBar);
		this.searchLayout = (LinearLayout)context.findViewById(R.id.videoSearched);
		this.loadingBar.setVisibility(View.VISIBLE);		
		this.searchLayout.setVisibility(View.GONE);
		this.notFound  = (LinearLayout) context.findViewById(R.id.notFound);
		textView.setVisibility(View.GONE);
		
	}
	@Override
	protected String doInBackground(String... params) {
		try {
			JSONObject json  = CallServices.callService(query);
			JSONObject feed = json.getJSONObject("feed");   
	        JSONArray entry = feed.getJSONArray("entry");
	        if(Validations.validateIsNotNullAndNotEmpty(entry)){
				for (int i = 0; i < entry.length(); i++) {
					JSONObject obj   		= entry.getJSONObject(i);
					JSONObject media 		= obj.getJSONObject("media$group");				
					
					Date published = Utils.stringToDate(media.getJSONObject("yt$uploaded").getString("$t"), Utils.YOUTUBE_DATE_PATTERN);
					
					YouTubeVideo youTube = new YouTubeVideo();
					youTube.setId(media.getJSONObject("yt$videoid").getString("$t"));
					youTube.setTitle(media.getJSONObject("media$title").getString("$t"));
					youTube.setUploader(obj.getJSONArray("author").getJSONObject(0).getJSONObject("name").getString("$t"));
					youTube.setThumbnail(media.getJSONArray("media$thumbnail").getJSONObject(0).getString("url"));
					youTube.setDescription(media.getJSONObject("media$description").getString("$t"));				
					youTube.setDuration(media.getJSONObject("yt$duration").getLong("seconds"));
					youTube.setViewCount(obj.getJSONObject("yt$statistics").getLong("viewCount"));
	//				youTube.setVideoUrl(media.getJSONArray("media$content").getJSONObject(1).getString("url"));
					youTube.setPublished(published);
					
					youTubeLst.add(youTube);
				}
	        }
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	@Override
	protected void onPostExecute(String result) {	
		YouTubeListAdapter adapter = new YouTubeListAdapter(context, R.layout.videos_list, youTubeLst);
		listView.setAdapter(adapter);
		adapter.notifyDataSetChanged();
		loadingBar.setVisibility(View.GONE);
		searchLayout.setVisibility(View.VISIBLE);			
		notFound.setVisibility(View.GONE);
		
		if(Validations.validateListIsNullOrEmpty(youTubeLst))
			notFound.setVisibility(View.VISIBLE);
	}
}
