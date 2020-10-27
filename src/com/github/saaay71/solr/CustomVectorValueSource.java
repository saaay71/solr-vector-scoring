package com.github.saaay71.solr;

import org.apache.lucene.analysis.payloads.PayloadHelper;
import org.apache.lucene.index.*;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.DoubleValues;
import org.apache.lucene.search.DoubleValuesSource;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.util.BytesRef;
import org.apache.solr.common.SolrException;

import java.io.IOException;
import java.util.*;

public class CustomVectorValueSource extends DoubleValuesSource {
	private List<Double> vector;
	private String field;
	double queryVectorNorm = 0;

	public CustomVectorValueSource(String Vector, String field) {
		this.field = field;
		this.vector = new ArrayList<Double>();
		String[] vectorArray = Vector.split(",");
		for (int i = 0; i < vectorArray.length; i++) {
			double v = Double.parseDouble(vectorArray[i]);
			vector.add(v);

			queryVectorNorm += Math.pow(v, 2.0);

		}

		if (vector != null) {
			System.out.println("Input Vector size:" + vector.size());
		} else {
			System.out.println("Input Vector is null");
		}
	}

	@Override
	public DoubleValues getValues(LeafReaderContext ctx, DoubleValues doubleValues) throws IOException {

		return new DoubleValues() {

			double val = 0;

			@Override
			public double doubleValue() throws IOException {
				return val;
			}

			@Override
			public boolean advanceExact(int docId) throws IOException {
				float score = 0;
				double docVectorNorm = 0;
				LeafReader reader = ctx.reader();
				Terms terms = reader.getTermVector(docId, field);

				if (vector.size() != terms.size()) {
					throw new SolrException(SolrException.ErrorCode.BAD_REQUEST,
							"indexed and input vector array must have same length");
				}

				TermsEnum iter = terms.iterator();
				BytesRef text;
				while ((text = iter.next()) != null) {
					String term = text.utf8ToString();
					float payloadValue = 0f;
					PostingsEnum postings = iter.postings(null, PostingsEnum.ALL);
					while (postings.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
						int freq = postings.freq();
						while (freq-- > 0)
							postings.nextPosition();

						BytesRef payload = postings.getPayload();
						payloadValue = PayloadHelper.decodeFloat(payload.bytes, payload.offset);

						docVectorNorm += Math.pow(payloadValue, 2.0);
					}

					score = (float) (score + payloadValue * (vector.get(Integer.parseInt(term))));
				}

				if ((docVectorNorm == 0) || (queryVectorNorm == 0))
					val = 0;
				val = (float) (score / (Math.sqrt(docVectorNorm) * Math.sqrt(queryVectorNorm)));

				return true;
			}
		};
	}

	@Override
	public boolean needsScores() {
		return false;
	}

	@Override
	public DoubleValuesSource rewrite(IndexSearcher indexSearcher) throws IOException {
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		CustomVectorValueSource that = (CustomVectorValueSource) o;
		return vector.equals(that.vector) && field.equals(that.field);
	}

	@Override
	public int hashCode() {
		return Objects.hash(vector, field);
	}

	@Override
	public String toString() {
		return "VectorValuesSource{" + "vector=" + vector + ", Field=" + field + '}';
	}

	@Override
	public boolean isCacheable(LeafReaderContext leafReaderContext) {
		return false;
	}
}