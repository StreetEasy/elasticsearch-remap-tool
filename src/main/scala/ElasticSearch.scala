import scala.util.{Try, Failure}
import scala.collection.JavaConverters._
import com.typesafe.scalalogging.slf4j.Logging
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.common.transport._
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.action.admin.indices.flush.FlushRequest
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesResponse
import org.json4s.DefaultFormats
import org.json4s.JsonAST.JValue
import org.json4s.jackson.JsonMethods

object Elasticsearch extends Logging {

  protected implicit val jsonFormats = DefaultFormats

  val hostName = "localhost"

  private lazy val settings = ImmutableSettings.settingsBuilder()
    .put("cluster.name", "content-api")
    .put("client.transport.sniff", true)
    .build()

  lazy val client = new TransportClient(settings).addTransportAddress(new InetSocketTransportAddress(hostName, 9300))

  def indexExists(indexName: String) = client.admin.indices.prepareExists(indexName).execute.actionGet.isExists

  def createIndex(indexName: String, mappings: Option[JValue]) {
    logger.info(s"Creating index: $indexName")

    val index = client.admin.indices.prepareCreate(indexName)

    mappings foreach { m =>
      val contentMappings = JsonMethods.compact(m \ "content-mappings")
      val tagMappings = JsonMethods.compact(m \ "tag-mappings")
      val sectionMappings = JsonMethods.compact(m \ "section-mappings")
      val networkFrontMappings = JsonMethods.compact(m \ "network-front-mappings")
      val storyPackageMappings = JsonMethods.compact(m \ "story-package-mappings")

      index
        .addMapping("content", contentMappings)
        .addMapping("tag", tagMappings)
        .addMapping("section", sectionMappings)
        .addMapping("network-front", networkFrontMappings)
        .addMapping("story-package", storyPackageMappings)
    }

    index.execute.actionGet
  }

  def createAlias(alias: String, indexName: String) {
    logger.info(s"Adding alias: $alias")
    client.admin.indices.prepareAliases().addAlias(indexName, alias).execute().actionGet()
  }

  def migrate(
      fromIndex: String,
      toIndex:String,
      batchSize: Int,
      writeTimeOut: Long): Boolean = {

    logger.info(s"Migrating data from $fromIndex to $toIndex (batch size: $batchSize write timeout: $writeTimeOut)")

    val scrollTime = new TimeValue(60 * 1000 * 5) //setting to 5 minutes

    val query = client.prepareSearch(fromIndex)
      .setScroll(scrollTime)
      .setQuery(QueryBuilders.matchAllQuery())
      .setSize(batchSize)

    var scrollResp = query.execute().actionGet()

    val recordsToCopy = scrollResp.getHits.totalHits()
    var recordsCopied = 0

    while(scrollResp.getHits.asScala.nonEmpty) {
      logger.info("Bulk updating")
      try {
        val bulkRequest = client.prepareBulk()

        scrollResp.getHits.asScala.foreach { hit =>
          bulkRequest.add(client.prepareIndex(toIndex, hit.getType, hit.getId).setSource(hit.source()))
        }

        val payloadCount = bulkRequest.numberOfActions()

        logger.info(s"Prepared ${payloadCount.toString} updates")

        val result = bulkRequest.execute().actionGet(writeTimeOut)

        if (result.hasFailures) {
          logger.error(s"${result.buildFailureMessage()}")
        } else {
          recordsCopied += payloadCount
          logger.info(s"Copied $payloadCount records ($recordsCopied of $recordsToCopy completed)")
        }

        scrollResp = client.prepareSearchScroll(scrollResp.getScrollId).setScroll(scrollTime).execute().actionGet()

        var errTimes = 0
        while (scrollResp.status().getStatus != 200 && errTimes < 100) {
          logger.warn(s"Scroll read status: ${scrollResp.status().getStatus}. Waiting to retry (${errTimes + 1} of 100)")
          Thread.sleep(2000)
          scrollResp = client.prepareSearchScroll(scrollResp.getScrollId)
            .setScroll(scrollTime).execute().actionGet()
          if (scrollResp.status().getStatus == 200) {
            errTimes = 0
          } else {
            errTimes += 1
          }
        }

        logger.info(s"Next batch request result code: ${scrollResp.status().getStatus}")

        if (errTimes > 0) {
          logger.error("Could not recover from error condition - aborting")
          sys.exit()
        }

      } catch {
        case e: Exception => logger.error(s"${e.getMessage}")
      }
    }

    logger.info("Flushing...")
    Thread.sleep(writeTimeOut)
    try {
      val flusher = new FlushRequest(toIndex)
      client.admin().indices().flush(flusher).actionGet(writeTimeOut.max(5000)) // allow at least five seconds here
    } catch {
      case e: Exception => logger.warn("Flush did not complete or may have timed out - you can do this manually if needed")
    }

    logger.info("Settling...")
    Thread.sleep(writeTimeOut)

    testRecordCount(fromIndex, toIndex)
  }

  def closeConnection() {
    client.close()
  }

  def moveAlias(alias: String, fromIndex: String, toIndex: String): Try[IndicesAliasesResponse] = {
    if (indexExists(fromIndex) && indexExists(toIndex)) {
      Try {
        client.admin.indices.prepareAliases().addAlias(toIndex, alias).removeAlias(fromIndex, alias).execute().actionGet(5000)
      }
    } else {
      Failure(new IllegalArgumentException)
    }
  }

  def testRecordCount(fromIndex: String, toIndex: String) = {
    val from = client.prepareSearch(fromIndex).setQuery(QueryBuilders.matchAllQuery()).execute().actionGet().getHits.totalHits()
    val to = client.prepareSearch(toIndex).setQuery(QueryBuilders.matchAllQuery()).execute().actionGet().getHits.totalHits()

    if (from != to) {
      logger.warn(s"Source / target record counts ($from / $to respectively) don't match - please check")
    }

    from == to
  }

}
