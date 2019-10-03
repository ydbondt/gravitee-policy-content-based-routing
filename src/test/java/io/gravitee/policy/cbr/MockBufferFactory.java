package io.gravitee.policy.cbr;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.buffer.BufferFactory;

public class MockBufferFactory implements BufferFactory {

    private TestBuffer buff = new TestBuffer();

    @Override
    public Buffer buffer(int initialSizeHint) {
        return buff;
    }

    @Override
    public Buffer buffer() {
        return buff;
    }

    @Override
    public Buffer buffer(String str) {
        return buff.appendString(str);
    }

    @Override
    public Buffer buffer(String str, String enc) {
        return buff.appendString(str, enc);
    }

    @Override
    public Buffer buffer(byte[] bytes) {
        return buff.appendString(new String(bytes));
    }
}
