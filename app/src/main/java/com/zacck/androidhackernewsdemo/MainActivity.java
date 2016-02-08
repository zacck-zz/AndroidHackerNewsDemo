package com.zacck.androidhackernewsdemo;

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //setup Ui
        ListView mArticlesListView = (ListView)findViewById(R.id.articlesList);
        mAdp = new ArrayAdapter(this, android.R.layout.simple_list_item_1, mTitles);
        mArticlesListView.setAdapter(mAdp);
        mArticleDb = this.openOrCreateDatabase("ArticlesDB", MODE_PRIVATE, null);

        mArticleDb.execSQL("CREATE TABLE IF NOT EXISTS articles(id INTEGER PRIMARY KEY, articleId INTEGER, url VARCHAR, title VARCHAR, content VARCHAR)");

        //run the download task
        DownlodNewsTask mTask = new DownlodNewsTask();
        try
        {
            String mData = mTask.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty").get();
            //Log.i("Result From Server is: ", mData);
            JSONArray DataArray = new JSONArray(mData);

            //emptyDb to prevent Duplication while testing
            mArticleDb.execSQL("DELETE FROM articles");

            for(int i = 0; i<30; i++)
            {
                String ArticleId = DataArray.getString(i);
                DownlodNewsTask getArticle = new DownlodNewsTask();
                String ArticleInfo = getArticle.execute("https://hacker-news.firebaseio.com/v0/item/"+ArticleId+".json?print=pretty").get();
                JSONObject mObject = new JSONObject(ArticleInfo);
                String ArticleTitle = mObject.getString("title");
                String ArticleUrl = mObject.getString("url");
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

                mStatement.execute();

            }

            //check db writes worked
            Cursor mArticleCursor = mArticleDb.rawQuery("SELECT * FROM articles ORDER BY articleId DESC", null);

            int articleIdIndex = mArticleCursor.getColumnIndex("articleId");
            int urlIndex = mArticleCursor.getColumnIndex("url");
            int titleIndex = mArticleCursor.getColumnIndex("title");
            //iterate cursor
            mArticleCursor.moveToFirst();
            while (mArticleCursor != null )
            {
                mTitles.add(mArticleCursor.getString(titleIndex));
                Log.i("articleResults", Integer.toString(mArticleCursor.getInt(articleIdIndex)));
                Log.i("articleResults", mArticleCursor.getString(urlIndex));
                Log.i("articleResults", mArticleCursor.getString(titleIndex));
                mArticleCursor.moveToNext();
            }
            mAdp.notifyDataSetChanged();




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

    //get the data from the web
    public class DownlodNewsTask extends AsyncTask<String, Void, String>
    {

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

            }
            catch (Exception e)
            {
                Log.i(getPackageName(), "This exception occurred during download: "+e.toString());

            }
            return result;
        }
    }
}
