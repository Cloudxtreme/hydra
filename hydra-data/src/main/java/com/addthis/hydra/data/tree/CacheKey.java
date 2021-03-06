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
package com.addthis.hydra.data.tree;

import com.addthis.hydra.store.db.DBKey;
import com.addthis.hydra.store.util.Raw;

public class CacheKey {

    public final int hc;
    public final long db;
    public final String name;
    private volatile DBKey dbkey;

    public CacheKey(long db, String name) {
        int hash = Math.abs(Long.hashCode(db) + name.hashCode());
        this.db = db;
        this.name = name;
        if (hash == Integer.MIN_VALUE) {
            hash = Integer.MAX_VALUE;
        }
        hc = hash;
    }

    public DBKey dbkey() {
        if (dbkey == null) {
            dbkey = new DBKey(db, Raw.get(name));
        }
        return dbkey;
    }

    @Override
    public boolean equals(Object key) {
        if (!(key instanceof CacheKey)) {
            return false;
        }
        CacheKey ck = (CacheKey) key;
        return (ck.db == db) && ck.name.equals(name);
    }

    @Override
    public int hashCode() {
        return hc;
    }
}
