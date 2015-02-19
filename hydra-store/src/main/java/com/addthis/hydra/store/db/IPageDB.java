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
package com.addthis.hydra.store.db;

import javax.annotation.Nonnull;

import java.util.Map.Entry;

import com.addthis.basis.util.ClosableIterator;

import com.addthis.codec.codables.BytesCodable;
import com.addthis.hydra.store.db.IPageDB.Key;
import com.addthis.hydra.store.kv.PagedKeyValueStore;
import com.addthis.hydra.store.util.Raw;


public interface IPageDB<K extends Key, V extends BytesCodable> extends AutoCloseable {

    public interface Key {

        public long id();

        public byte[] key();

        public byte[] toBytes();

        public byte[] deltaEncode(@Nonnull Key baseKey);

        public Raw rawKey();

    }

    public interface Range<K, V> extends ClosableIterator<Entry<K, V>>, Iterable<Entry<K, V>> {

    }

    public V get(K key);

    public V put(K key, V value);

    public V remove(K key);

    public void remove(K from, K to, boolean inclusive);

    public Range<K, V> range(K from, K to);

    public PagedKeyValueStore<DBKey, V> getEps();

    /**
     * Close the source.
     *
     * @param cleanLog if true then wait for the BerkeleyDB clean thread to finish.
     * @param operation optionally test or repair the berkeleyDB.
     * @return status code. A status code of 0 indicates success.
     */
    public int close(boolean cleanLog, CloseOperation operation);

    public void setCacheSize(final int cachesize);

    public void setPageSize(int pagesize);

    public void setCacheMem(long maxmem);

    public void setPageMem(int maxmem);

    public void setMemSampleInterval(int sample);
}
