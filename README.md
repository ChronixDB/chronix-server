[![Build Status](https://travis-ci.org/ChronixDB/chronix.server.svg?branch=master)](https://travis-ci.org/ChronixDB/chronix.server)
[![Coverage Status](https://coveralls.io/repos/ChronixDB/chronix.server/badge.svg?branch=master&service=github)](https://coveralls.io/github/ChronixDB/chronix.server?branch=master)
[![Sputnik](https://sputnik.ci/conf/badge)](https://sputnik.ci/app#/builds/ChronixDB/chronix.server)
[![Apache License 2](http://img.shields.io/badge/license-ASF2-blue.svg)](https://github.com/ChronixDB/chronix.server/blob/master/LICENSE)
[ ![Download](https://api.bintray.com/packages/chronix/maven/chronix-server-client/images/download.svg) ](https://bintray.com/chronix/maven/chronix-server-client/_latestVersion)

# Chronix Server
The Chronix Server is an implementation of the Chronix API that stores time series in [Apache Solr](http://lucene.apache.org/solr/).
Chronix uses several techniques to optimize query times and storage demand.
Thus Chronix achieves on a benchmark asking serveral ranges (.5 day up to 180 days) an average runtime per range-query of 23 milliseconds.
The dataset contains about 3.7 billion pairs and takes 108 GB serialized as CSV.
Chronix needs only 8.7 GB to store the dataset.
Everything runs on a standard laptop computer.
No need of clustering, parallel processing or another complex stuff.
Check it out and give it a try.

The repository [chronix.examples](https://github.com/ChronixDB/chronix.examples) contains some examples.

## How Chronix Server stores time series
![Chronix Architecture](https://bintray.com/artifact/download/chronix/Images/chronix-architecture.jpg)

The key data type of Chronix is called a *record*.
It stores a chunk of time series data in a compressed binary large object.
The record also stores technical fields, time stamps for start and end, that describe the time range of the chunk of data, and a set of arbitrary user-defined attributes.
Storing records instead of individual pairs of time stamp and value has two major advantages:
1. A reduced storage demand due to compression 
2. Almost constant query times for accessing a chunk due to indexable attributes and a constant overhead for decompression.

The architecture of Chronix has the four building blocks shown in Figure.
It is well-suited to the parallelism of multi-core systems.
All blocks can work in parallel to each other to increase the throughput.
### Semantic Compression
Semantic Compression is **optional** and reduces the amount of time series with the goal of storing fewer records.
It uses techniques that exploit knowledge on the shape and the significance of a time series to remove irrelevant details even if some accuracy is lost, e.g. dimensionality reduction through aggregation.

### Attributes and Chunks
Attributes and Chunks breaks down time series into chunks of *n* data points that are serialized into *c* Bytes.
It also calculates the attributes and the pre-calculated values of the records.
Part of this serialization is a *Date-Delta Compaction* that compares the deltas between time stamps.
It serializes only the value if the aberration of two deltas is within a defined range, otherwise it writes both the time stamp and the value to the record's data field.

### Basic Compression
Then Basic Compression uses gzip, a lossless compression technique that operates on *c* consecutive bytes.
Only the record's data field is compressed to reduce the storage demand while the attributes remain uncompressed for access.
Compression of operational time series data yields a high compression rate due its value characteristics.
In spite of the decompression costs when accessing data, compression actually improves query times as data is processed faster.

### Multi-Dimensional Storage
The Multi-Dimensional Storage holds the records in a compressed binary format.
Only the fields that are necessary to locate the records are visible as so-called dimensions to the data storage system.
Queries can then use any combination of those dimensions to locate records.
Chronix uses Apache Solr as it ideally matches the requirements.
Furthermore Chronix has built-in analysis functions, e.g, a trend and outlier detector, to optimize operational time series analyses. 

## Data model
Chronix allows one to store any kind of time series and hence the data model is open to your needs.
Chronix Server per default uses the [Chronix Time Series](https://github.com/ChronixDB/chronix.timeseries) package.
The data model for the Chronix Time Series package.

A time series has at least the following required fields:

| Field Name  | Value Type |
| ------------- | ------------- |
| start      | Long    |
| end        | Long    |
| name     | String  |
| type     | String  |
| data       | Byte[]  |

The data field contains json serialized and gzip compressed points of time stamp (long) and numeric value (double).
Furthermore a time series can have arbitrary user-defined attributes. 
The type of an attribute is restricted by the available [fields](https://cwiki.apache.org/confluence/display/solr/Solr+Field+Types) of Apache Solr.

## Chronix Server Client ([Source](https://github.com/ChronixDB/chronix.server/tree/master/chronix-server-client))

A Java client that is used to store and stream time series from Chronix.
The following code snippet shows how to setup an connection to Chronix and stream time series.
The examples uses the [Chronix API](https://github.com/ChronixDB/chronix.api), Chronix Server Client, 
[Chronix Time Series](https://github.com/ChronixDB/chronix.timeseries) and [SolrJ](http://mvnrepository.com/artifact/org.apache.solr/solr-solrj/5.5.0)
```Java
//An connection to Solr
SolrClient solr = new HttpSolrClient("http://localhost:8983/solr/chronix/");

//Define a group by function for the time series records
Function<MetricTimeSeries, String> groupBy = ts -> ts.getName() + "-" + ts.attribute("host");

//Define a reduce function for the grouped time series records
BinaryOperator<MetricTimeSeries> reduce = (ts1, ts2) -> {
      MetricTimeSeries.Builder reduced = new MetricTimeSeries.Builder(ts1.getName(),ts1.getType())
            .points(concat(ts1.getTimestamps(), ts2.getTimestamps()),
                  concat(ts1.getValues(), ts2.getValues()))
            .attributes(ts1.attributes());
            return reduced.build();
        };

//Create a Chronix Client with a metric time series and the Chronix Solr Storage
ChronixClient<MetricTimeSeries,SolrClient,SolrQuery> chronix = 
                                          new ChronixClient<>(new MetricTimeSeriesConverter(),
                                          new ChronixSolrStorage<>(nrOfDocsPerBatch,groupBy,reduce));

//Lets stream time series from Chronix. We want the maximum of all time series that metric matches *load*.
SolrQuery query = new SolrQuery("name:*load*");
query.setParam("cf","metric{max}");

//The result is a Java Stream. We simply collect the result into a list.
List<MetricTimeSeries> maxTS = chronix.stream(solr, query).collect(Collectors.toList());
```

## Chronix Server Parts
The Chronix server parts are Solr extensions (e.g. a custom query handler).
Hence there is no need to build a custom modified Solr.
We just plug the Chronix server parts into a standard Solr.

The following sub projects are Solr extensions and ship with the binary release of Chronix.
The latest release of Chronix server is based on Apache Solr version 6.4.2

## Chronix Server Query Handler ([Source](https://github.com/ChronixDB/chronix.server/tree/master/chronix-server-query-handler))
The Chronix Server Query Handler is the entry point for requests asking for time series.
It splits a request based on the filter queries up in range or function queries:

- cf=<type>{function;function};<type>{function;function};... (for aggregations, analyses, or transformations)
- cf='' (empty, for range queries)

But before the Chronix Query Handler delegates a request, it modifies the user query string.
This is necessary as Chronix stores records and hence a query asking for a specific time range has to be modified.
As a result it converts a query:
```
host:prodI4 AND name:\\HeapMemory\\Usage\\Used AND start:NOW-1MONTH AND end:NOW-10DAYS
```
in the following query:
```
host:prodI4 AND name:\\HeapMemory\\Usage\\Used AND -start:[NOW-10DAYS-1ms TO *] AND -end:[* TO NOW-1MONTH-1ms]
```

### Range Query
A range query is answered using the default Solr query handler which supports all the great features (fields, facets, ...) of Apache Solr.

Example Result:
```
{
  "responseHeader":{
    "query_start_long":0,
    "query_end_long":9223372036854775807,
    "status":0,
    "QTime":3},
  "response":{"numFound":21,"start":0,"docs":[
      {
        "start":1377468017361,
        "name":"\\Load\\max",
        "end":1377554376850,
        "data":"byte[]" // serialized and compressed points
       },...
   ]
}
```

### Function Query
A custom query handler answers function queries.
Chronix determines if a query is a function query by using the filter query mechanism of Apache Solr.
There are three types of functions: Aggregations, Transformations, and High-level Analyses.

Currently the following functions are available:

(See the GPL2 branch that has more functions)

- Maximum (metric{max})
- Minimum (metric{min})
- Average (metric{avg})
- Standard Deviation (metric{dev})
- Percentiles (metric{p:[0.1,...,1.0]})
- Count (metric{count}) (*Release 0.2*)
- Sum (metric{sum}) (*Release 0.2*)
- Range (metric{range}) (*Release 0.2*)
- First/Last (metric{first/last}) (*Release 0.2*)
- Bottom/Top (metric{bottom/top:10}) (*Release 0.2*)
- Derivative (metric{derivative}) (*Release 0.2*)
- Non Negative Derivative (metric{nnderivative}) (*Release 0.2*)
- Difference (metric{diff}) (*Release 0.2*)
- Signed Difference (metric{sdiff}) (*Release 0.2*)
- Scale (metric{scale:0.5}) (*Release 0.2*)
- Divide (metric{divide:4}) (*Release 0.2*)
- Time window based Moving Average (metric{movavg:10,MINUTES}) (*Release 0.2*)
- Samples based Moving Average (metric{smovavg:10}) (*Release 0.4*)
- Add (metric{add:4}) (*Release 0.2*)
- Subtract (metric{sub:4}) (*Release 0.2*)
- A linear trend detection (metric{trend})
- Outlier detection (metric{outlier})
- Frequency detection (metric{frequency:10,6})
- Time series similarity search (metric{fastdtw:compare(metric=Load),1,0.8})
- Timeshift (metric{timeshift:[+/-]10,DAYS}) (*Release 0.3*)
- Distinct (metric{distinct}) (*Release 0.4*)
- Integral (metric{integral}) (*Release 0.4*)
- SAX (metric{sax:\*af\*,10,60,0.01})

Multiple analyses, aggregations, and transformations are allowed per query.
If so, Chronix will first execute the transformations in the order they occur.
Then it executes the analyses and aggregations on the result of the chained transformations.
For example the query:

```
cf=metric{max;min;trend;movavg:10,minutes;scale:4}
```

is executed as follows:

1. Calculate the moving average
2. Scale the result of the moving average by 4
3. Calculate the max, min, and the trend based on the prior result.
 
A function query does not return the raw time series data by default.
It returns all requested time series attributes, the analysis and its result.
With the enabled option ```fl=+data``` Chronix will return the data for the analyses.
The attributes are merged using a set to avoid duplicates.
For example a query for a metric that is collected on several hosts might return the following result:
```
{
  "responseHeader":{
    "query_start_long":0,
    "query_end_long":9223372036854775807,
    "status":0,
    "QTime":3},
  "response":{"numFound":21,"start":0,"docs":[
      {
        "start":1377468017361,
        "name":"\\Load\\max",
        "end":1377554376850,
        "host:"["host-1","host-2", ...]
       }...
   ]
}
```

A few example analyses:
```
q=name:*load* // Get all time series that metric name matches *load*

+ cf=metric{max} //Get the maximum of 
+ cf=metric{p:0.25} //To get the 25% percentile of the time series data
+ cf=metric{trend} //Returns all time series that have a positive trend
+ cf=metric{frequency=10,6} //Checks time frames of 10 minutes if there are more than 6 points. If true it returns the time series.
+ cf=metric{fastdtw(metric:*load*),1,0.8} //Uses fast dynamic time warping to search for similar time series
```

### Join Time Series Records
An query can include multiple records of time series and therefore Chronix has to know how to group records that belong together.
Chronix uses a so called *join function* that can use any arbitrary set of time series attributes to group records.
For example we want to join all records that have the same attribute values for host, process, and name:
```
cj=host,process,name
```
If no join function is defined Chronix applies a default join function that uses the name.

### Modify Chronix' response
Per default Chronix returns (as Solr does) all defined fields in the *schema.xml*.
One has three ways to modify the response using the *fl* parameter:

#### One specific user defined field
If only a specific user defined field is needed, e.g. the host field, one can set:
```
fl=host
```
Then Chronix will return the *host* field and the required fields (start,end,data,id).

#### Exclude a specific field
If one do not need a specific field, such as the data field, one can pass *-data* in the *fl* parameter.
```
fl=-data
``` 
In that case all fields, expect the data field, are returned.
Even when the excluded field is a required field.

#### Explicit return of a field
This is useful in combination with an analysis. 
Analyses per default do not return the raw data for performance reasons.
But if the raw data is needed, one can pass
```
fl=+data
```

### Chronix Response Writer
This allows one to query raw (uncompressed) data from Chronix in JSON format.
To execute the transformer you have to add it to the *fl* parameter:
```
q=name:*load*&fl=+dataAsJson //to get all fields and the dataAsJson field
q=name:*load*&fl=dataAsJson //to get only the required fields (except the data field) and dataAsJson
```
The records in the result contains a field called *dataAsJson* that holds the raw time series data as json.
Note: The data field that normally ship the compressed data is not included in the result.

Example Result:
```
{
  "responseHeader":{
    "query_start_long":0,
    "query_end_long":9223372036854775807,
    "status":0,
    "QTime":3},
  "response":{"numFound":21,"start":0,"docs":[
      {
        "start":1377468017361,
        "name":"\\Load\\max",
        "end":1377554376850,
        "dataAsJson":"[[timestamps],[values]]" //as json string
       }...
   ]
}
```
### Chronix Plug-ins
Chronix provides a plug-in mechanism to add user-defined types as well as function for types. 
#### Types
See the [Metric](https://github.com/ChronixDB/chronix.server/tree/master/chronix-server-type-metric/src/main/java/de/qaware/chronix/solr/type/metric) type for an example.


#### Functions
See the [NoOp](https://github.com/ChronixDB/chronix.server/tree/master/chronix-server-function-metric/src/main/java/de/qaware/chronix/solr/type/metric/functions/ext) funtion for metric types for an example.

We will provide more information in the new documentation of Chronix.

### Chronix Server Retention ([Source](https://github.com/ChronixDB/chronix.server/tree/master/chronix-server-retention))
The Chronix Server Retention plugin deletes time series data that is older than a given threshold.
The configuration of the plugin is within the *config.xml* of the Solr Core.
The following snippet of Solr config.xml shows the configuration:
```XML
<requestHandler name="/retention" class="de.qaware.chronix.solr.retention.ChronixRetentionHandler">
  <lst name="invariants">
   <!-- Use the end field of a record to determine its age. -->
   <str name="queryField">end</str>
   <!-- Delete time series that are older than 40DAYS -->
   <str name="timeSeriesAge">40DAYS</str> 
    <!-- Do it daily at 12 o'clock -->
   <str name="removeDailyAt">12</str>
   <!-- Define the source  -->
   <str name="retentionUrl">http://localhost:8983/solr/chronix/retention</str>
   <!-- Define how the index is updated after deletion -->
   <str name="optimizeAfterDeletion">false</str>
   <str name="softCommit">false</str>
  </lst>
</requestHandler>
```

## Usage
All libraries are available in the [Chronix Bintray Maven](https://bintray.com/chronix/maven) repository.
A build script snippet for use in all Gradle versions, using the Chronix Bintray Maven repository:
```groovy
repositories {
    mavenCentral()
    maven {
        url "http://dl.bintray.com/chronix/maven"
    }
}
dependencies {
   compile 'de.qaware.chronix:chronix-server-client:<currentVersion>'
   compile 'de.qaware.chronix:chronix-server-query-handler:<currentVersion>'
   compile 'de.qaware.chronix:chronix-server-retention:<currentVersion>'
}
```

## Contributing
Is there anything missing? Do you have ideas for new features or improvements? You are highly welcome to contribute
your improvements, to the Chronix projects. All you have to do is to fork this repository,
improve the code and issue a pull request.

## Building Chronix from Scratch
Everything should run out of the box. The only two things that must be available:
- Git
- JDK 1.8

Just do the following steps:

```bash
cd <checkout-dir>
git clone https://github.com/ChronixDB/chronix.server.git
cd chronix.server
./gradlew clean build
```

## Maintainer

Florian Lautenschlager @flolaut

## License

This software is provided under the Apache License, Version 2.0 license.

See the `LICENSE` file for details.
