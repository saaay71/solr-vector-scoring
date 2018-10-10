package com.github.saaay71.solr;


import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.analysis.payloads.PayloadHelper;
import org.apache.lucene.util.BytesRef;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.StrUtils;

import java.nio.CharBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class VectorUtils {
    public final static Character DELIMITER = ',';
    public final static Character SPARSE_DELIMITER = '|';
    public final static int SPARSE_SIZE = Float.BYTES + Integer.BYTES;
    public final static Byte DENSE_VECTOR_BYTE = -1;
    public final static Byte SPARSE_VECTOR_BYTE = -2;

    public static BytesRef encode(String input) {
        return encode(input, VectorType.AUTO);
    }

    public static List<Float> parseDenseStrings(List<String> items) {
        return items.stream().map(Float::parseFloat).collect(Collectors.toList());
    }

    public static List<Pair<Integer, Float>> parseSparseStrings(List<String> items) {
        return items.stream().map(x -> {
            int delimiterIndex = x.indexOf(SPARSE_DELIMITER);
            return Pair.of(Integer.parseInt(CharBuffer.wrap(x, 0, delimiterIndex).toString()),
                    Float.parseFloat(CharBuffer.wrap(x, delimiterIndex + 1, x.length()).toString()));
        }).collect(Collectors.toList());
    }

    public static BytesRef encodeDense(BytesRef bytes, List<Float> floats) {
        final int numOfFloats = floats.size();
        for (int i = 0; i < numOfFloats; ++i) {
            PayloadHelper.encodeFloat(floats.get(i), bytes.bytes, bytes.offset + (i * Float.BYTES));
        }
        return bytes;
    }

    public static BytesRef encodeSparse(BytesRef bytes, List<Pair<Integer, Float>> items) {
        final int numberOfElements = items.size();
        for(int i = 0; i < numberOfElements; ++i) {
            PayloadHelper.encodeInt(items.get(i).getLeft(), bytes.bytes, bytes.offset + (i * SPARSE_SIZE));
            PayloadHelper.encodeFloat(items.get(i).getRight(), bytes.bytes, bytes.offset + (i * SPARSE_SIZE) + Integer.BYTES);
        }
        return bytes;
    }

    public static BytesRef encode(String input, VectorType vecType) {
        List<String> items = StrUtils.splitSmart(input, DELIMITER);
        byte[] bytes;
        switch (vecType) {
            case DENSE:
                bytes = new byte[items.size() * Float.BYTES];
                return encodeDense(new BytesRef(bytes), parseDenseStrings(items));
            case SPARSE:
                bytes = new byte[items.size() * SPARSE_SIZE];
                return encodeSparse(new BytesRef(bytes), parseSparseStrings(items));
            case AUTO:
                final boolean isSparse = input.indexOf(SPARSE_DELIMITER) != -1;
                bytes = new byte[1 + (isSparse? (items.size() * SPARSE_SIZE): (items.size() * Float.BYTES))];
                BytesRef bytesRef = new BytesRef(bytes, 1, bytes.length - 1);
                if(isSparse) {
                    bytes[0] = SPARSE_VECTOR_BYTE;
                    encodeSparse(bytesRef, parseSparseStrings(items));
                } else {
                    bytes[0] = DENSE_VECTOR_BYTE;
                    encodeDense(bytesRef, parseDenseStrings(items));
                }
                return new BytesRef(bytes);
        }
        throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "unsupported vector type: " + vecType.name());
    }

    public static Iterator<Float> decodeDense(BytesRef buffer) {
        return new Iterator<Float>() {

            private final BytesRef bytesRef = buffer;
            private final int maxOffset = bytesRef.length + bytesRef.offset;
            private int i = bytesRef.offset;

            @Override
            public boolean hasNext() {
                return i < maxOffset;
            }

            @Override
            public Float next() {
                Float decodedData = PayloadHelper.decodeFloat(bytesRef.bytes, i);
                i += Float.BYTES;
                return decodedData;
            }
        };
    }

    public static Iterator<Pair<Integer, Float>> decodeSparse(BytesRef buffer) {
        return new Iterator<Pair<Integer, Float>>() {

            private final BytesRef bytesRef = buffer;
            private final int maxOffset = bytesRef.length + bytesRef.offset;
            private int i = bytesRef.offset;

            @Override
            public boolean hasNext() {
                return i < maxOffset;
            }

            @Override
            public Pair<Integer, Float> next() {
                Integer index = PayloadHelper.decodeInt(bytesRef.bytes, i);
                Float decodedFloat = PayloadHelper.decodeFloat(bytesRef.bytes, i + Integer.BYTES);
                i += SPARSE_SIZE;
                return Pair.of(index, decodedFloat);
            }
        };
    }

    public static double[] parseInputVec(String input, int vecDimensions) {
        List<String> items = StrUtils.splitSmart(input, VectorUtils.DELIMITER);
        boolean isSparse = input.indexOf(VectorUtils.SPARSE_DELIMITER) != -1;
        if(!isSparse) {
            return VectorUtils.parseDenseStrings(items).stream().mapToDouble(x -> (Double.valueOf(x.toString()))).toArray();
        }

        List<Pair<Integer, Float>> parsedList = VectorUtils.parseSparseStrings(items);
        double[] denseVector = new double[vecDimensions];
        parsedList.forEach(kvPair -> denseVector[kvPair.getLeft()] = Double.valueOf(kvPair.getRight().toString()));
        return denseVector;
    }

    public enum VectorType {
        SPARSE,
        DENSE,
        AUTO
    }
}
