package com.github.saaay71.solr;

import com.github.saaay71.solr.updateprocessor.LSHUpdateProcessorFactory;
import com.google.common.collect.Iterables;
import info.debatty.java.lsh.LSHSuperBit;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.util.StrUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class VectorQueryTest extends SolrTestCaseJ4 {

    private static AtomicInteger idCounter = new AtomicInteger();
    private static final Set<String> reqLSHFields = new HashSet<>(Arrays.asList("seed", "buckets", "stages", "dimensions"));
    private static LSHSuperBit superBit;
    private static String[] vectors = {
            "0|1.55,1|3.53,2|2.3,3|0.7,4|3.44,5|2.33",
            "0|3.54,1|0.4,2|4.16,3|4.88,4|4.28,5|4.25"
    };
    private static String[] denseVectors = {
            "1.55,3.53,2.3,0.7,3.44,2.33",
            "3.54,0.4,4.16,4.88,4.28,4.25"
    };
    private static Iterator<String> vectorsIter = Iterables.cycle(vectors).iterator();
    private static Iterator<String> denseVectorsIter = Iterables.cycle(denseVectors).iterator();


    @BeforeClass
    public static void beforeClass() throws Exception {
        initCore("solrconfig.xml", "schema-vector.xml");
        LSHUpdateProcessorFactory lshUpdateProcessorFactory = (LSHUpdateProcessorFactory) h.getCore()
                .getUpdateProcessingChain("LSH").getProcessors().get(0);
        Field[] fields = Arrays.stream(lshUpdateProcessorFactory.getClass().getDeclaredFields()).filter(x -> reqLSHFields.contains(x.getName())).toArray(size -> new Field[size]);
        Long[] fieldVals = new Long[fields.length];
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            field.setAccessible(true);
            fieldVals[i] = Long.valueOf(field.get(lshUpdateProcessorFactory).toString());
        }
        superBit = new LSHSuperBit(fieldVals[2].intValue(), fieldVals[1].intValue(), fieldVals[3].intValue(), fieldVals[0]);
    }

    @Before
    public void before() throws Exception {
        deleteByQueryAndGetVersion("*:*", params());
        idCounter.set(0);
    }

    @Test
    public void denseDataTest() throws Exception {
        indexSampleDenseData();

        assertQ(req("q", "*:*"),
                "//*[@numFound='10']");

        assertQ(req("q", "{!vp f=vector vector=\"0.1,4.75,0.3,1.2,0.7,4.0\"}",
                "fl", "name,score,vector"), "//*[@numFound='10']");

        assertQ(req("q", "{!vp f=vector vector=\"1.55,3.53,2.3,0.7,3.44,2.33\"}",
                "fl", "name,score,vector"),
                "//*[@numFound='10']",
                "//doc[1]/float[@name='score'][.='1.0']",
                "count(//float[@name='score'][.='1.0'])=5"
        );

        assertQ(req("q", "{!vp f=vector vector=\""
                        + denseVectors[0].replaceAll("\\|", "").replaceAll(" ", ",")
                        + "\"}",
                "fl", "name,score,vector"),
                "//*[@numFound='10']",
                "//doc[1]/float[@name='score'][.='1.0']",
                "count(//float[@name='score'][.='1.0'])=5"
        );

        // test LSH
        int[] hash = superBit.hash(StrUtils.splitSmart(denseVectors[0], ',').stream().mapToDouble(Double::new).toArray());
        String lshQueryString = IntStream.range(0, hash.length).mapToObj(x -> String.format(Locale.ROOT, "_lsh_hash_:\"%d_%d\"", x, hash[x]))
                .collect(Collectors.joining(" OR "));
        assertQ(req("q", lshQueryString),
                "//*[@numFound='5']"
        );
    }

    @Test
    public void VectorQueryQParserPluginTest() throws Exception {
        indexSampleDenseData();

        // test only lsh
        assertQ(req("q", "{!vp f=vector vector=\"" + denseVectors[0] + "\" lsh=\"true\" topNDocs=\"0\"}"),
                "//*[@numFound='5']",
                "//doc[1]/str[@name='vector'][.='" + denseVectors[0] + "']"
        );

        // test lsh + cosine similarity
        assertQ(req("q", "{!vp f=vector vector=\"" + denseVectors[0] + "\" lsh=\"true\"}",
                "fl", "name, score, vector, _vector_, _lsh_hash_"),
                "//*[@numFound='5']",
                "//doc[1]/str[@name='vector'][.='" + denseVectors[0] + "']",
                "//doc[1]/float[@name='score'][.>='1.0']",
                "count(//float[@name='score'][.>='1.0'])=5"
        );

        //test lsh + cosine similarity + lucene query
        assertQ(req("q", "{!vp f=vector vector=\"" + denseVectors[1] + "\" lsh=\"true\" topNDocs=\"5\" v=\"id:2\"}",
                "fl", "name, score, vector, _vector_, _lsh_hash_"),
                "//*[@numFound='1']",
                "//doc[1]/str[@name='vector'][.='" + denseVectors[1] + "']",
                "//doc[1]/float[@name='score'][.>='1.0']",
                "count(//float[@name='score'][.>='1.0'])=1"
        );
    }

    @Test
    public void sparseDataTest() throws Exception {
        indexSampleData();

        assertQ(req("q", "*:*"),
                "//*[@numFound='10']");

        assertQ(req("q", "{!vp f=vector vector=\"0.1,4.75,0.3,1.2,0.7,4.0\"}",
                "fl", "name,score,vector"), "//*[@numFound='10']");

        assertQ(req("q", "{!vp f=vector vector=\"1.55,3.53,2.3,0.7,3.44,2.33\"}",
                "fl", "name,score,vector"),
                "//*[@numFound='10']",
                "//doc[1]/float[@name='score'][.='1.0']",
                "count(//float[@name='score'][.='1.0'])=5"
        );

        assertQ(req("q", "{!vp f=vector vector=\"" + sparseToDenseVector(vectors[0]) + "\"}",
                "fl", "name,score,vector"),
                "//*[@numFound='10']",
                "//doc[1]/float[@name='score'][.='1.0']",
                "count(//float[@name='score'][.='1.0'])=5"
        );
    }

    private void indexSampleData() throws Exception {
        for(int i = 0; i < 10; i++) {
            addAndGetVersion(sdoc("id", idCounter.incrementAndGet(), "vector", vectorsIter.next()),
                    params("update.chain", "LSH", "wt", "json"));
        }
        assertU(commit());
    }

    private void indexSampleDenseData() throws Exception {
        for(int i = 0; i < 10; i++) {
            addAndGetVersion(sdoc("id", idCounter.incrementAndGet(), "vector", denseVectorsIter.next()),
                    params("update.chain", "LSH", "wt", "json"));
        }
        assertU(commit());
    }

    private String sparseToDenseVector(String sparseVec) {
        List<String> splitList = StrUtils.splitSmart(sparseVec.replaceAll(",", " "), ' ')
                .stream().map(x -> x.split("\\|")[1])
                .collect(Collectors.toList());

        return StrUtils.join(splitList, ',');
    }
}
