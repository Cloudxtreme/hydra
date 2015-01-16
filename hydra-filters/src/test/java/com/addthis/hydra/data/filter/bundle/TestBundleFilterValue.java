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
package com.addthis.hydra.data.filter.bundle;

import com.addthis.bundle.util.AutoField;
import com.addthis.bundle.util.map.MapBundle;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class TestBundleFilterValue {

    @Test
    public void fieldTest() {
        BundleFilterValue bfv = new BundleFilterValue().setValue("foo").setToField(AutoField.newAutoField("car"));
        MapBundle bundle = MapBundle.createBundle(new String[]{"dog", "food"});
        bfv.filter(bundle);
        assertEquals(bundle.get("dog"), "food");
        assertEquals(bundle.get("car"), "foo");
    }
}
