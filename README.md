android-transcoder
=================

Hardware accelerated transcoder for Android, written in pure Java.

## Usage

```
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    ParcelFileDescriptor parcelFileDescriptor = resolver.openFileDescriptor(data.getData(), "r");
    FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
    MediaTranscoder.getInstance().transcodeVideo(fileDescriptor, file.getAbsolutePath(), new MediaTranscoder.Listener() {
        @Override
        public void onTranscodeProgress(double progress) {
            ...
        }

        @Override
        public void onTranscodeCompleted() {
            startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.fromFile(file), "video/mp4"));
            ...
        }

        @Override
        public void onTranscodeFailed(Exception exception) {
            ...
        }
    }
}
```

See `TranscoderActivity.java` in example directory for ready-made transcoder app.

## License

```
Copyright (C) 2014 Yuya Tanaka

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
