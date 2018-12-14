/*
 * Copyright (C) 2014 Yuya Tanaka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.ypresto.androidtranscoder.strategy;

import androidx.annotation.Nullable;

/**
 * Base class for exceptions thrown by {@link OutputStrategy}.
 */
@SuppressWarnings("WeakerAccess")
public class OutputStrategyException extends RuntimeException {

    public final static int TYPE_UNAVAILABLE = 0;
    public final static int TYPE_ALREADY_COMPRESSED = 1;

    private int type;

    public OutputStrategyException(int type, String detailMessage) {
        super(detailMessage);
        this.type = type;
    }

    public OutputStrategyException(int type, Exception cause) {
        super(cause);
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public static OutputStrategyException unavailable(@Nullable Exception cause) {
        return new OutputStrategyException(TYPE_UNAVAILABLE, cause);
    }

    public static OutputStrategyException alreadyCompressed(@Nullable String detailMessage) {
        return new OutputStrategyException(TYPE_ALREADY_COMPRESSED, detailMessage);
    }
}
