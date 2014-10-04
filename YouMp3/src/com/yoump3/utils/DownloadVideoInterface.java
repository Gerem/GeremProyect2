package com.yoump3.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class DownloadVideoInterface {
	// finds video through regex and URL decoding.
		public static String FindYoutubeVideo(String html) {
			
			String ResultString = null;
			Pattern urlencod = Pattern.compile("\"url_encoded_fmt_stream_map\": \"([^\"]*)\"");
            Matcher urlencodMatch = urlencod.matcher(html);
            if (urlencodMatch.find()) {
                String url_encoded_fmt_stream_map;
                url_encoded_fmt_stream_map = urlencodMatch.group(1);

                // normal embedded video, unable to grab age restricted videos
                Pattern encod = Pattern.compile("url=(.*)");
                Matcher encodMatch = encod.matcher(url_encoded_fmt_stream_map);
                if (encodMatch.find()) {
                    String sline = encodMatch.group(1);

//                    extractUrlEncodedVideos(sNextVideoURL, sline);
                }
            }
			return ResultString;

		}
		// downloads html of a webpage, youtube in our case.
		public static String downloadWebpage(String link) throws IOException {

			URL url = new URL(link);
			URLConnection urlConnection = url.openConnection();
			urlConnection.setConnectTimeout(10000);
			urlConnection.setReadTimeout(10000);
			BufferedReader breader = new BufferedReader(new InputStreamReader(
					urlConnection.getInputStream()));

			StringBuilder stringBuilder = new StringBuilder();

			String line;
			while ((line = breader.readLine()) != null) {
				stringBuilder.append(line);
			}

			return stringBuilder.toString();
		}
}
