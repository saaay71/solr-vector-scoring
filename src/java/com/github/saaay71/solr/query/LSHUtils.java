package com.github.saaay71.solr.query;

import java.util.Locale;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class LSHUtils {

    public static Stream<String> getLSHStringStream(int[] lshInts) {
        return IntStream.range(0, lshInts.length).mapToObj(x -> (String.format(Locale.ROOT, "%d_%d", x, lshInts[x])));
    }
}
