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
package com.addthis.hydra.job;

import java.util.List;

import com.addthis.basis.test.SlowTest;

import com.addthis.bark.StringSerializer;
import com.addthis.codec.Codec;
import com.addthis.codec.json.CodecJSON;
import com.addthis.hydra.job.store.DataStoreUtil;
import com.addthis.hydra.job.store.SpawnDataStore;
import com.addthis.hydra.util.ZkCodecStartUtil;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;

import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.addthis.hydra.job.store.SpawnDataStoreKeys.SPAWN_JOB_CONFIG_PATH;
import static org.junit.Assert.*;

@Category(SlowTest.class)
public class JobConfigManagerTest extends ZkCodecStartUtil {

    private static final Codec codec = CodecJSON.INSTANCE;
    private JobConfigManager jobConfigManager;
    private SpawnDataStore spawnDataStore;

    @After
    public void cleanup() throws Exception {
        if (spawnDataStore != null) {
            spawnDataStore.close();
        }
    }

    private JobConfigManager getJobConfigManager() throws Exception {
        if (spawnDataStore == null) {
            spawnDataStore = DataStoreUtil.makeCanonicalSpawnDataStore();
        }
        if (jobConfigManager == null) {
            jobConfigManager = new JobConfigManager(spawnDataStore, null);
        }
        return jobConfigManager;
    }

    @Test
    public void testBasicExists() throws Exception {
        JobConfigManager jcm = getJobConfigManager();
        String id = "foo";
        IJob job = new ZnodeJob(id);
        job.setQueryConfig(new JobQueryConfig());
        jcm.addJob(job);
        assertNotNull(spawnDataStore.getChild(SPAWN_JOB_CONFIG_PATH, id));
        assertNotNull(spawnDataStore.get(SPAWN_JOB_CONFIG_PATH + "/foo/config"));
        assertNotNull(spawnDataStore.get(SPAWN_JOB_CONFIG_PATH + "/foo/queryconfig"));
    }

    @Test
    public void testDeserQC() throws Exception {
        JobConfigManager jcm = getJobConfigManager();
        String id = "foo";
        IJob job = new ZnodeJob(id);
        jcm.addJob(job);
        String raw = spawnDataStore.get(SPAWN_JOB_CONFIG_PATH + "/foo/queryconfig");
        assertNotNull(raw);
        JobQueryConfig jqc = codec.decode(JobQueryConfig.class, StringSerializer.deserialize(raw.getBytes()).getBytes());
        assertEquals(jqc, new JobQueryConfig());
    }


    @Test
    public void testChangeData() throws Exception {
        JobConfigManager jcm = getJobConfigManager();
        String id = "foo";
        IJob job = new ZnodeJob(id);
        JobQueryConfig jqc = new JobQueryConfig();
        job.setQueryConfig(jqc);
        jcm.addJob(job);

        String qc = spawnDataStore.get(SPAWN_JOB_CONFIG_PATH + "/foo/queryconfig");
        // What's up with logging config?
        //logger.warn("qc: " + qc);
        JobQueryConfig cycle_jqc = codec.decode(JobQueryConfig.class, qc.getBytes());
        assertEquals(jqc, cycle_jqc);

        // change stuff
        jqc.setCanQuery(false);
        job.setQueryConfig(jqc);
        jcm.updateJob(job);
        String qc2 = spawnDataStore.get(SPAWN_JOB_CONFIG_PATH + "/foo/queryconfig");
        JobQueryConfig cycle_jqc_2 = codec.decode(JobQueryConfig.class, qc2.getBytes());
        // how to do Not=?
        //System.out.println(cycle_jqc_2);
        assertFalse(cycle_jqc.equals(cycle_jqc_2));
    }

    @Test
    public void testDelete() throws Exception {
        JobConfigManager jcm = getJobConfigManager();
        String id = "foo";
        IJob job = new ZnodeJob(id);
        job.setQueryConfig(new JobQueryConfig());
        jcm.addJob(job);
        assertNotNull(spawnDataStore.getChild(SPAWN_JOB_CONFIG_PATH, id));
        jcm.deleteJob(job.getId());
        assertNull(spawnDataStore.getChild(SPAWN_JOB_CONFIG_PATH, "foo"));
    }


    @Test
    public void testGetJob() throws Exception {
        JobConfigManager jcm = getJobConfigManager();
        String id = "bar";
        IJob job = new ZnodeJob(id);
        job.setQueryConfig(new JobQueryConfig());
        jcm.addJob(job);
        IJob jobBack = jcm.getJob("bar");
        assertNotNull(jobBack);
        assertEquals(job.getId(), jobBack.getId());
        assertEquals(job.getQueryConfig(), jobBack.getQueryConfig());
        assertEquals(job.getCopyOfTasks(), jobBack.getCopyOfTasks());
    }


    @Test
    public void testGetJobNull() throws Exception {
        JobConfigManager jcm = getJobConfigManager();
        IJob job = new ZnodeJob("foo");
        jcm.addJob(job);
        IJob jobBack = jcm.getJob("noexist");
        assertNull(jobBack);
    }


    @Test
    public void testGetJobWithTasks() throws Exception {
        JobConfigManager jcm = getJobConfigManager();
        String id = "bar";
        IJob job = new ZnodeJob(id);
        List<JobTask> tasks = ImmutableList.of(new JobTask("s1.local", 1, 5), new JobTask("s1.local", 2, 5), new JobTask("s2.local", 3, 6));
        job.setTasks(tasks);
        jcm.addJob(job);
        IJob jobBack = jcm.getJob("bar");
        assertEquals(job.getId(), jobBack.getId());
        assertEquals(job.getQueryConfig(), jobBack.getQueryConfig());
        assertEquals(ImmutableList.copyOf(Collections2.transform(job.getCopyOfTasks(), new nodeGetter())),
                ImmutableList.copyOf(Collections2.transform(jobBack.getCopyOfTasks(), new nodeGetter())));
    }

    public static class nodeGetter implements Function<JobTask, Integer> {

        @Override
        public Integer apply(JobTask t) {
            return t.getTaskID();
        }
    }

    @Test
    public void testGetJobs() throws Exception {
        JobConfigManager jcm = getJobConfigManager();
        jcm.addJob(new ZnodeJob("foo"));
        jcm.addJob(new ZnodeJob("bar"));
        new JobConfigManager(spawnDataStore);
    }

}