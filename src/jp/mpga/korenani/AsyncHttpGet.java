package jp.mpga.korenani;
import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.os.AsyncTask;

public class AsyncHttpGet extends AsyncTask<String, Integer, String>{
    private static final String _defaultEncode = "UTF-8";
    private AsyncCallback<String> _asyncCallback;
    
    /**
     * 非同期にHTTP Get を行います
     * @param activity 呼び出し元 Activity
     */
    public AsyncHttpGet(AsyncCallback<String> asyncCallback) {
        this._asyncCallback = asyncCallback;
    }
    
    /**
     * バックグラウンド処理。第一引数には URL、第二引数にはエンコードを記述（省略時は"UTF-8"）
     */
	@Override
	protected String doInBackground(String... args) {
		String url = args[0];
		String encode = args.length > 1 ? args[1] : _defaultEncode;
		String result = "";
		
		HttpGet method = new HttpGet( url );
		DefaultHttpClient client = new DefaultHttpClient();
		method.setHeader( "Connection", "Keep-Alive" );
		HttpResponse response;
		try {
			response = client.execute( method );
			int status = response.getStatusLine().getStatusCode();
			if ( status == HttpStatus.SC_OK )
			{
			    result = EntityUtils.toString( response.getEntity(), encode );
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			client.getConnectionManager().shutdown();
		}
		return result;
	}
	
    protected void onPreExecute() {
        super.onPreExecute();
        this._asyncCallback.onPreExecute();
    }

    protected void onProgressUpdate(int progress) {
        super.onProgressUpdate(progress);
        this._asyncCallback.onProgressUpdate(progress);
    }

    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        this._asyncCallback.onPostExecute(result);
    }

    protected void onCancelled() {
        super.onCancelled();
        this._asyncCallback.onCancelled();
    }

}
