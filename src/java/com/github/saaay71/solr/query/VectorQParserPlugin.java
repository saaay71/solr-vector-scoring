package com.github.saaay71.solr.query;

import com.github.saaay71.solr.VectorUtils;
import com.github.saaay71.solr.updateprocessor.LSHUpdateProcessorFactory;
import info.debatty.java.lsh.LSHSuperBit;
import org.apache.lucene.queryparser.simple.SimpleQueryParser;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.Query;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.StrUtils;
import org.apache.solr.core.InitParams;
import org.apache.solr.core.SolrCore;
import org.apache.solr.parser.SolrQueryParserBase;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.schema.FieldType;
import org.apache.solr.search.*;
import org.apache.solr.update.processor.UpdateRequestProcessorChain;
import org.apache.solr.update.processor.UpdateRequestProcessorFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class VectorQParserPlugin extends QParserPlugin {

	private static Map<String, LSHSuperBit> superBitCache = new HashMap<>();

	@Override
	public QParser createParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {
		return new QParser(qstr, localParams, params, req) {
			@Override
			public Query parse() throws SyntaxError {
				String field = localParams.get(QueryParsing.F);
				String vector = localParams.get("vector");
				boolean cosine = localParams.getBool("cosine", true);

				if (field == null) {
					throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "'f' not specified");
				}

				if (vector == null) {
					throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "vector missing");
				}



				FieldType ft = req.getCore().getLatestSchema().getFieldType(field);

				Query subQuery = subQuery(localParams.get(QueryParsing.V), null).getQuery();
				if(ft != null) {
					VectorQuery q = new VectorQuery(subQuery);
					q.setQueryString(localParams.toLocalParamsString()); 
					query = q;
				}


				if (query == null) {
					throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Query is null");
				}

				String[] vectorArray = vector.split(",");

				if(localParams.getBool("lsh", false)) {
					final int topNDocs = localParams.getInt("topNDocs", 100);
					String lshQuery = computeLSHQueryString(vector, vectorArray);
					Query luceneQuery = req.getCore().getQueryPlugin("lucene")
							.createParser(lshQuery, localParams, params, req).parse();

					// do not run cosine similarity
					if(topNDocs == 0) {
						return luceneQuery;
					}
					final int reRankWeight = localParams.getInt("weight", 10);
					SolrParams computedLocalParams = new ModifiableSolrParams(localParams)
							.set("reRankQuery", "{!vp f=" + field + " vector=" +vector + " lsh=\"false\"}")
							.set("reRankDocs=", topNDocs)
							.set("reRankWeight", reRankWeight)
							.set("q", lshQuery);
					query = ((AbstractReRankQuery) req.getCore().getQueryPlugin(ReRankQParserPlugin.NAME)
							.createParser(lshQuery, computedLocalParams, params, req).getQuery()).wrap(luceneQuery);
				}

				List<Double> vectorList = new ArrayList<>();
				for(int i=0;i<vectorArray.length;i++){
					double v = Double.parseDouble(vectorArray[i]);
					vectorList.add(v);
				}

				return new VectorScoreQuery(query, vectorList, req.getSchema().getField(field), cosine);
			}

			private String computeLSHQueryString(String vector, String[] vectorArray) {
				String reqChain = getUpdateChainName(req);
				LSHSuperBit superBit = superBitCache.computeIfAbsent(reqChain, (k) -> {
					LSHUpdateProcessorFactory lshFactory = getLSHProcessorFromChain(req.getCore(), reqChain);
					return new LSHSuperBit(lshFactory.getStages(), lshFactory.getBuckets(), lshFactory.getDimensions(), lshFactory.getSeed());
				});
				int[] intHash = superBit.hash(VectorUtils.parseInputVec(vector, vectorArray.length));
				return LSHUtils.getLSHStringStream(intHash).map(x -> LSHUpdateProcessorFactory.DEFAULT_LSH_FIELD_NAME + ":\"" + x + "\"")
						.collect(Collectors.joining(" OR "));
			}
		};
	}

	private static LSHUpdateProcessorFactory getLSHProcessorFromChain(SolrCore core, String reqChainName) {
		UpdateRequestProcessorChain chain = core.getUpdateProcessingChain(reqChainName);
		if(chain == null) {
			throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Could not find processing chain with \"" + LSHUpdateProcessorFactory.class.getName() +
					"\", try setting a default chain or suppling one in the request params");
		}
		List<UpdateRequestProcessorFactory> LSHProcessors = chain.getProcessors().stream().filter(x -> x instanceof LSHUpdateProcessorFactory).collect(Collectors.toList());
		if(LSHProcessors.size() == 0 || LSHProcessors.size() > 1) {
			throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "found " + LSHProcessors.size() +
					" LSH Update Processors in update chain: \"" + reqChainName + "\" while only 1 is supported");
		}
		return (LSHUpdateProcessorFactory) LSHProcessors.get(0);
	}

	private static String getUpdateChainName(SolrQueryRequest req) {
		String chainName = req.getParams().get("update.chain");
		if(chainName != null) {
			return chainName;
		}
		// try get the default
		try {
			return ((NamedList) req.getCore().getSolrConfig().getPluginInfo(InitParams.class.getName()).initArgs.get("defaults")).get("update.chain").toString();
		} catch (NullPointerException e) {
			throw new SolrException(SolrException.ErrorCode.BAD_REQUEST,
					"Could not determine request chain name, try setting one using the update.chain request param");
		}
	}
}
