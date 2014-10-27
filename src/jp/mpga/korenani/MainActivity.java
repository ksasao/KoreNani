/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.mpga.korenani;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

// Need the following import to get access to the app resources, since this
// class is in a sub-package.
//import com.example.android.apis.R;

// ----------------------------------------------------------------------

public class MainActivity extends Activity {
    private Preview mPreview;
    Camera mCamera;
    int numberOfCameras;
    int cameraCurrentlyLocked;
    TextView textView;
    
	private Timer mainTimer;					//タイマー用
	private MainTimerTask mainTimerTask;		//タイマタスククラス
	private Handler mHandler = new Handler();   //UI Threadへのpost用ハンドラ

    // The first rear facing camera
    int defaultCameraId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Full Screen for BT-200
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        winParams.flags |= 0x80000000;
        win.setAttributes(winParams);

        // Create a RelativeLayout container that will hold a SurfaceView,
        // and set it as the content of our activity.
        

        // Set Camera View
        mPreview = new Preview(this);
        setContentView(mPreview);
        
        // Set Message View
        textView = new TextView(this);
        textView.setText("認識中...");
        textView.setTextSize(30.0f);
        
        textView.setGravity(Gravity.LEFT);
        textView.setBackgroundColor(Color.argb(128, 0, 0, 0));
        textView.setTextColor(Color.WHITE);
        
  
        int MP = ViewGroup.LayoutParams.MATCH_PARENT;
        int WC = ViewGroup.LayoutParams.WRAP_CONTENT;
        RelativeLayout relativeLayout = new RelativeLayout(this);
        RelativeLayout.LayoutParams param = new RelativeLayout.LayoutParams(WC, WC);
        param.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        relativeLayout.addView(textView, param);
        
        addContentView(relativeLayout, new LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.FILL_PARENT));



        mPreview.setOnTouchListener(
        		new OnTouchListener(){
					@Override
					public boolean onTouch(View v, MotionEvent event) {
						mPreview.takePreviewData();
						textView.setText(mPreview.message);
						return true;
					}
        		}
        );
        
		this.mainTimer = new Timer();
		this.mainTimerTask = new MainTimerTask();				
		this.mainTimer.schedule(mainTimerTask,5000, 5000);        

        // Find the total number of cameras available
        numberOfCameras = Camera.getNumberOfCameras();

        // Find the ID of the default camera
        CameraInfo cameraInfo = new CameraInfo();
            for (int i = 0; i < numberOfCameras; i++) {
                Camera.getCameraInfo(i, cameraInfo);
                if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
                    defaultCameraId = i;
                }
            }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Open the default i.e. the first rear facing camera.
        mCamera = Camera.open();
        cameraCurrentlyLocked = defaultCameraId;
        mPreview.setCamera(mCamera);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Because the Camera object is a shared resource, it's very
        // important to release it when the activity is paused.
        if (mCamera != null) {
            mPreview.setCamera(null);
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate our menu which can gather user input for switching camera
        MenuInflater inflater = getMenuInflater();
        //inflater.inflate(R.menu.camera_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    /**
     * タイマータスク派生クラス
     * run()に定周期で処理したい内容を記述
     * 
     */
    class MainTimerTask extends TimerTask {
    	@Override
    	public void run() {
             mHandler.post( new Runnable() {
                 public void run() {
						mPreview.takePreviewData();
						textView.setText(mPreview.message);
                 }
             });
    	}
    }
}



// ----------------------------------------------------------------------

/**
 * A simple wrapper around a Camera and a SurfaceView that renders a centered preview of the Camera
 * to the surface. We need to center the SurfaceView because not all devices have cameras that
 * support preview sizes at the same aspect ratio as the device's display.
 */
class Preview extends ViewGroup  implements SurfaceHolder.Callback {
    private final String TAG = "Preview";
    private boolean mProgressFlag = false;
    public String message = "";
    public String rev = "";
    public String oldItemName ="";

    SurfaceView mSurfaceView;
    SurfaceHolder mHolder;
    Size mPreviewSize;
    List<Size> mSupportedPreviewSizes;
    Camera mCamera;
    Context currentContext;

    Preview(Context context) {
        super(context);

        currentContext = context;
        mSurfaceView = new SurfaceView(context);
        addView(mSurfaceView);

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }
    
    

    public void setCamera(Camera camera) {
        mCamera = camera;
        if (mCamera != null) {
            mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
            requestLayout();
        }
    }

    public void switchCamera(Camera camera) {
       setCamera(camera);
       try {
           camera.setPreviewDisplay(mHolder);
       } catch (IOException exception) {
           Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
       }
       Camera.Parameters parameters = camera.getParameters();
       parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
       requestLayout();

       camera.setParameters(parameters);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // We purposely disregard child measurements because act as a
        // wrapper to a SurfaceView that centers the camera preview instead
        // of stretching it.
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        setMeasuredDimension(width, height);

        if (mSupportedPreviewSizes != null) {
            mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed && getChildCount() > 0) {
            final View child = getChildAt(0);

            final int width = r - l;
            final int height = b - t;

            int previewWidth = width;
            int previewHeight = height;
            if (mPreviewSize != null) {
                previewWidth = mPreviewSize.width;
                previewHeight = mPreviewSize.height;
            }
            this.setBackgroundColor(Color.BLACK);
        	//child.setBackgroundColor(Color.BLACK);
            
            // Center the child SurfaceView within the parent.
            if (width * previewHeight > height * previewWidth) {
                final int scaledChildWidth = previewWidth * height / previewHeight;
                child.layout((width - scaledChildWidth) / 2, 0,
                        (width + scaledChildWidth) / 2, height);
            } else {
                final int scaledChildHeight = previewHeight * width / previewWidth;
                child.layout(0, (height - scaledChildHeight) / 2,
                        width, (height + scaledChildHeight) / 2);
            }
        }
    }
    
