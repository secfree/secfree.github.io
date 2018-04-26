---
layout: blog
title: "Read CSV from Files and Write to Avro Format with Gobblin"
---

[Gobblin](http://gobblin.readthedocs.io/en/latest/) is a great tool for ETL. I want to restructure some data, converting CSV format files to Avro format.

The version of Gobblin I am using is 0.11.0 .

If the files located in a server's local filesystem, it's very easy. Just create a job configuration file like below.

```conf
source.class=gobblin.source.extractor.filebased.TextFileBasedSource
source.filebased.downloader.class=gobblin.source.extractor.filebased.CsvFileDownloader

source.filebased.fs.uri=file:///
source.schema=[{"columnName":"id","dataType":{"type":"int"}},{"columnName":"day","dataType":{"type":"string"}},{"columnName":"value","dataType":{"type":"double"}}]
source.skip.first.record=true
source.csv_file.delimiter=,
source.filebased.data.directory=/home/test/csv/

extract.table.type=append_only
extract.table.name=shop_metric
extract.namespace=test

converter.classes=gobblin.converter.csv.CsvToJsonConverterV2,gobblin.converter.avro.JsonIntermediateToAvroConverter

writer.destination.type=HDFS
writer.output.format=AVRO

data.publisher.type=gobblin.publisher.BaseDataPublisher
data.publisher.final.dir=/home/test/publish
```

The `source.schema` is needed to parse the CSV files.

But, if the files located in HDFS, the `TextFileBasedSource` class would not work. It only parses files with URI start with `file:///`.

In this case, it needs to custom the source class. Here's one kind of implementation.

- HadoopFileBasedSource.java

    ```java
    public abstract class HadoopFileBasedSource<S, D> extends FileBasedSource<S, D> {

        private static final Logger LOGGER = LoggerFactory.getLogger(HadoopFileBasedSource.class);
        @Override
        public List<String> getcurrentFsSnapshot(State state) {
            List<String> results = Lists.newArrayList();
            String path = state.getProp(ConfigurationKeys.SOURCE_FILEBASED_DATA_DIRECTORY);

            try {
                LOGGER.info("Running ls command with input " + path);
                results = this.fsHelper.ls(path);
            } catch (FileBasedHelperException e) {
                LOGGER.error("Not able to run ls command due to " + e.getMessage() + " will not pull any files", e);
            }
            return results;
        }
    }
    ```

- HadoopTextFileBasedSource.java

    ```java
    public class HadoopTextFileBasedSource extends HadoopFileBasedSource<String, String> {

        @Override
        public Extractor<String, String> getExtractor(WorkUnitState state) throws IOException {
            if (!state.contains(ConfigurationKeys.SOURCE_FILEBASED_OPTIONAL_DOWNLOADER_CLASS)) {
                state.setProp(ConfigurationKeys.SOURCE_FILEBASED_OPTIONAL_DOWNLOADER_CLASS,
                        TokenizedFileDownloader.class.getName());
            }
            return new FileBasedExtractor<>(state, new HadoopFsHelper(state));
        }

        @Override
        public void initFileSystemHelper(State state) throws FileBasedHelperException {
            this.fsHelper = new HadoopFsHelper(state);
            this.fsHelper.connect();
        }

        @Override
        protected String getLsPattern(State state) {
            return state.getProp(ConfigurationKeys.SOURCE_FILEBASED_DATA_DIRECTORY);
        }
    }
    ```
