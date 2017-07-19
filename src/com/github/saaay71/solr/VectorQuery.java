package com.github.saaay71.solr;

import java.io.IOException;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.ConstantScoreScorer;
import org.apache.lucene.search.ConstantScoreWeight;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;

public class VectorQuery extends Query {
	String queryStr = "";
	Query q;
	
	public VectorQuery(Query subQuery) {
		this.q = subQuery;
	}
	
	public void setQueryString(String queryString){
		this.queryStr = queryString;
	}

	@Override
	public Weight createWeight(IndexSearcher searcher, boolean needsScores) throws IOException {
		Weight w;
		if(q == null){
			w =  new ConstantScoreWeight(this) {
				@Override
				public Scorer scorer(LeafReaderContext context) throws IOException {
					return new ConstantScoreScorer(this, score(), DocIdSetIterator.all(context.reader().maxDoc()));
				}
			};
		}else{
			w = searcher.createWeight(q, needsScores);
		}
		return w;
	}

	@Override
	public String toString(String field) {
		return queryStr;
	}

	@Override
	public boolean equals(Object other) {
		return sameClassAs(other) &&
				queryStr.equals(other.toString());
	}

	@Override
	public int hashCode() {
		return classHash() ^ queryStr.hashCode();
	}

}
