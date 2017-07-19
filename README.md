# Vector Scoring Plugin for Solr : Dot Product and Cosine Similarity

With this plugin you can query documents with vectors and score them based on dot product or cosine similarity.
This plugin is the same as [Vector Scoring Plugin for Elasticsearch](https://github.com/MLnick/elasticsearch-vector-scoring).

## Plugin installation

The plugin was developed and tested on Solr `6.6.0`.

1. Copy VectorPlugin.jar to {solr.install.dir}/dist/plugins/
2. Add the library to solrconfig.xml file:
```
<lib dir="${solr.install.dir:../../../..}/dist/plugins/" regex=".*\.jar" />
```
3. Add the plugin Query parser to solrconfig.xml:
```
<queryParser name="vp" class="com.github.saaay71.solr.VectorQParserPlugin" />
```
4. Add the fieldType `VectorField` to schema file(managed-schema):
```
  <fieldType name="VectorField" class="solr.TextField" indexed="true" termOffsets="true" stored="true" termPayloads="true" termPositions="true" termVectors="true" storeOffsetsWithPositions="true">
    <analyzer>
      <tokenizer class="solr.WhitespaceTokenizerFactory"/>
      <filter class="solr.DelimitedPayloadTokenFilterFactory" encoder="float"/>
    </analyzer>
  </fieldType>
```
4. Add the field `vector` to schema file:
```
<field name="vector" type="VectorField" indexed="true" termOffsets="true" stored="true" termPositions="true" termVectors="true" multiValued="true"/>
``
You can change the field name if desired.
5. Start Solr!

## Example

### Add example documents

```sh
curl -X POST -H "Content-Type: application/json" http://localhost:8983/solr/{your-collection-name}/update?commit=true  --data-binary '
[
    {"name":"example 0", "vector":"0|1.55 1|3.53 2|2.3 3|0.7 4|3.44 5|2.33 "},
    {"name":"example 1", "vector":"0|3.54 1|0.4 2|4.16 3|4.88 4|4.28 5|4.25 "},
    {"name":"example 2", "vector":"0|1.11 1|0.6 2|1.47 3|1.99 4|2.91 5|1.01 "},
    {"name":"example 3", "vector":"0|0.06 1|4.73 2|0.29 3|1.27 4|0.69 5|3.9 "},
    {"name":"example 4", "vector":"0|4.01 1|3.69 2|2 3|4.36 4|1.09 5|0.1 "},
    {"name":"example 5", "vector":"0|0.64 1|3.95 2|1.03 3|1.65 4|0.99 5|0.09 "}
]'
```

### Query documents
Open your browser and copy the links
Query 1
```
http://localhost:8983/solr/{your-collection-name}/query?fl=name,score,vector&q={!vp f=vector vector="0.1,4.75,0.3,1.2,0.7,4.0"}
```

You should see the following result:
```
{
  "responseHeader":{
    "status":0,
    "QTime":1,
    "params":{
      "q":"{!myqp f=vector vector=\"0.1,4.75,0.3,1.2,0.7,4.0\"}",
      "fl":"name,score,vector"}},
  "response":{"numFound":6,"start":0,"maxScore":0.99984086,"docs":[
      {
        "name":["example 3"],
        "vector":["0|0.06 1|4.73 2|0.29 3|1.27 4|0.69 5|3.9 "],
        "score":0.99984086},
      {
        "name":["example 0"],
        "vector":["0|1.55 1|3.53 2|2.3 3|0.7 4|3.44 5|2.33 "],
        "score":0.7693964},
      {
        "name":["example 5"],
        "vector":["0|0.64 1|3.95 2|1.03 3|1.65 4|0.99 5|0.09 "],
        "score":0.76322395},
      {
        "name":["example 4"],
        "vector":["0|4.01 1|3.69 2|2 3|4.36 4|1.09 5|0.1 "],
        "score":0.5328145},
      {
        "name":["example 1"],
        "vector":["0|3.54 1|0.4 2|4.16 3|4.88 4|4.28 5|4.25 "],
        "score":0.48513117},
      {
        "name":["example 2"],
        "vector":["0|1.11 1|0.6 2|1.47 3|1.99 4|2.91 5|1.01 "],
        "score":0.44909418}]
  }}
```
Query 2
Adding the parameter `cosine=false` calculates the dot product
```
http://localhost:8983/solr/{your-collection-name}/query?fl=name,score,vector&q={!vp f=vector vector="0.1,4.75,0.3,1.2,0.7,4.0" cosine=false}
```

result of query 2:
```

{
  "responseHeader":{
    "status":0,
    "QTime":0,
    "params":{
      "q":"{!myqp f=vector vector=\"0.1,4.75,0.3,1.2,0.7,4.0\" cosine=false}",
      "fl":"name,score,vector"}},
  "response":{"numFound":6,"start":0,"maxScore":40.1675,"docs":[
      {
        "name":["example 3"],
        "vector":["0|0.06 1|4.73 2|0.29 3|1.27 4|0.69 5|3.9 "],
        "score":40.1675},
      {
        "name":["example 0"],
        "vector":["0|1.55 1|3.53 2|2.3 3|0.7 4|3.44 5|2.33 "],
        "score":30.180502},
      {
        "name":["example 1"],
        "vector":["0|3.54 1|0.4 2|4.16 3|4.88 4|4.28 5|4.25 "],
        "score":29.354},
      {
        "name":["example"],
        "vector":["0|4.01 1|3.69 2|2 3|4.36 4|1.09 5|0.1 "],
        "score":24.923502},
      {
        "name":["example"],
        "vector":["0|0.64 1|3.95 2|1.03 3|1.65 4|0.99 5|0.09 "],
        "score":22.1685},
      {
        "name":["example"],
        "vector":["0|1.11 1|0.6 2|1.47 3|1.99 4|2.91 5|1.01 "],
        "score":11.867001}]
  }}
```

Query 3
Quering on other fields and with vector scoring.
```
http://localhost:8983/solr/{your-collection-name}/query?fl=name,score,vector&q={!vp f=vector vector="0.1,4.75,0.3,1.2,0.7,4.0" cosine=false}name="example 2","example 4"
```

result of query 3:
```
{
  "responseHeader":{
    "status":0,
    "QTime":1,
    "params":{
      "q":"{!myqp f=vector vector=\"0.1,4.75,0.3,1.2,0.7,4.0\" cosine=false}name=\"example 2\",\"example 4\"",
      "fl":"name,score,vector"}},
  "response":{"numFound":2,"start":0,"maxScore":24.923502,"docs":[
      {
        "name":["example 4"],
        "vector":["0|4.01 1|3.69 2|2 3|4.36 4|1.09 5|0.1 "],
        "score":24.923502},
      {
        "name":["example 2"],
        "vector":["0|1.11 1|0.6 2|1.47 3|1.99 4|2.91 5|1.01 "],
        "score":11.867001}]
  }}
```
