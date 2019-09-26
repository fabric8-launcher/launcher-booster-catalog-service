package io.fabric8.launcher.booster.catalog.utils

import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

fun readCatalog(catalogJson: Path): List<Map<String, Any?>> {
    val parser = JSONParser()
    Files.newBufferedReader(catalogJson).use { reader ->
        val catalog = parser.parse(reader) as JSONArray
        return catalog as List<Map<String, Any?>>
    }
}

fun readMetadata(metadataJson: Path): Map<String, Any?> {
    val parser = JSONParser()
    Files.newBufferedReader(metadataJson).use { reader ->
        val metadata = parser.parse(reader) as JSONObject
        return metadata as Map<String, Any?>
    }
}
