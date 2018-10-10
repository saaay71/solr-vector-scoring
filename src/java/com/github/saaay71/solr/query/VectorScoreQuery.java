package com.github.saaay71.solr.query;

import java.io.IOException;
import java.util.*;

import com.github.saaay71.solr.VectorUtils;
import com.github.saaay71.solr.query.score.VectorQueryScorerFactory;
import org.apache.lucene.index.*;
import org.apache.lucene.queries.CustomScoreProvider;
import org.apache.lucene.queries.CustomScoreQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.BytesRef;
import org.apache.solr.common.SolrException;
import org.apache.solr.schema.SchemaField;

public class VectorScoreQuery extends CustomScoreQuery {
	private static final String DEFAULT_BINARY_FIELD_NAME = "_vector_";
	private static final Set<String> FIELDS = new HashSet<String>(){{add(DEFAULT_BINARY_FIELD_NAME);}};
	List<Double> vector;
	SchemaField field;
	boolean cosine;

	public VectorScoreQuery(Query subQuery, List<Double> vector, SchemaField field, boolean cosine) {
		super(subQuery);
		this.field = field;
		this.cosine = cosine;
		this.vector = vector;
	}
	@Override
	protected CustomScoreProvider getCustomScoreProvider(LeafReaderContext context) throws IOException {
		return new CustomScoreProvider(context){
			@Override
			public float customScore(int docID, float subQueryScore, float valSrcScore) throws IOException {
				BytesRef vecBytes = context.reader().document(docID, FIELDS).getBinaryValue(DEFAULT_BINARY_FIELD_NAME);
				if(vecBytes == null) {
					throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Could not find vector for docId: \"" + docID + "\"");
				}
				VectorUtils.VectorType vecType = VectorUtils.VectorType.valueOf(
						(String)((Map<String, Object>)(field.getArgs())).getOrDefault("vectorType", "AUTO")
				);

				return VectorQueryScorerFactory.getScorer(vecType, vecBytes).score(vector, VectorQuery.VectorQueryType.COSINE, vecBytes);
			}
		};
	}
}