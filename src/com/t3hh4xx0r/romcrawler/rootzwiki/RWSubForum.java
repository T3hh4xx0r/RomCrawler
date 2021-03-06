package com.t3hh4xx0r.romcrawler.rootzwiki;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.t3hh4xx0r.romcrawler.R;
import com.t3hh4xx0r.romcrawler.Receiver;
import com.t3hh4xx0r.romcrawler.SettingsMenu;
import com.t3hh4xx0r.romcrawler.activities.ListFragmentViewPagerActivity;
import com.t3hh4xx0r.romcrawler.activities.MainActivity;
import com.t3hh4xx0r.romcrawler.adapters.RWSubAdapter;
import com.t3hh4xx0r.romcrawler.adapters.TitleResults;

import eu.erikw.PullToRefreshListView;
import eu.erikw.PullToRefreshListView.OnRefreshListener;

public class RWSubForum extends Activity {

	public static ArrayList<String> entriesArray;
    public static ArrayList<String> threadArray;
    static ArrayList<String> authorArray;
    static ArrayList<String> titlesArray;
    static ArrayList<String> TITLES;
    static ArrayList<String> subList;
    public static ArrayList<String> identList;
    public static ArrayList<String> typeList;
    String message;
    String url;
    String threadTitle = null;  
    String ident;
    ProgressBar pB;
    PullToRefreshListView listView;
    Button fb;
    Context ctx;
    int subCount;
    boolean first;

    
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.main);
        ctx = (RWSubForum.this);

        Bundle extras = getIntent().getExtras();
        url = extras.getString("url");
        try {
        	first = extras.getBoolean("first");
        } catch (Exception e) {
        	first = false;
        }
        entriesArray = new ArrayList<String>();
        threadArray = new ArrayList<String>();
        subList = new ArrayList<String>();
        titlesArray = new ArrayList<String>();
        authorArray = new ArrayList<String>();
        identList = new ArrayList<String>();
        TITLES = new ArrayList<String>();
        typeList = new ArrayList<String>();
        listView = (PullToRefreshListView) findViewById(android.R.id.list);
        listView.setOnRefreshListener(new OnRefreshListener() {
			public void onRefresh() {
				threadArray.clear();
				TITLES.clear();
	        	new CreateArrayListTask().execute(url);
	            listView.onRefreshComplete();
			}
        });
        pB = (ProgressBar) findViewById(R.id.progressBar1);
		
        final Vibrator vibe = (Vibrator) getSystemService(VIBRATOR_SERVICE) ;

        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> a, View v, int position, long id) { 
            	
    		 Object o = listView.getItemAtPosition(position);
             TitleResults fullObject = (TitleResults)o;
             threadTitle = fullObject.getItemName();
             String author = fullObject.getAuthorDate();
             String ident = fullObject.getIdent();
             Intent intent;
             Bundle b = new Bundle();
             boolean isForum = fullObject.getIsForum();
             if (isForum) {
                 intent = new Intent(RWSubForum.this, RWSubForum.class);
                 b.putString("type", "rwsf");
            	 b.putInt("c", position);
             } else {
            	 intent = new Intent(RWSubForum.this, ListFragmentViewPagerActivity.class);
            	 b.putInt("c", position-subCount);            		 
            	 b.putString("type", "rw");
                 b.putStringArrayList("urls", threadArray);
                 b.putStringArrayList("titles", TITLES);
                 b.putStringArrayList("idents", identList);
             }
             b.putString("url", fullObject.getUrl());
             b.putString("author", author);
             b.putString("ident", ident);             
             intent.putExtras(b);
             startActivity(intent);    	
            }  
           });
        listView.setOnItemLongClickListener(new OnItemLongClickListener() {
        	@Override
        	public boolean onItemLongClick(AdapterView<?> a, View v, int position, long id) {
    		vibe.vibrate(50);
      		Object o = listView.getItemAtPosition(position);
            TitleResults fullObject = (TitleResults)o;
            threadTitle = fullObject.getItemName();
            String URL = fullObject.getUrl();
            Intent iW = new Intent(Intent.ACTION_VIEW, Uri.parse(URL));
    		startActivity(Intent.createChooser(iW, getResources().getString(R.string.browser_view)));
        	return false;	
        	}
        });
        new CreateArrayListTask().execute(url);
    }
 
    private class CreateArrayListTask extends AsyncTask<String, Void, ArrayList<TitleResults>> {       
        final ArrayList<TitleResults> results = new ArrayList<TitleResults>();
        
		@Override
		protected ArrayList<TitleResults> doInBackground(String... urls) {
			subCount = 0;
	        TitleResults titleArray =  new TitleResults();
			StringBuilder whole = new StringBuilder();
	        authorArray = new ArrayList<String>();
	        subList.clear();
	        
			try {
				URL url = new URL(urls[0]);
				HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
				urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
				try {
					BufferedReader in = new BufferedReader(
						new InputStreamReader(new BufferedInputStream(urlConnection.getInputStream())));
					String inputLine;
					while ((inputLine = in.readLine()) != null)
						whole.append(inputLine);
					in.close();
				} catch (IOException e) {
	       			e.printStackTrace();
				}
				finally {
					urlConnection.disconnect();
				}
			} catch (Exception e) {
       			e.printStackTrace();
			}
			Document doc = Parser.parse(whole.toString(), urls[0]);
			Elements threads = doc.select(".topic_title");
	       	Elements authors = doc.select("span[class]");
	       	Elements subs = doc.select(".col_c_forum");
	       	for (Element sub : subs) {
    			String subTitle = sub.text();
    			if (!subList.contains(subTitle)) {
	    			subList.add(subTitle);
		       		subCount++;
    			}
   				typeList.add("forum");
	       		titleArray =  new TitleResults();
	   			String shits[] = sub.toString().split(" ", 6);
	   			String shit = shits[shits.length-2];
	   			shit = new String(shit.replace("href=", ""));
	   			shit = new String(shit.replaceAll("\"", "")); 
	   				

       			entriesArray.add(shit);
       			titleArray.setItemName(subTitle);
	       		titleArray.setUrl(shit);
	       		titleArray.setIsForum(true);
	          	results.add(titleArray);
	          			
      			String[] bits = shit.replace("http://", "").split("/");
	          	ident = bits[bits.length-1];
	           	String[] bits2 = ident.split("-");
	           	ident = new String(bits2[0]);
	           	titleArray.setIdent(ident);
	           	identList.add(ident);
	       	}
      		for (Element author : authors) {
      			if (author.text().startsWith("Started")) {      				
      				authorArray.add(author.text());
      			}
      		}
       		for (Element thread : threads) {
       			typeList.add("thread");
       			titleArray =  new TitleResults();
       			titleArray.setAuthorDate(authorArray.get(0));
       			authorArray.remove(0);
       			
       			//Thread title
       			threadTitle = thread.text();
       			titleArray.setItemName(threadTitle);
       			titlesArray.add(threadTitle);
       			TITLES.add(threadTitle);
       			//Thread link
       			String threadStr = new String(thread.attr("abs:href"));
       			String endTag = new String("/page__view__getnewpost");
       			threadStr = new String(threadStr.replace(endTag, ""));
       			titleArray.setUrl(threadStr);
       			entriesArray.add(threadStr);
       			threadArray.add(threadStr);
       			String[] bits = threadStr.split("/");
       			ident = bits[bits.length-1];
       			String[] bits2 = ident.split("-");
       			ident = new String(bits2[0]);
       			titleArray.setIdent(ident);
       			identList.add(ident);
       			results.add(titleArray);
       		}
 			return results;
		}
			
		protected void onPreExecute(){
					results.clear();
		        	pB.setVisibility(View.VISIBLE);
				}
		
        @Override
        protected void onPostExecute(ArrayList<TitleResults> results) {
        	if (results.size() == 0) {
        		makeToast("no results", RWSubForum.this);
        	}
        	pB.setVisibility(View.GONE); 
        	listView.setVisibility(View.VISIBLE);
        	listView.setAdapter(new RWSubAdapter(RWSubForum.this, results, "rw"));
            
     		Intent intent = new Intent(ctx, Receiver.class);
    		PendingIntent pendingIntent = PendingIntent.getBroadcast(
    				ctx.getApplicationContext(), 234324243, intent, 0);
    		AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
    		alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
    				+ 0, pendingIntent);
        }
    }
    
	public void makeToast(String message, Context ctx) {
		Toast.makeText(ctx, message, Toast.LENGTH_LONG).show();
	}
	
	@Override
	public void onResume() {
        listView.setVisibility(View.INVISIBLE);
    	new CreateArrayListTask().execute(url);
        super.onResume();
	}
 
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuinflate = new MenuInflater(this);
		menuinflate.inflate(R.menu.forum_menu, menu);
		return true;
	}	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case android.R.id.home:
	            Intent intent = new Intent(this, MainActivity.class);
	            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	            startActivity(intent);
	        break;
	        case R.id.restart:
	        	new CreateArrayListTask().execute(url);
	            listView.onRefreshComplete();
	        break;
	        case R.id.settings:
	            Intent si = new Intent(this, SettingsMenu.class);
	            si.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	            startActivity(si);
	        break;
	    default:
	            return super.onOptionsItemSelected(item);
	    }
		return false;
	}
}

