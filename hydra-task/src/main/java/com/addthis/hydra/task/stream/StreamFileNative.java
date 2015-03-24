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
package com.addthis.hydra.task.stream;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.addthis.codec.annotations.FieldConfig;
import com.addthis.codec.codables.Codable;
import com.addthis.hydra.store.compress.CompressedStream;


public class StreamFileNative implements StreamFile, Codable {

    @FieldConfig(codable = true)
    private String name;
    @FieldConfig(codable = true)
    private long length;
    @FieldConfig(codable = true)
    private long lastModified;

    private File file;

    public StreamFileNative() {
    }

    public StreamFileNative(File file) {
        this(file.getPath(), file.length(), file.lastModified());
        this.file = file;
    }

    public StreamFileNative(String name, long length, long lastModified) {
        this.name = name;
        this.length = length;
        this.lastModified = lastModified;
    }

    public File file() {
        if (file == null) {
            file = new File(name);
        }
        return file;
    }

    /**
     * Interface
     */
    @Override
    public String name() {
        return name;
    }

    /**
     * Interface
     */
    @Override
    public long length() {
        return length;
    }

    /**
     * Interface
     */
    @Override
    public long lastModified() {
        return lastModified;
    }

    /**
     * Interface
     */
    @Override
    public String getPath() {
        return file().getPath();
    }

    public String getURLString() {
        String path = file().getPath();
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return "file://".concat(path);
    }

    /**
     * Interface
     */
    @Override
    public InputStream getInputStream() throws IOException {
        InputStream in = new BufferedInputStream(new FileInputStream(name));
        in = CompressedStream.decompressInputStream(in, name);
        return in;
    }

    public String toString() {
        return name + "," + length + "," + lastModified;
    }
}
