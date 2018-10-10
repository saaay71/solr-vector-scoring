package com.github.saaay71.solr.query.score;

import com.github.saaay71.solr.VectorUtils;
import com.github.saaay71.solr.query.VectorQuery;
import org.apache.lucene.util.BytesRef;

import java.util.Iterator;
import java.util.List;

public class DenseQueryScorer implements VectorQueryScorer {
    public Float score(List<Double> vector, VectorQuery.VectorQueryType vQType, BytesRef buffer) {
        float score = 0f;
        double docVectorNorm = 0d;
        int vectorIndex = 0;
        Iterator<Float> vecIter = VectorUtils.decodeDense(buffer);
        while(vecIter.hasNext()) {
            Float val = vecIter.next();
            score += (val) * (vector.get(vectorIndex));

            if(vQType == VectorQuery.VectorQueryType.COSINE) {
                docVectorNorm += Math.pow(val, 2.0);
            }

            ++vectorIndex;
        }

        if(vQType == VectorQuery.VectorQueryType.COSINE) {
            double queryVectorNorm = vector.stream().mapToDouble(x -> Math.pow(x, 2.0)).sum();
            if ((docVectorNorm == 0) || (queryVectorNorm == 0)) return 0f;
            return (float)(score / (Math.sqrt(docVectorNorm) * Math.sqrt(queryVectorNorm)));
        }

        return score;
    }
}
