/*
 * Copyright (c) 2011-2013 GoPivotal, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package reactor.tcp.syslog.hdfs;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import reactor.function.Consumer;
import reactor.io.Buffer;
import reactor.tcp.encoding.syslog.SyslogMessage;

import java.io.IOException;

/**
 * @author Jon Brisbin
 */
public class HdfsConsumer implements Consumer<SyslogMessage> {

	private final FSDataOutputStream out;

	public HdfsConsumer(Configuration conf, String dir, String name) throws IOException {
		Path path = new Path(dir, name);
		FileSystem fs = path.getFileSystem(conf);
		out = fs.create(path, true);
	}

	@Override
	public void accept(SyslogMessage msg) {
		try {
			byte[] bytes = msg.toString().getBytes();
			int len = bytes.length;
			for (int i = 0; i < len; i += Buffer.SMALL_BUFFER_SIZE) {
				int size = Math.min(len - i, Buffer.SMALL_BUFFER_SIZE);
				out.write(bytes, i, size);
			}
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

}
