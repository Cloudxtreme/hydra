/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.addthis.hydra.data.util;

import java.io.IOException;
import java.io.UncheckedIOException;

import java.net.URISyntaxException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.addthis.basis.net.HttpUtil;
import com.addthis.basis.util.LessBytes;
import com.addthis.basis.util.LessStrings;

import com.addthis.maljson.JSONArray;
import com.addthis.maljson.JSONObject;

import com.google.common.base.Throwables;

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;

import org.apache.commons.lang3.mutable.MutableInt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class JSONFetcher {

    private static final Logger log = LoggerFactory.getLogger(JSONFetcher.class);

    public static HashMap<String, String> staticLoadMap(String mapURL) {
        return new JSONFetcher().loadMap(mapURL);
    }

    public static HashMap<String, String> staticLoadMap(String mapURL, HashMap<String, String> map) {
        return new JSONFetcher().loadMap(mapURL, map);
    }

    public static JSONArray staticLoadJSONArray(String mapURL, int urlTimeout, int urlRetries) {
        return new JSONFetcher(urlTimeout, urlRetries, false).loadJSONArray(mapURL);
    }

    public static HashMap<String, String> staticLoadMap(String mapURL, int urlTimeout, int urlRetries, HashMap<String, String> map) {
        return new JSONFetcher(urlTimeout, urlRetries, false).loadMap(mapURL, map);
    }

    public static HashSet<String> staticLoadSet(String mapURL) {
        return new JSONFetcher().loadSet(mapURL);
    }

    public static HashSet<String> staticLoadSet(String mapURL, HashSet<String> set) {
        return new JSONFetcher().loadSet(mapURL, set);
    }

    public static HashSet<String> staticLoadSet(String mapURL, int urlTimeout, int urlRetries, HashSet<String> set) {
        return new JSONFetcher(urlTimeout, urlRetries, false).loadSet(mapURL, set);
    }

    public static HashSet<String> staticLoadCSVSet(String mapURL, int urlTimeout, int urlRetries, HashSet<String> set) {
        return new JSONFetcher(urlTimeout, urlRetries, false).loadCSVSet(mapURL, set);
    }

    private final int timeout;
    private final int retries;
    private final boolean trace;
    private final int backoffMillis;
    private final Retryer<byte[]> retryer;

    public JSONFetcher() {
        this(60000, false);
    }

    public JSONFetcher(int timeout, boolean trace) {
        this(timeout, 0, trace);
    }

    public JSONFetcher(int timeout, int retries, boolean trace) {
        this(timeout, retries, trace, 0);
    }

    public JSONFetcher(int timeout, int retries, boolean trace, int backoffMillis) {
        this.timeout = timeout;
        this.retries = retries;
        this.trace = trace;
        this.backoffMillis = backoffMillis;
        RetryerBuilder<byte[]> retryerBuilder = RetryerBuilder
                .<byte[]>newBuilder()
                .retryIfExceptionOfType(UncheckedIOException.class)
                .withStopStrategy(StopStrategies.stopAfterAttempt(retries));
        if (backoffMillis > 0) {
            retryerBuilder.withWaitStrategy(WaitStrategies.exponentialWait(backoffMillis, TimeUnit.MILLISECONDS));
        } else {
            retryerBuilder.withWaitStrategy(WaitStrategies.noWait());
        }
        retryer = retryerBuilder.build();
    }

    public HashMap<String, String> loadMap(String mapURL) {
        return loadMap(mapURL, new HashMap<>());
    }

    /**
     * loads a json-formatted object from an url and adds enclosing
     * curly brackets if missing
     *
     * @param mapURL
     * @return string/string map
     */
    public HashMap<String, String> loadMap(String mapURL, HashMap<String, String> map) {
        try {
            byte[] raw = retrieveBytes(mapURL);
            String kv = LessBytes.toString(raw).trim();
            if (!(kv.startsWith("{") && kv.endsWith("}"))) {
                kv = LessStrings.cat("{", kv, "}");
            }
            JSONObject o = new JSONObject(kv);
            if (map == null) {
                map = new HashMap<>();
            }
            for (String key : o.keySet()) {
                map.put(key, o.optString(key));
            }
            return map;
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public HashSet<String> loadSet(String mapURL) {
        return loadSet(mapURL, new HashSet<>());
    }

    /**
     * loads a json-formatted array from an url and adds enclosing
     * square brackets if missing
     *
     * @param mapURL
     * @return string set
     */
    public HashSet<String> loadCSVSet(String mapURL, HashSet<String> set) {
        try {
            byte[] raw = retrieveBytes(mapURL);
            String list = LessBytes.toString(raw);

            if (set == null) {
                set = new HashSet<>();
            }

            Scanner in = new Scanner(list);

            while (in.hasNext()) {
                set.add(in.nextLine().replaceAll("^\"|\"$", ""));
            }

            return set;
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public JSONArray loadJSONArray(String mapURL) {
        try {
            byte[] raw = retrieveBytes(mapURL);
            String list = LessBytes.toString(raw);
            if (!(list.startsWith("[") && list.endsWith("]"))) {
                list = LessStrings.cat("[", list, "]");
            }
            JSONArray array = new JSONArray(list);
            return array;
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private byte[] request(String url, MutableInt retry) throws URISyntaxException {
        if (retry.getValue() > 0) {
            log.info("Attempting to fetch {}. Retry {}", url, retry.getValue());
        }
        retry.increment();
        try {
            return HttpUtil.httpGet(url, timeout).getBody();
        } catch (URISyntaxException u) {
            log.error("URISyntaxException on url {}", url, u);
            throw u;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private byte[] retrieveBytes(String url) throws Exception {
        MutableInt retry = new MutableInt(0);
        try {
            return retryer.call(() -> request(url, retry));
        } catch (ExecutionException e) {
            throw Throwables.propagate(e);
        } catch (RetryException e) {
            throw new IOException("Max retries exceeded");
        }
    }

    /**
     * loads a json-formatted array from an url and adds enclosing
     * square brackets if missing
     *
     * @param mapURL
     * @return string set
     */
    public HashSet<String> loadSet(String mapURL, HashSet<String> set) {
        try {
            byte[] raw = retrieveBytes(mapURL);
            String list = LessBytes.toString(raw);
            if (!(list.startsWith("[") && list.endsWith("]"))) {
                list = LessStrings.cat("[", list, "]");
            }
            JSONArray o = new JSONArray(list);
            if (set == null) {
                set = new HashSet<>();
            }
            for (int i = 0; i < o.length(); i++) {
                set.add(o.getString(i));
            }
            return set;
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
