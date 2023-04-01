# KT Search Client 

[![matrix-test-and-deploy-docs](https://github.com/jillesvangurp/kt-search/actions/workflows/deploy-docs-and-test.yml/badge.svg?branch=master)](https://github.com/jillesvangurp/kt-search/actions/workflows/deploy-docs-and-test.yml)

Kt-search is a Kotlin Multi Platform library to search across the Opensearch and Elasticsearch ecosystem on any platform that kotlin can compile to. It provides Kotlin DSLs for querying, defining mappings, bulk indexing, index templates, index life cycle management, index aliases, and much more. The key goal for this library is to provide a best in class developer experience for using Elasticsearch and Opensearch.  

## License

This project is [licensed](LICENSE) under the MIT license.

## Learn more

- **[Manual](https://jillesvangurp.github.io/kt-search/manual)** - this is generated from the `docs` module. Just like this README.md file. The manual covers most of the extensive feature set of this library. Please provide feedback via the issue tracker if something is not clear to you.
- [API Documentation](https://jillesvangurp.github.io/kt-search/api/). Dokka documentation.
- [Release Notes](https://github.com/jillesvangurp/kt-search/releases)
- You can also learn a lot by looking at the integration tests in the `search-client` module.
- The code sample below should help you figure out the basics.

## Use cases

Integrate **advanced search** capabilities in your Kotlin applications. Whether you want to build a web based dashboard, an advanced ETL pipeline or simply expose a search endpoint as a microservice, this library has you covered. 

- Add search functionality to your server applications. Kt-search works great with **Spring Boot**, Ktor, Quarkus, and other popular JVM based servers. Simply create your client as a singleton object and inject it wherever you need search.
- Build complicated ETL functionality using the Bulk indexing DSL.
- Use Kt-search in a **Kotlin-js** based web application to create **dashboards**, or web applications that don't need a separate server. See our [Full Stack at FORMATION](https://github.com/formation-res/kt-fullstack-demo) demo project for an example.
- For dashboards and advanced querying, aggregation support is key and kt-search provides great support for that and makes it really easy to deal with complex nested aggregations.
- Use **Kotlin Scripting** to operate and introspect your cluster. See the companion project [kt-search-kts](https://github.com/jillesvangurp/kt-search-kts/) for more on this as well as the scripting section in the [Manual](https://jillesvangurp.github.io/kt-search/manual/Scripting.html). The companion library combines `kt-search` with `kotlinx-cli` for command line argument parsing and provides some example scripts; all with the minimum of boiler plate.
- Use kt-search from a **Jupyter Notebook** with the Kotlin kernel. See the `jupyter-example` directory for an example and check the [Manual](https://jillesvangurp.github.io/kt-search/manual/Jupyter.html) for instructions.

The goal for kt-search is to be the **most convenient way to use opensearch and elasticsearch from Kotlin** on any platform where Kotlin is usable.

Kt-search is extensible and modular. You can easily add your own custom DSLs for e.g. things not covered by this library or custom plugins you use. And while it is opinionated about using e.g. kotlinx.serialization, you can also choose to use alternative serialization frameworks, or even use your own http client and just use the search-dsl.

## Gradle

Add the `maven.tryformation.com` repository:

```kotlin
repositories {
    mavenCentral()
    maven("https://maven.tryformation.com/releases") {
        content {
            includeGroup("com.jillesvangurp")
        }
    }
}
```

And then the dependency:

```kotlin
    // check the latest release tag for the latest version
    implementation("com.jillesvangurp:search-client:2.x.y")
```

**IMPORTANT** We've switched maven repositories a couple of times now. Recently we switched back from jitpack.io to using our own repository. Jitpack is just too flaky for us to depend on and somehow they keep on having regressions with kotlin multi-platform projects.

**This also means the groupId has changed**. It's now `com.jillesvangurp` instead of `com.github.jillesvangurp.kt-search`.

I of course would like to get this on maven central eventually. However, I've had a really hard time getting that working and am giving up on that for now. The issue seems to be that I always hit some weird and very unspecific error and their documentation + plugins just never seem to quite work as advertised. Multi platform, multi module, and kotlin scripting are three things that tend to make things complicated apparently. If anyone wants to support me with this, please reach out. Otherwise use our private repository for now.

## Usage

```kotlin
val client = SearchClient(
  KtorRestClient()
)
```

First we create a client. Kotlin has default values for parameters. So, we use sensible defaults for the 
`host` and `port` variables to connect to `localhohst` and `9200`. You can also configure multiple hosts, 
or add ssl and basic authentication to connect to managed Opensearch or Elasticsearch clusters. If you use
multiple hosts, you can also configure a strategy for selecting the host to connect to. For more on 
this, read the [manual](https://jillesvangurp.github.io/kt-search/manual/GettingStarted.html).

```kotlin
@Serializable
data class TestDocument(
  val name: String,
  val tags: List<String>? = null
)
```

In the example below we will use this `TestDocument`, which we can serialize using the 
[kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) 
framework. You can also pass in your own serialized json in requests, so if you want to use e.g. jackson or gson,
you can do so easily.

```kotlin
val indexName = "readme-index"

// create an index and use our mappings dsl
client.createIndex(indexName) {
  settings {
    replicas = 0
    shards = 3
  }
  mappings(dynamicEnabled = false) {
    text(TestDocument::name)
    keyword(TestDocument::tags)
  }
}
```

This creates the index and uses the mappings and settings DSL. With this DSL, you can map fields, 
configure analyzers, etc. 
You can read more about that [here](https://jillesvangurp.github.io/kt-search/manual/IndexManagement.html) 

To fill the index with some content, we need to use bulk operations.

In kt-search this is made very easy with a DSL that abstracts away the book keeping
that you need to do for this. The bulk block below creates a BulkSession, which flushes
operations to Elasticsearch for you. You can configure and tailor how this works via parameters
that have sensible defaults. For example the number of operations that is flushes is something
that you'd want to probably configure.

The refresh parameter uses WaitFor as a default. This means that after the block exits, the documents
will have been indexed and are available for searching. 

```kotlin
client.bulk(refresh = Refresh.WaitFor) {
  index(
    doc = TestDocument(
      name = "apple",
      tags = listOf("fruit")
    ),
    index = indexName
  )
  index(
    doc = TestDocument(
      name = "orange",
      tags = listOf("fruit", "citrus")
    ),
    index = indexName,
  )
  index(
    // you can also provide raw json
    // but it has to be a single line in the bulk request
    source =
    """{"name":"banana","tags":["fruit","tropical"]}""",
    index = indexName
  )
}
```

You can read more about 
[bulk operations](https://jillesvangurp.github.io/kt-search/manual/BulkIndexing.html) in the manual.

Now that we have some documents in an index, we can do some queries:

```kotlin
// search for some fruit
val results = client.search(indexName) {
  query = bool {
    must(
      // note how we use property references here
      term(TestDocument::tags, "fruit"),
      matchPhrasePrefix(TestDocument::name, "app")
    )
  }
}

println("found ${results.total} hits")
results
  // extension function that deserializes
  // uses kotlinx.serialization
  .parseHits<TestDocument>()
  .first()
  // hits don't always include source
  // in that case it will be a null document
  .let {
    println("doc ${it.name}")
  }
// you can also get the JsonObject if you don't
// have a model class
println(results.hits?.hits?.first()?.source)
```

Captured Output:

```
found 1 hits
doc apple
{"name":"apple","tags":["fruit"]}

```

Aggregations are also supported via a DSL:

```kotlin
val resp = client.search(indexName) {
  // we don't care about retrieving hits
  resultSize = 0
  agg("by-tag", TermsAgg(TestDocument::tags) {
    aggSize = 50
    minDocCount = 1
  })
}
// picking the results apart is just as easy.
resp.aggregations
  .termsResult("by-tag")
  .parsedBuckets.forEach { bucket ->
    println("${bucket.parsed.key}: ${bucket.parsed.docCount}")
  }
```

Captured Output:

```
fruit: 3
citrus: 1
tropical: 1

```

These examples show off a few nice features of this library:

- Kotlin DSLs are nice, type safe, and easier to read and write than pure Json. And of course
you get auto completion too. There are DSLs for searching, creating indices and mappings, datastreams, 
index life cycle management, bulk operations, aggregations, and more. 
- Where in JSON, you use a lot of String literals, kt-search actually allows you to use
 property references or enum values. So refactoring your data model doesn't 
 break your mappings and queries.
- Kt-search makes complicated features like bulk operations, aggregations, etc. really easy 
to use and accessible.
- While a DSL is nice to have, sometimes it just doesn't have the feature you 
need or maybe you want to work with raw json. Kt-search allows you to do both and mix 
schema less with type safe kotlin. You can add custom 
properties to the DSL via `put` or you can use Kotlin string literals to pass in and template
raw json.
- Kt-search is designed to be [extensible](https://jillesvangurp.github.io/kt-search/manual/ExtendingTheDSL.html). 
It's easy to use the built in features. But you 
can easily add your own features. 

## Manual

There are of course a lot more features that this library supports. The 
[manual](https://jillesvangurp.github.io/kt-search/manual) covers all of those.

### Introduction

- [What is Kt-Search](https://jillesvangurp.github.io/kt-search/manual/WhatIsKtSearch.html)

- [Getting Started](https://jillesvangurp.github.io/kt-search/manual/GettingStarted.html)

- [Indices, Settings, Mappings, and Aliases](https://jillesvangurp.github.io/kt-search/manual/IndexManagement.html)

### Search

- [Search and Queries](https://jillesvangurp.github.io/kt-search/manual/Search.html)

- [Text Queries](https://jillesvangurp.github.io/kt-search/manual/TextQueries.html)

- [Term Level Queries](https://jillesvangurp.github.io/kt-search/manual/TermLevelQueries.html)

- [Compound Queries](https://jillesvangurp.github.io/kt-search/manual/CompoundQueries.html)

- [Geo Spatial Queries](https://jillesvangurp.github.io/kt-search/manual/GeoQueries.html)

- [Aggregations](https://jillesvangurp.github.io/kt-search/manual/Aggregations.html)

- [Deep Paging Using search_after and scroll](https://jillesvangurp.github.io/kt-search/manual/DeepPaging.html)

### Indices and Documents

- [Deleting by query](https://jillesvangurp.github.io/kt-search/manual/DeleteByQuery.html)

- [Document Manipulation](https://jillesvangurp.github.io/kt-search/manual/DocumentManipulation.html)

- [Index Repository](https://jillesvangurp.github.io/kt-search/manual/IndexRepository.html)

- [Efficiently Ingest Content Using Bulk Indexing](https://jillesvangurp.github.io/kt-search/manual/BulkIndexing.html)

- [Creating Data Streams](https://jillesvangurp.github.io/kt-search/manual/DataStreams.html)

### Advanced Topics

- [Migrating from the old Es Kotlin Client](https://jillesvangurp.github.io/kt-search/manual/Migrating.html)

- [Extending the Json DSLs](https://jillesvangurp.github.io/kt-search/manual/ExtendingTheDSL.html)

- [Using Kotlin Scripting](https://jillesvangurp.github.io/kt-search/manual/Scripting.html)

- [Jupyter Notebooks](https://jillesvangurp.github.io/kt-search/manual/Jupyter.html)

## Related projects

There are several libraries that build on kt-search:

- [jillesvangurp/kt-search](https://github.com/jillesvangurp/kt-search) - the main Github project for kt-search.
- [kt-search-kts](https://github.com/jillesvangurp/kt-search-kts) - this library combines `kt-search` with `kotlinx-cli` to make scripting really easy. Combined with the out of the box support for managing snapshots, creating template mappings, bulk indexing, data-streams, etc. this is the perfect companion to script all your index operations. Additionally, it's a great tool to e.g. query your data, or build some health checks against your production indices.
- [kt-search-logback-appender](https://github.com/jillesvangurp/kt-search-logback-appender) - this is a logback appender that bulk indexes log events straight to elasticsearch.
- [es-kotlin-client](https://github.com/jillesvangurp/es-kotlin-client) - version 1 of this client; now no longer maintained.

## Setting up a development environment

Any recent version of Intellij should be able to import this project as is. 
This project uses docker for testing and to avoid having the tests create a 
mess in your existing elasticsearch cluster, it uses a different port than
the default Elasticsearch port.

If you want to save some time while developing, it helps to start docker manually.

```bash
docker-compose -f docker-compose-es-8.yml up -d
```

For additional details, refer to the build file.

## Compatibility

The integration tests on GitHub Actions use a **matrix build** that tests everything against Elasticsearch 7 & 8 and Opensearch 1 & 2.

It may work fine with earlier Elasticsearch versions as well. But we don't actively test this and the tests are known to not pass with Elasticsearch 6 due to some changes in the mapping dsl. You may be able to work around some of this, however.

There is an annotation that is used to restrict APIs when needed. E.g. `search-after` only works with Elasticsearch and Opensearch 2 and has the following annotation to indicate that:

```kotlin
@VariantRestriction(SearchEngineVariant.ES7,SearchEngineVariant.ES8)
suspend fun SearchClient.searchAfter(target: String, keepAlive: Duration, query: SearchDSL): Pair<SearchResponse,Flow<SearchResponse.Hit>> {
    validateEngine("search_after does not work on OS1",
        SearchEngineVariant.OS2,
        SearchEngineVariant.ES7, 
        SearchEngineVariant.ES8)

    // ...
}
```

The annotation is informational only for now. In our tests, we use `onlyon` to prevent tests from
failing on unsupported engines For example, this is added to the test for `search_after`:

```kotlin
onlyOn("opensearch implemented search_after with v2",
    SearchEngineVariant.OS2,
    SearchEngineVariant.ES7,
    SearchEngineVariant.ES8)
```

## Module Overview

This repository contains several kotlin modules that each may be used independently.

| Module          | Description                                                                                                                               |
|-----------------|-------------------------------------------------------------------------------------------------------------------------------------------|
| `json-dsl`      | Kotlin DSL for creating json requests                                                                                                     |
| `search-dsls`   | DSLs for search and mappings based on `json-dsl`.                                                                                         |
| `search-client` | Multiplatform REST client for Elasticsearch 7 & 8 and Opensearch 1.                                                                       |
| `docs`          | Contains the code that generates the [manual](https://jillesvangurp.github.io/kt-search/manual/) and readmes.                             |

The search client module is the main module of this library. I extracted the json-dsl module and `search-dsls` module with the intention of eventually moving these to separate libraries. Json-dsl is actually useful for pretty much any kind of json dialect and I have a few APIs in mind where I might like to use it. The choice to not impose kotlinx.serialization on json dsl also means that both that and the search dsl are really portable and only depend on the Kotlin standard library.

## Contributing

Pull requests are very welcome! Please consider communicating your intentions in advance to avoid conflicts, or redundant work.

Some suggestions of things you could work on:

- Extend the mapping or query DSLs. Our goal is to have coverage of all the common things we and other users need. The extensibility of `JsonDsl` always gives you the option to add whatever is not directly supported by manipulating the underlying map. But creating extension functions that do this properly is not har.
- Add more API support for things in Opensearch/Elasticsearch that are not currently supported. The REST api has dozens of end point other than search. Like the DSL, adding extension functions is easy and using the underlying rest client allows you to customize any requests.
- Work on one of the issues or suggest some new ones.

## Support and Community

Please file issues if you find any or have any reasonable suggestions for changes.

Within reason, I can help with simple issues. Beyond that, I can offer my services as a consultant as well if you need some more help with getting started or just using Elasticsearch/Opensearch in general with just about any tech stack. I can help with discovery projects, trainings, architecture analysis, query and mapping optimizations, or just generally help you get the most out of your search setup and your product roadmap.

You can reach me via the issue tracker and I also lurk in the amazing [Kotlin Slack](https://kotlinlang.org/community/), [Elastic Slack](https://www.elastic.co/blog/join-our-elastic-stack-workspace-on-slack), and [Search Relevancy Slack](https://opensourceconnections.com/blog/2021/07/06/building-the-search-community-with-relevance-slack/) communities. And I have a [website](https://www.jillesvangurp.com) with more contact details.

## About this README

This readme is generated using my [kotlin4example](https://github.com/jillesvangurp/kotlin4example) library. I started developing that a few years ago when 
I realized that I was going to have to write a lot of documentation with code examples for kt-search. By now,
both the manual and this readme heavily depend on this and it makes maintaining and adding documentation super easy. 

The way it works is that it provides a dsl for writing markdown that you use to write documentation. It allows you to include runnable code blocks and when it builds the documentation it figures out how to extract those from the kotlin source files and adds them as markdown code snippets. It can also intercept printed output and the return values of the blocks.

If you have projects of your own that need documentation, you might get some value out of using this as well. 



