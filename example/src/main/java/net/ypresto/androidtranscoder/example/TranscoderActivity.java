package net.ypresto.androidtranscoder.example;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import net.ypresto.androidtranscoder.MediaTranscoder;
import net.ypresto.androidtranscoder.format.MediaFormatStrategyPresets;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;


public class TranscoderActivity extends Activity {
    private static final String TAG = "TranscoderActivity";
    private static final int REQUEST_CODE_PICK = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transcoder);
        findViewById(R.id.select_video_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(Intent.ACTION_GET_CONTENT).setType("video/*"), REQUEST_CODE_PICK);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_PICK: {
                final File file;
                if (resultCode == RESULT_OK) {
                    try {
                        file = File.createTempFile("transcode_test", ".mp4", getExternalFilesDir(null));
                    } catch (IOException e) {
                        Toast.makeText(this, "Failed to create temporary file.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    ContentResolver resolver = getContentResolver();
                    final ParcelFileDescriptor parcelFileDescriptor;
                    try {
                        parcelFileDescriptor = resolver.openFileDescriptor(data.getData(), "r");
                    } catch (FileNotFoundException e) {
                        Log.w("Could not open '" + data.getDataString() + "'", e);
                        Toast.makeText(TranscoderActivity.this, "File not found.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    final FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                    final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
                    progressBar.setMax(1000);
                    final long startTime = SystemClock.uptimeMillis();
                    MediaTranscoder.Listener listener = new MediaTranscoder.Listener() {
                        @Override
                        public void onTranscodeProgress(double progress) {
                            if (progress < 0) {
                                progressBar.setIndeterminate(true);
                            } else {
                                progressBar.setIndeterminate(false);
                                progressBar.setProgress((int) Math.round(progress * 1000));
                            }
                        }

                        @Override
                        public void onTranscodeCompleted() {
                            Log.d(TAG, "transcoding took " + (SystemClock.uptimeMillis() - startTime) + "ms");
                            Toast.makeText(TranscoderActivity.this, "transcoded file placed on " + file, Toast.LENGTH_LONG).show();
                            findViewById(R.id.select_video_button).setEnabled(true);
                            progressBar.setIndeterminate(false);
                            progressBar.setProgress(1000);
                            startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.fromFile(file), "video/mp4"));
                            try {
                                parcelFileDescriptor.close();
                            } catch (IOException e) {
                                Log.w("Error while closing", e);
                            }
                        }

                        @Override
                        public void onTranscodeFailed(Exception exception) {
                            progressBar.setIndeterminate(false);
                            progressBar.setProgress(0);
                            findViewById(R.id.select_video_button).setEnabled(true);
                            Toast.makeText(TranscoderActivity.this, "Transcoder error occurred.", Toast.LENGTH_LONG).show();
                            try {
                                parcelFileDescriptor.close();
                            } catch (IOException e) {
                                Log.w("Error while closing", e);
                            }
                        }
                    };
                    Log.d(TAG, "transcoding into " + file);
                    MediaTranscoder.getInstance().transcodeVideo(fileDescriptor, file.getAbsolutePath(),
                            MediaFormatStrategyPresets.createAndroid720pStrategy(), listener);
                    findViewById(R.id.select_video_button).setEnabled(false);
                }
                break;
            }
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.transcoder, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
