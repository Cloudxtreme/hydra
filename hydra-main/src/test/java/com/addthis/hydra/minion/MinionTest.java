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
package com.addthis.hydra.minion;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.addthis.basis.test.SlowTest;

import com.addthis.basis.util.LessBytes;
import com.addthis.basis.util.LessFiles;
import com.addthis.codec.config.Configs;
import com.addthis.codec.json.CodecJSON;
import com.addthis.hydra.util.ZkCodecStartUtil;

import com.google.common.collect.ImmutableList;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertEquals;

@Category(SlowTest.class)
public class MinionTest extends ZkCodecStartUtil {

    @Test
    public void testJoinGroup() throws Exception {
        Minion minion = new Minion(zkClient);
        minion.joinGroup();
        List<String> upMinions = zkClient.getChildren().forPath("/minion/up");
        assertEquals(ImmutableList.of(minion.getUUID()), upMinions);
        minion.closeZkClient();
    }

    @Test
    public void decodeDefault() throws Exception {
        Minion minion = Configs.decodeObject(Minion.class, "{queueType = \"\"}");
    }

    @Test
    public void stateFile() throws IOException {
        String stateFile = "src/test/resources/test-minion.state";
        Minion minion = new Minion(null);
        CodecJSON.decodeString(minion, LessBytes.toString(LessFiles.read(new File(stateFile))));
        CodecJSON.encodeString(minion);
    }
}