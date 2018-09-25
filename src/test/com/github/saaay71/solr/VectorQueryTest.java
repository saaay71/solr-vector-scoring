package com.github.saaay71.solr;

import com.google.common.collect.Iterables;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.util.StrUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


public class VectorQueryTest extends SolrTestCaseJ4 {

    private static AtomicInteger idCounter = new AtomicInteger();
    private static String[] vectors = {
            "|1,1.55 |2,3.53 |3,2.3 |4,0.7 |5,3.44 |6,2.33",
            "|1,3.54 |2,0.4 |3,4.16 |4,4.88 |5,4.28 |6,4.25"
    };
    private static String[] denseVectors = {
            "|1.55 |3.53 |2.3 |0.7 |3.44 |2.33",
            "|3.54 |0.4 |4.16 |4.88 |4.28 |4.25"
    };
    private static Iterator<String> vectorsIter = Iterables.cycle(vectors).iterator();
    private static Iterator<String> denseVectorsIter = Iterables.cycle(denseVectors).iterator();


    @BeforeClass
    public static void beforeClass() throws Exception {
        initCore("solrconfig.xml", "schema-vector.xml");
    }

    @Before
    public void before() throws Exception {
        deleteByQueryAndGetVersion("*:*", params());
        idCounter.set(0);
    }

    @Test
    public void denseDataTest() throws Exception {
        System.out.println("test runs!");
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
    }

    @Test
    public void sparseDataTest() throws Exception {
        System.out.println("test runs!");
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
            assertU(adoc(sdoc("id", idCounter.incrementAndGet(), "vector", vectorsIter.next())));
        }
        assertU(commit());
    }

    private void indexSampleDenseData() throws Exception {
        for(int i = 0; i < 10; i++) {
            assertU(adoc(sdoc("id", idCounter.incrementAndGet(), "vector", denseVectorsIter.next())));
        }
        assertU(commit());
    }

    private String sparseToDenseVector(String sparseVec) {
        List<String> splitList = StrUtils.splitSmart(sparseVec.replaceAll("\\|", ""), ' ')
                .stream().map(x -> x.split(",")[1])
                .collect(Collectors.toList());

        return StrUtils.join(splitList, ',');
    }
}
