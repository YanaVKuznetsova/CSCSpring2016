package family.kuziki.yaBR;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.TextHttpResponseHandler;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.concurrent.ExecutionException;

import static android.widget.Toast.LENGTH_LONG;

public class Translator {

    private static final String urlTranslate = "https://translate.yandex.net/api/v1.5/tr.json/translate?&lang=ru&format=plain&text=";
    private static final String apiKey = "&key=trnsl.1.1.20160425T151703Z.4adf02246a2895da.3ea89905d8057da0427aaef1a2eb984bb2cd5a6b";

    // Singleton
    private Translator(){
    }

    public static Translator getInstance(){
        return SingletonHolder.instance;
    }

    private static class SingletonHolder {
        public static Translator instance = new Translator();
    }

    public String translate(String word, Database database) throws InterruptedException {
        SQLiteDatabase db = database.getWritableDatabase();
        // check the word
        Cursor cursor = db.query("mytable", null, null, null, null, null, null);
        String translation = null;
        if(cursor.moveToFirst()) {
            int wordColIndex = cursor.getColumnIndex("word");
            int translationColIndex = cursor.getColumnIndex("translation");
            do {
                String possibleWord = cursor.getString(wordColIndex);
                if (possibleWord.equals(word)) {
                    translation = cursor.getString(translationColIndex);
                    Log.d("Translator_Database", "word is found!");
                }
            } while (cursor.moveToNext());
        }
        if (translation != null) {
            db.close();
            return translation;
        }
        // if DB doesn't contain the word, add it to the DB
        RequestResponse request = null;
        try {
            request = sendRequest(word);
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String response = parseReceivedRequest(request);
        Log.d("Translator", response);
        ContentValues contentValues = new ContentValues();
        contentValues.put("word", word);
        contentValues.put("translation", response);
        long rowID = db.insert("mytable", null, contentValues);
        Log.d("Translator_Database", String.valueOf(rowID));
        db.close();
        return response;
    }

    private String parseReceivedRequest(RequestResponse request) {
        try {
            return request.getResponse().getString("text").toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return "Translation failed";
        }
    }

    private RequestResponse sendRequest(String searchString) throws ExecutionException, InterruptedException, JSONException {
        // Prepare search string to be put in a URL
        String urlString = "";
        try {
            urlString = URLEncoder.encode(searchString, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        String s = urlTranslate + urlString + apiKey;
        Log.d("Translator" , "URL :" +s);
        final RequestResponse response = new RequestResponse();

        response.setResponse(new GetJSONTask().execute(s).get());
        response.setIsDone();

        return response;
    }
}
