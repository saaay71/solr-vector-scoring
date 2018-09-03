package com.github.saaay71.solr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.analysis.payloads.PayloadHelper;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queries.CustomScoreProvider;
import org.apache.lucene.queries.CustomScoreQuery;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.BytesRef;
import org.apache.solr.common.SolrException;

public class VectorScoreQuery extends CustomScoreQuery {	
	List<Double> vector;
	String field;
	boolean cosine = true;
	double queryVectorNorm = 0;
	
	public VectorScoreQuery(Query subQuery, String Vector, String field, boolean cosine) {
		super(subQuery);
		this.field = field;
		this.cosine = cosine;
		this.vector = new ArrayList<Double>();
		String[] vectorArray = Vector.split(",");
		for(int i=0;i<vectorArray.length;i++){
			double v = Double.parseDouble(vectorArray[i]);
			vector.add(v);
			if (cosine){
				queryVectorNorm += Math.pow(v, 2.0);
			}
		}
	}
	@Override
	protected CustomScoreProvider getCustomScoreProvider(LeafReaderContext context) throws IOException {
		return new CustomScoreProvider(context){
			@Override
			public float customScore(int docID, float subQueryScore, float valSrcScore) throws IOException {
				float score = 0;
				double docVectorNorm = 0;
				LeafReader reader = context.reader();
				Terms terms = reader.getTermVector(docID, field);
				if(vector.size() != terms.size()){
					throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "indexed and input vector array must have same length");
				}
				TermsEnum iter = terms.iterator();
			    BytesRef text;
			    while ((text = iter.next()) != null) {
			    	String term = text.utf8ToString();
			    	float payloadValue = 0f;
			    	PostingsEnum postings = iter.postings(null, PostingsEnum.ALL);
			    	while (postings.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
			    		int freq = postings.freq();
			    		while (freq-- > 0) postings.nextPosition();

			    		BytesRef payload = postings.getPayload();
			    		payloadValue = PayloadHelper.decodeFloat(payload.bytes, payload.offset); 
			    		
			    		if (cosine)
			              docVectorNorm += Math.pow(payloadValue, 2.0);
			    	}
			    		
			    	score = (float)(score + payloadValue * (vector.get(Integer.parseInt(term))));
			    }
			    
			    if (cosine) {
			      if ((docVectorNorm == 0) || (queryVectorNorm == 0)) return 0f;
			      return (float)(score / (Math.sqrt(docVectorNorm) * Math.sqrt(queryVectorNorm)));
			    }

				return score;
			}
		};
	}
}