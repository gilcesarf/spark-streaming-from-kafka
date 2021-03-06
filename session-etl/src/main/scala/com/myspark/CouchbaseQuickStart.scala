package com.myspark

import com.couchbase.client.java.document.JsonDocument
import com.couchbase.client.java.document.json.JsonObject
import org.apache.spark._
import com.couchbase.spark._

/**
  * Created by rans on 23/03/16.
  */
object CouchbaseQuickStart {
  def main(args: Array[String]): Unit = {
    val cfg = new SparkConf()
      .setAppName("couchbaseQuickstart") // give your app a name
      .setMaster("local[*]") // set the master to local for easy experimenting
      .set("com.couchbase.bucket.travel-sample", "") // open the travel-sample bucket

    // Generate The Context
    val sc = new SparkContext(cfg)

    //read a document by its id and upsert it
    sc
      .couchbaseGet[JsonDocument](Seq("airline_10123", "airline_10748"))
      .map(oldDoc => {
        val name = "my_" + oldDoc.content().get("name")
        val content = oldDoc.content().put("name", name)
        JsonDocument.create(oldDoc.id(), content)
      })
      .saveToCouchbase()
  }
}
