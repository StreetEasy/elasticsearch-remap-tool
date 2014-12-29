import scala.util.{Try, Success, Failure}
import scala.io.Source
import com.typesafe.scalalogging.slf4j.Logging
import org.elasticsearch.indices.InvalidAliasNameException
import org.json4s.jackson.JsonMethods
import org.json4s.JsonAST.JValue

object Start extends App with Logging {

  try {
    val action = args(0)
    val oldIndex = args(1)
    val newIndex = args(2)

    action.toLowerCase match {
      case "remap" => {
        val batchSize: Int = Try(args(3).toInt.max(10)) getOrElse 500
        val writeTimeOut: Long = Try(args(4).toLong.max(1000)) getOrElse 30000
        remap(oldIndex, newIndex, batchSize, writeTimeOut)
      }
      case "update-alias" => {
        val alias = args(3)
        updateAlias(alias, oldIndex, newIndex)
      }
      case _ => logger.info(s"$action is not a recognised action")
    }
  }
  catch {
    case e: ArrayIndexOutOfBoundsException => logger.info("An expected argument was missing. See the README for correct usage.")
  }

  def remap(oldIndex: String,
         newIndex: String,
         batchSize: Int,
         writeTimeOut: Long) {

    if (!Elasticsearch.indexExists(oldIndex)) {
      logger.error("Source index does not exist - migration failed")
    }

    if (!Elasticsearch.indexExists(newIndex)) {
      logger.info(s"Creating index $newIndex...")
      Elasticsearch.createIndex(newIndex)

      logger.info(s"Attempting to migrate data from $oldIndex to $newIndex")
      if (Elasticsearch.migrate(oldIndex, newIndex, batchSize, writeTimeOut)) {
        logger.info("Migration complete! Check the results and update aliases as required")
      } else {
        logger.info("Failed (or nothing to do)")
      }
    } else {
      logger.error("Target index already exists - migration failed")
    }

    Elasticsearch.closeConnection()
  }

  def updateAlias(alias: String, oldIndex: String, newIndex: String) {
    logger.info(s"Attempting to update alias: $alias from $oldIndex")

    Elasticsearch.moveAlias(alias, oldIndex, newIndex) match {
      case Success(_) => logger.info(s"Alias $alias has been moved from $oldIndex to $newIndex")
      case Failure(e) => e match {
        case _: IllegalArgumentException => logger.error(s"Index $oldIndex and/or $newIndex does not exist: cannot move the alias $alias")
        case _: InvalidAliasNameException => logger.error(s"Unable to update alias - check that $alias exists on $oldIndex")
        case _ => logger.error("Unable to update alias. Error is: " + e.getMessage)
      }
    }
  }

  def loadJsonFromFile(source: String): JValue = JsonMethods.parse(Source.fromFile(source).mkString)

}
