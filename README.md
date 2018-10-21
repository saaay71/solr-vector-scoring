# Vector Scoring Plugin for Solr : Dot Product and Cosine Similarity

With this plugin you can query documents with vectors and score them based on dot product or cosine similarity.
This plugin is the same as [Vector Scoring Plugin for Elasticsearch](https://github.com/MLnick/elasticsearch-vector-scoring).

## Plugin installation

The plugin was developed and tested on Solr `7.4.0`.

1. Copy VectorPlugin.jar to {solr.install.dir}/dist/plugins/
2. Add the library to solrconfig.xml file:
```
<lib dir="${solr.install.dir:../../../..}/dist/plugins/" regex=".*\.jar" />
```
3. Add the plugin Query parser to solrconfig.xml:
```
<queryParser name="vp" class="com.github.saaay71.solr.query.VectorQParserPlugin" />
```
4. Add the fieldType `VectorField` to schema file(managed-schema):
```
      <fieldType name="VectorField" class="solr.BinaryField" stored="true" indexed="false" multiValued="false"/>
```
5. Add the field `vector` to schema file:
```
    <field name="_vector_" type="VectorField" />
    <field name="_lsh_hash_" type="string" indexed="true" stored="true" multiValued="true"/>
    <field name="vector" type="string" indexed="true" stored="true"/>
```

6. Add the LSH urp to solrconfig.xml
```
    <updateRequestProcessorChain name="LSH">
        <processor class="com.github.saaay71.solr.updateprocessor.LSHUpdateProcessorFactory" >
            <int name="seed">5</int>
            <int name="buckets">50</int>
            <int name="stages">50</int>
            <int name="dimensions">6</int>
            <str name="field">vector</str>
        </processor>
        <processor class="solr.RunUpdateProcessorFactory" />
    </updateRequestProcessorChain>
```

7. Start Solr!

## Example

### Add example documents

```sh
curl -X POST -H "Content-Type: application/json" http://localhost:8983/solr/{your-collection-name}/update?update.chain=LSH&commit=true  --data-binary '
[
    {"id":"1", "vector":"1.55,3.53,2.3,0.7,3.44,2.33"},
    {"id":"2", "vector":"3.54,0.4,4.16,4.88,4.28,4.25"}
]'
```

### Query documents
Open your browser and copy the links
#### Query 1
```
http://localhost:8983/solr/{your-collection-name}/query?fl=name,score,vector&q={!vp f=vector vector=\"1.55,3.53,2.3,0.7,3.44,2.33\" lsh=\"true\" topNDocs=\"5\"}&fl=name,score,vector,_vector_,_lsh_hash_
```

You should see the following result:
```
{
  "responseHeader":{
    "status":0,
    "QTime":8,
    "params":{
      "q":"{!vp f=vector vector=\"1.55,3.53,2.3,0.7,3.44,2.33\" lsh=\"true\" topNDocs=\"5\"}",
      "fl":"id, score, vector, _vector_, _lsh_hash_",
      "wt":"xml"}},
  "response":{"numFound":1,"start":0,"maxScore":36.65736,"docs":[
      {
        "id": "1",
        "vector":"1.55,3.53,2.3,0.7,3.44,2.33",
        "_vector_":"/z/GZmZAYeuFQBMzMz8zMzNAXCj2QBUeuA==",
        "_lsh_hash_":["0_8",
          "1_35",
          "2_7",
          "3_10",
          "4_2",
          "5_35",
          "6_16",
          "7_30",
          "8_27",
          "9_12",
          "10_7",
          "11_32",
          "12_48",
          "13_36",
          "14_10",
          "15_7",
          "16_42",
          "17_5",
          "18_3",
          "19_2",
          "20_1",
          "21_0",
          "22_24",
          "23_18",
          "24_42",
          "25_31",
          "26_35",
          "27_8",
          "28_1",
          "29_24",
          "30_47",
          "31_14",
          "32_22",
          "33_39",
          "34_0",
          "35_34",
          "36_34",
          "37_39",
          "38_27",
          "39_27",
          "40_45",
          "41_10",
          "42_21",
          "43_34",
          "44_41",
          "45_9",
          "46_31",
          "47_0",
          "48_4",
          "49_43"],
        "score":36.65736}
      ]
  }
}
```

#### Query 2
Quering on other fields and with vector scoring.
```
http://localhost:8983/solr/{your-collection-name}/query?fl=name,score,vector&q={!vp f=vector vector=\"3.54,0.4,4.16,4.88,4.28,4.25\" lsh=\"true\" topNDocs=\"5\" v=\"id:2\"}&fl=name,score,vector,_vector_,_lsh_hash_
```
or
```
http://localhost:8983/solr/{your-collection-name}/query?fl=name,score,vector&q={!vp f=vector vector=\"3.54,0.4,4.16,4.88,4.28,4.25\" lsh=\"true\" topNDocs=\"5\"} id:2&fl=name,score,vector,_vector_,_lsh_hash_
```

result of query 2:
```
{
  "responseHeader":{
    "status":0,
    "QTime":9,
    "params":{
      "q":"{!vp f=vector vector=\"3.54,0.4,4.16,4.88,4.28,4.25\" lsh=\"true\" topNDocs=\"5\" v=\"id:2\"}",
      "fl":"name, score, vector, _vector_, _lsh_hash_",
      "wt":"xml"}},
  "response":{"numFound":1,"start":0,"maxScore":38.649788,"docs":[
      {
        "id": "2",
        "vector":"3.54,0.4,4.16,4.88,4.28,4.25",
        "_vector_":"/0Bij1w+zMzNQIUeuECcKPZAiPXDQIgAAA==",
        "_lsh_hash_":["0_2",
          "1_33",
          "2_39",
          "3_49",
          "4_38",
          "5_12",
          "6_21",
          "7_26",
          "8_44",
          "9_25",
          "10_15",
          "11_25",
          "12_24",
          "13_8",
          "14_22",
          "15_43",
          "16_1",
          "17_17",
          "18_14",
          "19_1",
          "20_26",
          "21_47",
          "22_15",
          "23_36",
          "24_21",
          "25_41",
          "26_32",
          "27_35",
          "28_13",
          "29_4",
          "30_2",
          "31_39",
          "32_19",
          "33_36",
          "34_15",
          "35_30",
          "36_17",
          "37_0",
          "38_39",
          "39_32",
          "40_5",
          "41_1",
          "42_33",
          "43_0",
          "44_32",
          "45_21",
          "46_23",
          "47_4",
          "48_24",
          "49_16"],
        "score":38.649788}]
  }
}
```
