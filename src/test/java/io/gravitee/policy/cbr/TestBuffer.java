package io.gravitee.policy.cbr;

import io.gravitee.gateway.api.buffer.Buffer;

import java.nio.charset.Charset;

public class TestBuffer implements Buffer {

    private String bufferString = "";

    @Override
    public Buffer appendBuffer(Buffer buff) {
        this.bufferString += buff.getNativeBuffer();
        return this;
    }

    @Override
    public Buffer appendBuffer(Buffer buff, int length) {
        this.bufferString += buff.getNativeBuffer();
        return this;
    }

    @Override
    public Buffer appendString(String str, String enc) {
        this.bufferString += str;
        return this;
    }

    @Override
    public Buffer appendString(String str) {
        this.bufferString += str;
        return this;
    }

    @Override
    public String toString(String enc) {
        return bufferString;
    }

    @Override
    public String toString(Charset enc) {
        return bufferString;
    }

    @Override
    public byte[] getBytes() {
        return bufferString.getBytes();
    }

    @Override
    public int length() {
        return bufferString.length();
    }

    @Override
    public Object getNativeBuffer() {
        return bufferString;
    }
}
