package com.github.saaay71.solr;


import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.analysis.payloads.AbstractEncoder;
import org.apache.lucene.analysis.payloads.PayloadEncoder;
import org.apache.lucene.analysis.payloads.PayloadHelper;
import org.apache.lucene.util.BytesRef;

import java.nio.CharBuffer;

public class VectorPayloadEncoder extends AbstractEncoder implements PayloadEncoder {
    private final static char delimiter = ',';
    private final static int SIZE = Float.BYTES + Integer.BYTES;
    public final static int DENSE_VECTOR_PREFIX = -1;

    public VectorPayloadEncoder() { }

    public BytesRef encode(char[] buffer, int offset, int length) {
        int i;
        for(i = offset; i < offset + length; ++i) {
            if(buffer[i] == delimiter) {
                break;
            }
        }
        byte[] bytes = new byte[SIZE];

        final boolean isSparse = i < offset + length;
        final float vectorElem;
        if(isSparse) {
            final int sparseIndex = Integer.parseInt(CharBuffer.wrap(buffer, offset, i - offset).toString());
            PayloadHelper.encodeInt(sparseIndex, bytes, 0);
            vectorElem = Float.parseFloat(CharBuffer.wrap(buffer, offset + i, offset + length - (i + 1)).toString());
        } else {
            PayloadHelper.encodeInt(DENSE_VECTOR_PREFIX, bytes, 0);
            vectorElem = Float.parseFloat(CharBuffer.wrap(buffer, offset, length).toString());
        }

        PayloadHelper.encodeFloat(vectorElem, bytes, Integer.BYTES);
        return new BytesRef(bytes);
    }

    public static Pair<Integer, Float> decode(byte[] buffer) {
        return decode(buffer, 0);
    }

    public static Pair<Integer, Float> decode(byte[] buffer, int offset) {
        final int vecIndex = PayloadHelper.decodeInt(buffer, offset);
        return Pair.of(vecIndex, PayloadHelper.decodeFloat(buffer, offset + Integer.BYTES));
    }
}
