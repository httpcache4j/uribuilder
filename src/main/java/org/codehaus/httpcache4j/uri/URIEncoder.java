/*
 * Copyright (c) 2010. The Codehaus. All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.codehaus.httpcache4j.uri;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;

/**
 * Wraps the {@link URLEncoder} of the JDK to provide a more useful interface
 *
 * @author <a href="mailto:hamnis@codehaus.org">Erlend Hamnaberg</a>
 * @version $Revision: $
 */
public final class URIEncoder {
    private URIEncoder() {
    }

    public static String encodeUTF8(String input) {
        return encode(input, StandardCharsets.UTF_8);
    }

    public static String encode(String input, String encoding) {
        return encode(input, Charset.forName(encoding));
    }

    public static String encode(String input, Charset encoding) {
        if (input == null) {
            return null;
        }
        try {
            return URLEncoder.encode(input, encoding.name());
        } catch (UnsupportedEncodingException e) {
            throw new UnsupportedCharsetException(encoding.name());
        }
    }
}
