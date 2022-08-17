package com.jillesvangurp.ktsearch

import com.jillesvangurp.jsondsl.JsonDsl
import com.jillesvangurp.jsondsl.PropertyNamingConvention
import com.jillesvangurp.jsondsl.json
import com.jillesvangurp.jsondsl.withJsonDsl
import kotlinx.serialization.json.JsonObject
import kotlin.time.Duration

class ILMActions(): JsonDsl(namingConvention = PropertyNamingConvention.ConvertToSnakeCase) {
    fun rollOver(maxPrimaryShardSizeGb: Int) {
        this["rollover"] = withJsonDsl {
            this["max_primary_shard_size"] = "${maxPrimaryShardSizeGb}gb"
        }
    }

    fun shrink(numberOfShards: Int) {
        this["shrink"] = withJsonDsl {
            this["number_of_shards"] = numberOfShards
        }
    }

    fun forceMerge(numberOfSegments: Int) {
        this["forcemerge"] = withJsonDsl {
            this["max_num_segments"] = numberOfSegments
        }
    }

    fun delete() {
        this["delete"] = JsonDsl()
    }
}

class ILMPhaseConfiguration(): JsonDsl(namingConvention = PropertyNamingConvention.ConvertToSnakeCase) {
    var minAge by property<String>()
    fun minAge(duration: Duration) {
        val m = duration.inWholeMinutes
        minAge = when {
            m>60*24 -> {
                "${duration.inWholeDays}d"
            }
            m>60 -> {
                "${duration.inWholeHours}h"
            }
            else -> {
                "${duration.inWholeSeconds}s"
            }
        }
    }
    var actions by property(defaultValue = ILMActions())

    fun actions(block: ILMActions.() -> Unit) {
        actions.apply(block)
    }

}

class IMLPhases : JsonDsl(namingConvention = PropertyNamingConvention.ConvertToSnakeCase) {
    fun hot(block: ILMPhaseConfiguration.()->Unit) {
        this["hot"] = ILMPhaseConfiguration().apply(block)
    }
    fun warm(block: ILMPhaseConfiguration.()->Unit) {
        this["warm"] = ILMPhaseConfiguration().apply(block)
    }
    fun cold(block: ILMPhaseConfiguration.()->Unit) {
        this["cold"] = ILMPhaseConfiguration().apply(block)
    }
    fun frozen(block: ILMPhaseConfiguration.()->Unit) {
        this["frozen"] = ILMPhaseConfiguration().apply(block)
    }
    fun delete(block: ILMPhaseConfiguration.()->Unit) {
        this["delete"] = ILMPhaseConfiguration().apply(block)
    }
}

class IMLPolicy : JsonDsl(namingConvention = PropertyNamingConvention.ConvertToSnakeCase) {
    var phases by property<IMLPhases>(defaultValue = IMLPhases())
}

class ILMConfiguration(): JsonDsl(namingConvention = PropertyNamingConvention.ConvertToSnakeCase) {
    var policy by property<IMLPolicy>(defaultValue = IMLPolicy())
}

suspend fun SearchClient.setIlmPolicy(policyId: String, block: IMLPhases.()->Unit): JsonObject {
    validateEngine(
        "ilm only works on Elasticsearch",
        SearchEngineVariant.ES7,
        SearchEngineVariant.ES8
    )

    val config = ILMConfiguration()
    config.policy.phases.apply(block)

    return restClient.put {
        path("_ilm","policy", policyId)
        body = config.json(true)
    }.parseJsonObject()
}

suspend fun SearchClient.deleteIlmPolicy(policyId: String): JsonObject {
    return restClient.delete {
        path("_ilm","policy", policyId)
    }.parseJsonObject()
}

suspend fun SearchClient.getIlmPolicy(policyId: String): JsonObject {
    return restClient.get {
        path("_ilm","policy", policyId)
    }.parseJsonObject()
}