    public void takePreviewData() {
        if (!mProgressFlag) {
            mProgressFlag = true;
            mCamera.setPreviewCallback(editPreviewImage);
        }
    }

    private final Camera.PreviewCallback editPreviewImage =
            new Camera.PreviewCallback() {
     
        public void onPreviewFrame(byte[] data, Camera camera) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
     
            // JPEG に変換
            int width = camera.getParameters().getPreviewSize().width;
            int height = camera.getParameters().getPreviewSize().height;
            YuvImage yuvimage = new YuvImage(data, ImageFormat.NV21, width, height, null);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            yuvimage.compressToJpeg(new Rect(0, 0, width, height), 80, baos);
            byte[] jdata = baos.toByteArray();
            
            // docomo サーバに問い合わせ
            AsyncHttpPostBinary asyncPost = new AsyncHttpPostBinary(new AsyncCallback<String>() {
                public void onPreExecute() {
                    // do something
                }
                public void onProgressUpdate(int progress) {
                    // do something
                }
                public void onPostExecute(String result) {
                	Log.v("DOCOMO", result);
                	String parsed = "";
                	try {
						JSONObject json = new JSONObject(result);
						JSONArray items = json.getJSONArray("candidates");
						if(items!=null && items.length()!=0){
							JSONObject detail = items.getJSONObject(0).getJSONObject("detail");
							if(detail.has("itemName")){
								String itemName = detail.getString("itemName");
								parsed += itemName + "\r\n";
								if(!itemName.equals(oldItemName)){
									oldItemName = itemName;
									rev = "";
								}
							}
							if(detail.has("releaseDate")){
								String releaseDate = detail.getString("releaseDate");
								parsed += "発売日: " + releaseDate + "\r\n";
							}
							JSONArray sites = items.getJSONObject(0).getJSONArray("sites");
							String amazonUrl = "";
							for(int i=0; i<sites.length(); i++){
								String url = sites.getJSONObject(i).getString("url");
								if(url.startsWith("http://www.amazon.co.jp")){
									amazonUrl = url;
									break;
								}
							}
							if(amazonUrl.length() > 0){

								AsyncHttpGet amazon = new AsyncHttpGet(new AsyncCallback<String>(){

									@Override
									public void onPreExecute() {
										// TODO Auto-generated method stub
										
									}

									@Override
									public void onPostExecute(String result) {
										int review = result.indexOf("投稿者");
										if(review > 0){
											String message = result.substring(review);
											int start = message.indexOf("drkgry")+11;
											int end = message.indexOf("/div",start) - 3;
											rev = message.substring(start,end).replaceAll("<br />", "");
											Log.v("Amazon", "start: " + start + "/end: " + end + " " + rev);
										}
									}

									@Override
									public void onProgressUpdate(int progress) {
										// TODO Auto-generated method stub
										
									}

									@Override
									public void onCancelled() {
										// TODO Auto-generated method stub
										
									}
									
								});
								amazon.execute(amazonUrl);
							}
							message = parsed + rev;
							
						}
					} catch (JSONException e) {
						Log.e("DOCOMO", e.getMessage());
					}
                }
                public void onCancelled() {
                    // do something
                }
            });
            
            String apiKey = ""; // TODO Set Your API Key
            String url = "https://api.apigw.smt.docomo.ne.jp/imageRecognition/v1/recognize?APIKEY=" 
            + apiKey + "&recog=product-all&numOfCandidates=1";
            asyncPost.execute(url, jdata);
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS");
     
            // ファイル保存
            String basePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
            String filename = dateFormat.format(new Date()) + ".jpg";
            String fullpath = basePath + '/' + filename;
     
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(fullpath);
                fos.write(jdata);
                fos.close();
                
                // 他のアプリから直ちに参照できるようにする
                ContentValues values = new ContentValues();
                ContentResolver contentResolver = currentContext.getContentResolver();
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                values.put(MediaStore.Images.Media.TITLE, filename); 
                values.put("_data", fullpath);
                contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            } catch (Exception e) {
                Log.e("CAMERA", e.getMessage());
            }
            Log.v("CAMERA", "Save as " + fullpath + " " + jdata.length + " bytes.");
            mCamera.startPreview();
     
            mProgressFlag = false;
        }
    };

    
    
    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, acquire the camera and tell it where
        // to draw.
        try {
            if (mCamera != null) {
                mCamera.setPreviewDisplay(holder);
            }
        } catch (IOException exception) {
            Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop the preview.
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }


    private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // Now that the size is known, set up the camera parameters and begin
        // the preview.
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
        requestLayout();
        
        mCamera.setParameters(parameters);
        mCamera.startPreview();
    }

}
