package com.github.saaay71.solr.updateprocessor;

import com.github.saaay71.solr.VectorUtils;
import com.github.saaay71.solr.query.LSHUtils;
import info.debatty.java.lsh.LSHSuperBit;

import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.apache.solr.update.processor.UpdateRequestProcessorFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class LSHUpdateProcessorFactory extends UpdateRequestProcessorFactory {

    private static final Random random = new Random();
    public static final String DEFAULT_LSH_FIELD_NAME = "_lsh_hash_";
    public static final String DEFAULT_BINARY_FIELD_NAME = "_vector_";

    private String fieldName;
    private Long seed;
    private Integer buckets;
    private Integer stages;
    private Integer dimensions;

    @Override
    public void init( NamedList args )
    {
        Object argSeed = args.get("seed");
        seed = argSeed==null? genRandomSeed(): new Long(argSeed.toString());
        buckets = new Integer(getArgString(args, "buckets"));
        stages = new Integer(getArgString(args, "stages"));
        dimensions = new Integer(getArgString(args, "dimensions"));
        Object argFieldName = args.get("field");
        fieldName = argFieldName==null? DEFAULT_LSH_FIELD_NAME: (String) argFieldName;
    }

    public UpdateRequestProcessor getInstance(SolrQueryRequest req, SolrQueryResponse rsp, UpdateRequestProcessor next) {
        return new LSHUpdateProcessor(req.getSchema(), fieldName, stages, buckets, dimensions, seed, next);
    }

    private Long genRandomSeed() {
        return random.nextLong();
    }

    private static String getArgString(NamedList args, String fieldName) throws SolrException {
        Object argVal = args.get(fieldName);
        if(argVal == null) {
            throw new SolrException(SolrException.ErrorCode.NOT_FOUND, LSHUpdateProcessorFactory.class.getName()
                    + " requires arg: \"" + fieldName + "\" which was not configured");
        }
        return argVal.toString();
    }

    public Long getSeed() {
        return seed;
    }

    public Integer getDimensions() {
        return dimensions;
    }

    public Integer getBuckets() {
        return buckets;
    }

    public Integer getStages() {
        return stages;
    }
}

class LSHUpdateProcessor extends UpdateRequestProcessor {

    private final LSHSuperBit superBit;
    private final SchemaField field;
    private final int vecDimensions;
    private final VectorUtils.VectorType vecType;

    public LSHUpdateProcessor(IndexSchema schema, String fieldName, int stages, int buckets, int dimensions, Long seed, UpdateRequestProcessor next) {
        super(next);
        superBit = new LSHSuperBit(stages, buckets, dimensions, seed);
        field = schema.getField(fieldName);
        final String vecTypeArg = ((Map<String, Object>) field.getArgs()).getOrDefault("vectorType","AUTO").toString().toUpperCase();
        vecType = VectorUtils.VectorType.valueOf(vecTypeArg);
        vecDimensions = dimensions;
    }

    @Override
    public void processAdd(AddUpdateCommand cmd) throws IOException {
        SolrInputDocument cmdDoc = cmd.getSolrInputDocument();
        if(cmdDoc.containsKey(field.getName())) {
            final String vectorStr = (String) cmdDoc.getFieldValue(field.getName());
            cmdDoc.setField(LSHUpdateProcessorFactory.DEFAULT_BINARY_FIELD_NAME,
                    VectorUtils.encode(vectorStr, vecType).bytes);
            int[] hashValues = superBit.hash(VectorUtils.parseInputVec(vectorStr, vecDimensions));
            List<String> hashStringValues = LSHUtils.getLSHStringStream(hashValues).collect(Collectors.toList());
            cmdDoc.setField(LSHUpdateProcessorFactory.DEFAULT_LSH_FIELD_NAME, hashStringValues);
        }
        super.processAdd(cmd);
    }
}

