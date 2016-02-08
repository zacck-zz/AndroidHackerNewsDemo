package com.zacck.androidhackernewsdemo;

import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    Map<Integer, String> articleUrls = new HashMap<Integer, String>();
    Map<Integer, String> articleTitles = new HashMap<Integer, String>();
    ArrayList<Integer> ArticleIds = new ArrayList<Integer>();

    ArrayList<String> mTitles = new ArrayList<String>();
    ArrayAdapter mAdp;

    //get a Database
    SQLiteDatabase mArticleDb;

    //urls arraylist
    ArrayList<String> mUrls = new ArrayList<String>();
    //content arraylist
    ArrayList<String> mContent = new ArrayList<String>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //setup Ui
        ListView mArticlesListView = (ListView)findViewById(R.id.articlesList);
        mAdp = new ArrayAdapter(this, android.R.layout.simple_list_item_1, mTitles);

        //add a click listener
        mArticlesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Intent webViewIntent = new Intent(getApplicationContext(), WebViewActivity.class);
                webViewIntent.putExtra("articleUrl", mUrls.get(position));
                webViewIntent.putExtra("content", mContent.get(position));
                startActivity(webViewIntent);
            }
        });

        mArticlesListView.setAdapter(mAdp);
        mArticleDb = this.openOrCreateDatabase("ArticlesDB", MODE_PRIVATE, null);

        mArticleDb.execSQL("CREATE TABLE IF NOT EXISTS articles(id INTEGER PRIMARY KEY, articleId INTEGER, url VARCHAR, title VARCHAR, content VARCHAR)");

        //update the listfrom local Db
        updateListView();

        //run the download task
        DownlodNewsTask mTask = new DownlodNewsTask();
        try
        {
            mTask.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");

        }
        catch (Exception e)
        {
            Log.i("MainException", e.toString());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int mItemId = item.getItemId();

        if( mItemId ==R.id.action_settings)
        {
            return  true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void updateListView()
    {
        try {
            //check db writes worked
            Cursor mArticleCursor = mArticleDb.rawQuery("SELECT * FROM articles ORDER BY articleId DESC", null);

            int contentIndex = mArticleCursor.getColumnIndex("content");
            int urlIndex = mArticleCursor.getColumnIndex("url");
            int titleIndex = mArticleCursor.getColumnIndex("title");
            //iterate cursor
            mArticleCursor.moveToFirst();
            mUrls.clear();
            mTitles.clear();
            while (mArticleCursor != null) {
                mTitles.add(mArticleCursor.getString(titleIndex));
                mUrls.add(mArticleCursor.getString(urlIndex));
                mContent.add(mArticleCursor.getString(contentIndex));
                mArticleCursor.moveToNext();
            }
            mAdp.notifyDataSetChanged();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    //get the data from the web
    public class DownlodNewsTask extends AsyncTask<String, Void, String>
    {
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            updateListView();
        }

        @Override
        protected String doInBackground(String... urls) {
            String result ="";
            URL mUrl ;
            HttpURLConnection mUrlConnection = null;

            try
            {
                mUrl = new URL(urls[0]);
                mUrlConnection = (HttpURLConnection)mUrl.openConnection();

                InputStream mInputStream = mUrlConnection.getInputStream();

                InputStreamReader mReader = new InputStreamReader(mInputStream);

                int data = mReader.read();

                while (data != -1)
                {
                    char current =(char) data;
                    result += current;
                    data = mReader.read();
                }

                //Time Stuff
                //Log.i("Result From Server is: ", mData);
                JSONArray DataArray = new JSONArray(result);

                //emptyDb to prevent Duplication while testing
                mArticleDb.execSQL("DELETE FROM articles");

                for(int i = 0; i<30; i++)
                {
                    String ArticleId = DataArray.getString(i);
                    mUrl = new URL("https://hacker-news.firebaseio.com/v0/item/"+ArticleId+".json?print=pretty");
                    mUrlConnection = (HttpURLConnection)mUrl.openConnection();
                    mInputStream = mUrlConnection.getInputStream();
                    mReader = new InputStreamReader(mInputStream);

                    data = mReader.read();
                    String ArticleInfo ="";
                    while (data != -1)
                    {
                        char curr = (char) data;
                        ArticleInfo += curr;
                        data = mReader.read();
                    }

                    JSONObject mObject = new JSONObject(ArticleInfo);
                    String ArticleTitle = mObject.getString("title");
                    String ArticleUrl = mObject.getString("url");
                    /*content of url
                    mUrl = new URL(ArticleUrl);
                    mUrlConnection = (HttpURLConnection)mUrl.openConnection();
                    mInputStream = mUrlConnection.getInputStream();
                    mReader = new InputStreamReader(mInputStream);

                    data = mReader.read();
                    String ArticleContent ="";
                    while (data != -1)
                    {
                        char curr = (char) data;
                        ArticleInfo += curr;
                        data = mReader.read();
                    }
                    */





                    //save them using a map
                    ArticleIds.add(Integer.valueOf(ArticleId));
                    articleTitles.put(Integer.valueOf(ArticleId), ArticleTitle);
                    articleUrls.put(Integer.valueOf(ArticleId), ArticleUrl);

                    //lets insert to db
                    ///mArticleDb.execSQL("INSERT INTO articles(articleId, url, title) VALUES("+ DatabaseUtils.sqlEscapeString(ArticleId)+", "+DatabaseUtils.sqlEscapeString(ArticleUrl)+", "+DatabaseUtils.sqlEscapeString(ArticleTitle)+")");


                    //using prepared statements
                    String aSql = "INSERT INTO articles(articleId, url, title) VALUES(?, ?, ?)";
                    //convert the String to statement which is safer and better to use than rawSQL
                    SQLiteStatement mStatement = mArticleDb.compileStatement(aSql);

                    mStatement.bindString(1, ArticleId);
                    mStatement.bindString(2, ArticleUrl);
                    mStatement.bindString(3, ArticleTitle);
                    //mStatement.bindString(4, ArticleContent);

                    mStatement.execute();

                }

            }
            catch (Exception e)
            {
                Log.i(getPackageName(), "This exception occurred during download: "+e.toString());

            }
            return result;
        }
    }
}
