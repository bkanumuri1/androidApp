package com.example.assignment2_g11;


import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.security.cert.CertificateException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.io.File;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Toast;
import android.widget.VideoView;

import com.android.volley.RequestQueue;

import java.io.IOException;

import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private static int CAMERA_PERMISSION_CODE = 100;
    private static int VIDEO_RECORD_CODE = 101;
    private Uri videoPath;



    public Button button;
    MediaRecorder recorder;
    RequestQueue requestQueue;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        setContentView(R.layout.activity_main);
        if (isCameraPresentInPhone()) {
            Log.i("VIDEO_RECORD_TAG", "Camera is detected");
            getCameraPermission();
        } else {
            Log.i("VIDEO_RECORD_TAG", "No Camera is detected");
        }

    }

    public void recordButtonPressed(View view) {
        recordVideo();
    }

    private boolean isCameraPresentInPhone() {
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            return true;
        } else {
            return false;
        }
    }

    private void getCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]
                    {
                            Manifest.permission.CAMERA,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },
                    CAMERA_PERMISSION_CODE
            );
        }

    }

    private void recordVideo() {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 5);
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
        intent.putExtra("android.intent.extras.CAMERA_FACING", 1);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, VIDEO_RECORD_CODE);
        }
    }

    public String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return "";
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == VIDEO_RECORD_CODE) {
            if (resultCode == RESULT_OK) {
                videoPath = data.getData();

                if (videoPath != null) {
                    String uri = getRealPathFromURI(this, videoPath);
                    Log.i("FILE_NAME, ", uri);

                    postRequest(uri);
                }
            } else if (resultCode == RESULT_CANCELED) {
                Log.i("VIDEO_RECORD_TAG", "Recorded video is Cancelled");
            } else {
                Log.i("VIDEO_RECORD_TAG", "Recorded video has some error");
            }
        }
    }


    public void postRequest(String videoPath) {
        String postUrl = "https://1f67-72-216-240-118.ngrok.io/upload";
        try {
            OkHttpClient client = new OkHttpClient()
                    .newBuilder()
                    .callTimeout(60, TimeUnit.SECONDS)
                    .build();

            File file = new File(videoPath);
            RequestBody rb = RequestBody.create(MediaType.parse("multipart/form-data"), file);

            RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("videoname", videoPath, rb)
                    .addFormDataPart("username", "abc")
                    .build();

            Request request = new Request.Builder()
                    .url(postUrl)
                    .post(body)
                    .build();

//            Response response = client.newCall(request).execute();
//            response.close();


            Log.i("UPLOADING_VIDEO", postUrl);

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(okhttp3.Call call, IOException e) {
                    call.cancel();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Failed to connect to server", Toast.LENGTH_SHORT).show();
                        }
                    });

                    Log.e("HttpService", "onFailure() Request was:" + request);
                    e.printStackTrace();
                }

                @Override
                public void onResponse(okhttp3.Call call, Response response) throws IOException {
                    Log.i("response", "onResponse(): " + response);
                    response.close();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Video Uploaded to server", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
                }
            });
        } catch (Exception e) {
            Log.i("IOException", e.toString());
        }
    }
}