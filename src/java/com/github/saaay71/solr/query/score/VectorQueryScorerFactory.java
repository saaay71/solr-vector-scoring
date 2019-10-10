package com.github.saaay71.solr.query.score;

import com.github.saaay71.solr.VectorUtils;
import com.github.saaay71.solr.query.VectorQuery;
import com.google.protobuf.MapEntry;
import org.apache.lucene.util.BytesRef;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VectorQueryScorerFactory {
    public static final Map<VectorUtils.VectorType, VectorQueryScorer> scorers;

    static {
        scorers = new HashMap<>(2);
        scorers.put(VectorUtils.VectorType.SPARSE, new SparseQueryScorer());
        scorers.put(VectorUtils.VectorType.DENSE, new DenseQueryScorer());
    }

    public static VectorQueryScorer getScorer(VectorUtils.VectorType vectorType, BytesRef buffer) {
        if(vectorType != VectorUtils.VectorType.AUTO) {
            return scorers.get(vectorType);
        }

        if(buffer.bytes[buffer.offset] == VectorUtils.DENSE_VECTOR_BYTE) {
            shiftBytesRef(buffer);
            return safeGet(VectorUtils.VectorType.DENSE);
        }

        shiftBytesRef(buffer);
        return safeGet(VectorUtils.VectorType.SPARSE);
    }

    private static VectorQueryScorer safeGet(VectorUtils.VectorType vectorType) {
        VectorQueryScorer scorer = scorers.get(vectorType);
        if(scorer != null) {
            return scorer;
        }
        throw new RuntimeException("scorer \"" + vectorType.name() + "\" does not exist in factory");
    }

    private static void shiftBytesRef(BytesRef ref) {
        // shift buffer so the first byte is skipped
        ++ref.offset;
        --ref.length;
    }
}
