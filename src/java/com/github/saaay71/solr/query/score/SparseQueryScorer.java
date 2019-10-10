package com.github.saaay71.solr.query.score;

import com.github.saaay71.solr.VectorUtils;
import com.github.saaay71.solr.query.VectorQuery;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.util.BytesRef;

import java.util.Iterator;
import java.util.List;

public class SparseQueryScorer implements VectorQueryScorer {
    public Float score(List<Double> vector, VectorQuery.VectorQueryType vQType, BytesRef buffer) {
        float score = 0f;
        double docVectorNorm = 0d;
        Iterator<Pair<Integer, Float>> vecIter = VectorUtils.decodeSparse(buffer);
        while(vecIter.hasNext()) {
            Pair<Integer, Float> decodedPair = vecIter.next();
            Float decodedFloat = decodedPair.getRight();
            score += (decodedFloat) * (vector.get(decodedPair.getLeft()));

            if(vQType == VectorQuery.VectorQueryType.COSINE) {
                docVectorNorm += Math.pow(decodedFloat, 2.0);
            }
        }

        if(vQType == VectorQuery.VectorQueryType.COSINE) {
            double queryVectorNorm = vector.stream().mapToDouble(x -> Math.pow(x, 2.0)).sum();
            if ((docVectorNorm == 0) || (queryVectorNorm == 0)) return 0f;
            return (float)(score / (Math.sqrt(docVectorNorm) * Math.sqrt(queryVectorNorm)));
        }

        return score;
    }
}
