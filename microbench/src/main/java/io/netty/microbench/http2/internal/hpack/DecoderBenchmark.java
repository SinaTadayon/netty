/*
 * Copyright 2015 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

/*
 * Copyright 2015 Twitter, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.netty.microbench.http2.internal.hpack;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http2.internal.hpack.Decoder;
import io.netty.handler.codec.http2.internal.hpack.Encoder;
import io.netty.handler.codec.http2.internal.hpack.HeaderListener;
import io.netty.microbench.util.AbstractMicrobenchmark;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;
import java.util.List;

public class DecoderBenchmark extends AbstractMicrobenchmark {

    @Param
    public HeadersSize size;

    @Param({ "4096" })
    public int maxTableSize;

    @Param({ "8192" })
    public int maxHeaderSize;

    @Param({ "true", "false" })
    public boolean sensitive;

    @Param({ "true", "false" })
    public boolean limitToAscii;

    private byte[] input;

    @Setup(Level.Trial)
    public void setup() throws IOException {
        input = getSerializedHeaders(Util.headers(size, limitToAscii), sensitive);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void decode(final Blackhole bh) throws IOException {
        Decoder decoder = new Decoder(maxHeaderSize, maxTableSize);
        decoder.decode(Unpooled.wrappedBuffer(input), new HeaderListener() {
            @Override
            public void addHeader(CharSequence name, CharSequence value, boolean sensitive) {
                bh.consume(sensitive);
            }
        });
        decoder.endHeaderBlock();
    }

    private byte[] getSerializedHeaders(List<Header> headers, boolean sensitive) {
        Encoder encoder = new Encoder(4096);

        ByteBuf out = size.newOutBuffer();
        try {
            for (int i = 0; i < headers.size(); ++i) {
                Header header = headers.get(i);
                encoder.encodeHeader(out, header.name, header.value, sensitive);
            }
            byte[] bytes = new byte[out.readableBytes()];
            out.readBytes(bytes);
            return bytes;
        } finally {
            out.release();
        }
    }
}
