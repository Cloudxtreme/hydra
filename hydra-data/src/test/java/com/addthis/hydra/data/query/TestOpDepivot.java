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
package com.addthis.hydra.data.query;

import org.junit.Test;

public class TestOpDepivot extends TestOp {

    @Test
    public void defaultsAndArbitraryIndexLabel() throws Exception {
        doOpTest(
                new DataTableHelper().
                        tr().td("index").td("a", "b").td().
                        tr().td("aaa", "1", "3", "4").
                        tr().td("bbb", "2", "2", "4").
                        tr().td("ccc", "3", "1", "4"),
                "depivot=row,col,val",
                new DataTableHelper().
                        tr().td("aaa", "a", "1").
                        tr().td("aaa", "b", "3").
                        tr().td("bbb", "a", "2").
                        tr().td("bbb", "b", "2").
                        tr().td("ccc", "a", "3").
                        tr().td("ccc", "b", "1")
        );
    }

    @Test
    public void defaultsPlusCopyMetadata() throws Exception {
        doOpTest(
                new DataTableHelper()
                        .tr().td("index").td("a", "b").td()
                        .tr().td("aaa", "1", "3", "1")
                        .tr().td("bbb", "2", "2", "2")
                        .tr().td("ccc", "3", "1", "1"),
                "depivot=row,col,val,extra",
                new DataTableHelper()
                        .tr().td("aaa", "a", "1", "1")
                        .tr().td("aaa", "b", "3", "1")
                        .tr().td("bbb", "a", "2", "2")
                        .tr().td("bbb", "b", "2", "2")
                        .tr().td("ccc", "a", "3", "1")
                        .tr().td("ccc", "b", "1", "1")
                );
    }
}
