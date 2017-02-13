/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2014 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.jaspersoft.jasperserver.jaxrs.client.core;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class for validating, encoding and decoding components
 * of a URI.
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class UriComponent {

    /**
     * The URI component type.
     */
    public enum Type {

        /**
         * ALPHA / DIGIT / "-" / "." / "_" / "~" characters
         */
        UNRESERVED,
        /**
         * The URI scheme component type.
         */
        SCHEME,
        /**
         * The URI authority component type.
         */
        AUTHORITY,
        /**
         * The URI user info component type.
         */
        USER_INFO,
        /**
         * The URI host component type.
         */
        HOST,
        /**
         * The URI port component type.
         */
        PORT,
        /**
         * The URI path component type.
         */
        PATH,
        /**
         * The URI path component type that is a path segment.
         */
        PATH_SEGMENT,
        /**
         * The URI path component type that is a matrix parameter.
         */
        MATRIX_PARAM,
        /**
         * The URI query component type.
         */
        QUERY,
        /**
         * The URI query component type that is a query parameter.
         */
        QUERY_PARAM,
        /**
         * The URI fragment component type.
         */
        FRAGMENT,
    }

    private UriComponent() {
    }

    /**
     * Validates the legal characters of a percent-encoded string that
     * represents a URI component type.
     *
     * @param s the encoded string.
     * @param t the URI component type identifying the legal characters.
     * @return true if the encoded string is valid, otherwise false.
     */
    public static boolean valid(String s, Type t) {
        return valid(s, t, false);
    }

    /**
     * Validates the legal characters of a percent-encoded string that
     * represents a URI component type.
     *
     * @param s        the encoded string.
     * @param t        the URI component type identifying the legal characters.
     * @param template true if the encoded string contains URI template variables
     * @return true if the encoded string is valid, otherwise false.
     */
    public static boolean valid(String s, Type t, boolean template) {
        return _valid(s, t, template) == -1;
    }

    private static int _valid(String s, Type t, boolean template) {
        boolean[] table = ENCODING_TABLES[t.ordinal()];

        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            if ((c < 0x80 && c != '%' && !table[c]) || c >= 0x80) {
                if (!template || (c != '{' && c != '}')) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Contextually encodes the characters of string that are either non-ASCII
     * characters or are ASCII characters that must be percent-encoded using the
     * UTF-8 encoding. Percent-encoded characters will be recognized and not
     * double encoded.
     *
     * @param s the string to be encoded.
     * @param t the URI component type identifying the ASCII characters that
     *          must be percent-encoded.
     * @return the encoded string.
     */
    public static String contextualEncode(String s, Type t) {
        return _encode(s, t, false, true);
    }

    private static String _encode(String s, Type t, boolean template, boolean contextualEncode) {
        final boolean[] table = ENCODING_TABLES[t.ordinal()];
        boolean insideTemplateParam = false;

        StringBuilder sb = null;
        for (int offset = 0, codePoint;
             offset < s.length();
             offset += Character.charCount(codePoint))
        {
            codePoint = s.codePointAt(offset);

            if (codePoint < 0x80 && table[codePoint]) {
                if (sb != null) {
                    sb.append((char)codePoint);
                }
            } else {
                if (template) {
                    boolean leavingTemplateParam = false;
                    if (codePoint == '{') {
                        insideTemplateParam = true;
                    } else if (codePoint == '}') {
                        insideTemplateParam = false;
                        leavingTemplateParam = true;
                    }
                    if (insideTemplateParam || leavingTemplateParam) {
                        if (sb != null) {
                            sb.append(Character.toChars(codePoint));
                        }
                        continue;
                    }
                }

                if (contextualEncode
                        && codePoint == '%'
                        && offset + 2 < s.length()
                        && isHexCharacter(s.charAt(offset + 1))
                        && isHexCharacter(s.charAt(offset + 2)))
                {
                    if (sb != null) {
                        sb.append('%').append(s.charAt(offset + 1)).append(s.charAt(offset + 2));
                    }
                    offset += 2;
                    continue;
                }

                if (sb == null) {
                    sb = new StringBuilder();
                    sb.append(s.substring(0, offset));
                }

                if (codePoint < 0x80) {
                    if (codePoint == ' ' && (t == Type.QUERY_PARAM)) {
                        sb.append('+');
                    } else {
                        appendPercentEncodedOctet(sb, (char)codePoint);
                    }
                } else {
                    appendUTF8EncodedCharacter(sb, codePoint);
                }
            }
        }

        return (sb == null) ? s : sb.toString();
    }

    private final static char[] HEX_DIGITS = {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    private static void appendPercentEncodedOctet(StringBuilder sb, int b) {
        sb.append('%');
        sb.append(HEX_DIGITS[b >> 4]);
        sb.append(HEX_DIGITS[b & 0x0F]);
    }

    private static void appendUTF8EncodedCharacter(StringBuilder sb, int codePoint) {
        final CharBuffer cb = CharBuffer.wrap(Character.toChars(codePoint));
        final ByteBuffer bb = UTF_8_CHARSET.encode(cb);

        while (bb.hasRemaining()) {
            appendPercentEncodedOctet(sb, bb.get() & 0xFF);
        }
    }

    private static final String[] SCHEME = {"0-9", "A-Z", "a-z", "+", "-", "."};
    private static final String[] UNRESERVED = {"0-9", "A-Z", "a-z", "-", ".", "_", "~"};
    private static final String[] SUB_DELIMS = {"!", "$", "&", "'", "(", ")", "*", "+", ",", ";", "="};
    private static final boolean[][] ENCODING_TABLES = initEncodingTables();

    private static boolean[][] initEncodingTables() {
        boolean[][] tables = new boolean[Type.values().length][];

        List<String> l = new ArrayList<String>();
        l.addAll(Arrays.asList(SCHEME));
        tables[Type.SCHEME.ordinal()] = initEncodingTable(l);

        l.clear();

        l.addAll(Arrays.asList(UNRESERVED));
        tables[Type.UNRESERVED.ordinal()] = initEncodingTable(l);

        l.addAll(Arrays.asList(SUB_DELIMS));

        tables[Type.HOST.ordinal()] = initEncodingTable(l);

        tables[Type.PORT.ordinal()] = initEncodingTable(Arrays.asList("0-9"));

        l.add(":");

        tables[Type.USER_INFO.ordinal()] = initEncodingTable(l);

        l.add("@");

        tables[Type.AUTHORITY.ordinal()] = initEncodingTable(l);

        tables[Type.PATH_SEGMENT.ordinal()] = initEncodingTable(l);
        tables[Type.PATH_SEGMENT.ordinal()][';'] = false;

        tables[Type.MATRIX_PARAM.ordinal()] = tables[Type.PATH_SEGMENT.ordinal()].clone();
        tables[Type.MATRIX_PARAM.ordinal()]['='] = false;

        l.add("/");

        tables[Type.PATH.ordinal()] = initEncodingTable(l);

        l.add("?");

        tables[Type.QUERY.ordinal()] = initEncodingTable(l);

        tables[Type.FRAGMENT.ordinal()] = tables[Type.QUERY.ordinal()];

        tables[Type.QUERY_PARAM.ordinal()] = initEncodingTable(l);
        tables[Type.QUERY_PARAM.ordinal()]['='] = false;
        tables[Type.QUERY_PARAM.ordinal()]['+'] = false;
        tables[Type.QUERY_PARAM.ordinal()]['&'] = false;

        return tables;
    }

    private static boolean[] initEncodingTable(List<String> allowed) {
        boolean[] table = new boolean[0x80];
        for (String range : allowed) {
            if (range.length() == 1) {
                table[range.charAt(0)] = true;
            } else if (range.length() == 3 && range.charAt(1) == '-') {
                for (int i = range.charAt(0); i <= range.charAt(2); i++) {
                    table[i] = true;
                }
            }
        }

        return table;
    }

    private static final Charset UTF_8_CHARSET = Charset.forName("UTF-8");

    private static final int[] HEX_TABLE = initHexTable();

    private static int[] initHexTable() {
        int[] table = new int[0x80];
        Arrays.fill(table, -1);

        for (char c = '0'; c <= '9'; c++) {
            table[c] = c - '0';
        }
        for (char c = 'A'; c <= 'F'; c++) {
            table[c] = c - 'A' + 10;
        }
        for (char c = 'a'; c <= 'f'; c++) {
            table[c] = c - 'a' + 10;
        }
        return table;
    }

    /**
     * Checks whether the character {@code c} is hexadecimal character.
     *
     * @param c Any character
     * @return The is {@code c} is a hexadecimal character (e.g. 0, 5, a, A, f, ...)
     */
    public static boolean isHexCharacter(char c) {
        return c < 128 && HEX_TABLE[c] != -1;
    }
}
