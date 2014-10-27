package jp.mpga.korenani;

public interface AsyncCallback<T> {
    public void onPreExecute();
    public void onPostExecute(T result);
    public void onProgressUpdate(int progress);
    public void onCancelled();
}
