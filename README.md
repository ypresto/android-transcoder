# Transcoder

This project is an improved fork of [ypresto/android-transcoder](https://github.com/ypresto/android-transcoder).
Lots of changes were made, so the documentation must be rewritten. You can, however, take a look at the
demo app which provides a working example of the new API.

```groovy
implementation 'com.otaliastudios:transcoder:0.1.0'
```

## Setup

This library requires API level 18 (Android 4.3, JELLY_BEAN_MR2) or later.
If your app targets older versions, you can override the minSdkVersion by
adding this line to your manifest file:

```xml
<uses-sdk tools:overrideLibrary="com.otaliastudios.transcoder" />
```

In this case you should check at runtime that API level is at least 18, before
calling any method here.

## License

```
Copyright (C) 2014-2016 Yuya Tanaka

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
