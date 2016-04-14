android-transcoder
=================

Hardware accelerated transcoder for Android, written in pure Java.

[![Build Status](https://travis-ci.org/jacek-marchwicki/android-transcoder.svg?branch=master)](https://travis-ci.org/jacek-marchwicki/android-transcoder)

## Requirements

API Level 18 (Android 4.3, JELLY_BEAN_MR2) or later.
If your app targets older Android, you should add below line to AndroidManifest.xml:

```
<!-- Only supports API >= 18 -->
<uses-sdk tools:overrideLibrary="net.ypresto.androidtranscoder" />
```

Please ensure checking Build.VERSION by your self.

## Usage

```java
final FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
final MediaTranscoderEngine engine = new MediaTranscoderEngine();
engine.setProgressCallback(new MediaTranscoderEngine.ProgressCallback() {
    @Override
    public void onProgress(final double progress) {
    }
});
engine.setDataSource(fileDescriptor);
engine.transcodeVideo(file.getAbsolutePath(), new Android720pFormatStrategy(Android720pFormatStrategy.DEFAULT_BITRATE, 480, 340));
```

See `TranscoderActivity.java` in example directory for ready-made transcoder app.

## Quick Setup

### Gradle

```groovy
repositories {
    maven { url "https://jitpack.io" }
}

dependencies {

    // snapshot version
    compile 'com.github.jacek-marchwicki.android-transcoder:master-SNAPSHOT'

    // or use specific version (look on releases tab)
    compile 'com.github.jacek-marchwicki.android-transcoder:0.1.11'
}
```

## Build instructions

for library and example

```bash
./gradlew build
```

## Note (PLEASE READ FIRST)

- This library raises `RuntimeException`s (like `IlleagalStateException`) in various situations. Please catch it and provide alternate logics. I know this is bad design according to Effective Java; just is TODO.
- Currently this library does not generate streaming-aware mp4 file.
Use [qtfaststart-java](https://github.com/ypresto/qtfaststart-java) to place moov atom at beginning of file.
- Android does not gurantees that all devices have bug-free codecs/accelerators for your codec parameters (especially, resolution). Refer [supported media formats](http://developer.android.com/guide/appendix/media-formats.html) for parameters guaranteed by [CTS](https://source.android.com/compatibility/cts-intro.html).
- This library does not support video files recorded by other device like digital cameras, iOS (mov files, including non-baseline profile h.264), etc.

## License

```
Copyright (C) 2014-2015 Yuya Tanaka
Copyright (C) 2016 Jacek Marchwicki <jacek.marchwicki@gmail.com>

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

## References for Android Low-Level Media APIs

- http://bigflake.com/mediacodec/
- https://github.com/google/grafika
- https://android.googlesource.com/platform/frameworks/av/+/lollipop-release/media/libstagefright
