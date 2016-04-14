package net.ypresto.androidtranscoder.example;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import net.ypresto.androidtranscoder.engine.MediaTranscoderEngine;
import net.ypresto.androidtranscoder.format.Android720pFormatStrategy;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;


public class TranscoderActivity extends Activity {
    private static final String TAG = "TranscoderActivity";
    private static final int REQUEST_CODE_PICK = 1;
    private static final int PROGRESS_BAR_MAX = 1000;
    private final Handler handler = new Handler();
    private Thread thread;

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
        findViewById(R.id.cancel_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (thread != null) {
                    thread.interrupt();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_PICK: {
                if (resultCode == RESULT_OK) {

                    transcode(data);
                }
                break;
            }
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void transcode(final Intent data) {
        switchButtonEnabled(true);

        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final File file = File.createTempFile("transcode_test", ".mp4", getExternalFilesDir(null));
                    final ContentResolver resolver = getContentResolver();
                    final ParcelFileDescriptor parcelFileDescriptor = resolver.openFileDescriptor(data.getData(), "r");
                    if (parcelFileDescriptor == null) {
                        throw new IOException("Could not read parcel file");
                    }
                    try {
                        final FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                        final MediaTranscoderEngine engine = new MediaTranscoderEngine();
                        engine.setProgressCallback(new MediaTranscoderEngine.ProgressCallback() {
                            @Override
                            public void onProgress(final double progress) {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
                                        if (progress < 0) {
                                            progressBar.setIndeterminate(true);
                                        } else {
                                            progressBar.setIndeterminate(false);
                                            progressBar.setProgress((int) Math.round(progress * PROGRESS_BAR_MAX));
                                        }
                                    }
                                });
                            }
                        });
                        engine.setDataSource(fileDescriptor);
                        engine.transcodeVideo(file.getAbsolutePath(), new Android720pFormatStrategy(Android720pFormatStrategy.DEFAULT_BITRATE / 4, 480, 340));
                    } finally {
                        parcelFileDescriptor.close();
                    }

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            onTranscodeFinished(true, "Transcoded video");
                            startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.fromFile(file), "video/mp4"));
                        }
                    });

                } catch (IOException e) {
                    showError(e);
                } catch (InterruptedException e) {
                    showError(e);
                } catch (RuntimeException e) {
                    showError(e);
                }
            }

            private void showError(final Throwable e) {
                Log.e(TAG, "Failed to transcode video.", e);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        onTranscodeFinished(false, "Faild transcode" + e.getMessage());
                    }
                });
            }
        });
        thread.start();
    }

    private void onTranscodeFinished(boolean isSuccess, String toastMessage) {
        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        progressBar.setIndeterminate(false);
        progressBar.setProgress(isSuccess ? PROGRESS_BAR_MAX : 0);
        switchButtonEnabled(false);
        Toast.makeText(TranscoderActivity.this, toastMessage, Toast.LENGTH_LONG).show();
    }

    private void switchButtonEnabled(boolean isProgress) {
        findViewById(R.id.select_video_button).setEnabled(!isProgress);
        findViewById(R.id.cancel_button).setEnabled(isProgress);
    }
}
