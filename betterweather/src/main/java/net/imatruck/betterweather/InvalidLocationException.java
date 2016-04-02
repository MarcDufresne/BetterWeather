/*
 * Copyright 2013-2016 Marc-André Dufresne
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
package net.imatruck.betterweather;

/**
 * Invalid location exception class
 */
public class InvalidLocationException extends Exception {
    private static final long serialVersionUID = 1L;

    public InvalidLocationException() {
    }

    public InvalidLocationException(String detailMessage) {
        super(detailMessage);
    }

    public InvalidLocationException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public InvalidLocationException(Throwable throwable) {
        super(throwable);
    }
}
