package jp.mpga.korenani;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;


import android.os.AsyncTask;

public class AsyncHttpPostBinary extends AsyncTask<Object, Integer, String> {
	private AsyncCallback<String> _asyncCallback;

	/**
	 * 非同期に バイナリデータを octet-stream で HTTP POST します
	 * @param activity 呼び出し元 Activity
	 */
	public AsyncHttpPostBinary(AsyncCallback<String> asyncCallback) {
		this._asyncCallback = asyncCallback;
	}

	/**
	 * バックグラウンド処理。第一引数には URL(String)、第二引数には送信するバイト列(byte[])
	 */
	@Override
	protected String doInBackground(Object... args) {
		String result = "";
		HttpURLConnection http = null;  // HTTP通信
		OutputStream out = null;   // HTTPリクエスト送信用ストリーム
		InputStream in = null;    // HTTPレスポンス取得用ストリーム
		BufferedReader reader = null;  // レスポンスデータ出力用バッファ


		byte[] postData = (byte[]) args[1];

		try {
			// URL指定
			URL url = new URL((String) args[0]);

			// HttpURLConnectionインスタンス作成
			http = (HttpURLConnection)url.openConnection();

			// POST設定
			http.setRequestMethod("POST");

			// HTTPヘッダの「Content-Type」を「application/octet-stream」に設定
			http.setRequestProperty("Content-Type","application/octet-stream");

			// URL 接続を使用して入出力を行う
			http.setDoInput(true);
			http.setDoOutput(true);

			// キャッシュは使用しない
			http.setUseCaches(false);

			// 接続
			http.connect();

			// データを出力
			out = new BufferedOutputStream(http.getOutputStream());
			out.write(postData);
			out.flush();

			// レスポンスを取得
			in = new BufferedInputStream(http.getInputStream());
			reader = new BufferedReader(new InputStreamReader(in));
			result = reader.readLine();			 

		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if(reader != null) {
					reader.close();
				}
				if(in != null) {
					in.close();
				}
				if(out != null) {
					out.close();
				}
				if(http != null) {
					http.disconnect();
				}
			} catch(Exception e) {
			}
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
