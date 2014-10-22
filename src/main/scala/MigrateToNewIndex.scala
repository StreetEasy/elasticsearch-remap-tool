import com.typesafe.scalalogging.slf4j.Logging
import ElasticSearch._
import org.elasticsearch.indices.InvalidAliasNameException
import org.json4s.jackson.JsonMethods._
import org.json4s.JsonAST.JValue
import scala.io.Source._
import scala.util.{Try, Success, Failure}

object Main extends App with Logging {

  try {
    val action = args(0)
    val oldIndex = args(1)
    val newIndex = args(2)

    action.toLowerCase match {
      case "remap" => {
        val mapping = Try(args(3)).toOption
        val batchSize: Int = Try(args(4).toInt.max(10)) getOrElse 500
        val writeTimeOut: Long = Try(args(5).toLong.max(1000)) getOrElse 30000

        Utils.remap(oldIndex, newIndex, mapping, batchSize, writeTimeOut)
      }
      case "update-alias" => {
        val alias = args(3)
        Utils.updateAlias(alias, oldIndex, newIndex)
      }
      case _ => logger.info(s"$action is not a recognised action")
    }
  } catch {
    case e: ArrayIndexOutOfBoundsException => {
      logger.info("An expected argument was missing. See the README for correct usage.")
    }
  }
}

object Utils extends Logging {

  def remap(oldIndex: String,
         newIndex: String,
         mappingsSource: Option[String],
         batchSize: Int,
         writeTimeOut: Long) {

    if (!indexExists(oldIndex)) {
      logger.error("Source index does not exist - migration failed")
    }

    if (!indexExists(newIndex)) {
      logger.info("Loading mappings...")
      val mappings = mappingsSource map (m => loadJsonFromFile(m))

      logger.info(s"Creating index $newIndex...")
      createIndex(newIndex, mappings)

      logger.info(s"Attempting to migrate data from $oldIndex to $newIndex")
      if (migrate(oldIndex, newIndex, batchSize, writeTimeOut)) {
        logger.info("Migration complete! Check the results and update aliases as required")
      } else {
        logger.info("Failed (or nothing to do)")
      }
    } else {
      logger.error("Target index already exists - migration failed")
    }

    closeConnection()
  }

  def updateAlias(alias: String, oldIndex: String, newIndex: String) {
    logger.info(s"Attempting to update alias: $alias from $oldIndex")

    moveAlias(alias, oldIndex, newIndex) match {
      case Success(_) => logger.info(s"Alias $alias has been moved from $oldIndex to $newIndex")
      case Failure(e) => e match {
        case _: IllegalArgumentException => logger.error(s"Index $oldIndex and/or $newIndex does not exist: cannot move the alias $alias")
        case _: InvalidAliasNameException => logger.error(s"Unable to update alias - check that $alias exists on $oldIndex")
        case _ => logger.error("Unable to update alias. Error is: " + e.getMessage)
      }
    }
  }

  def loadJsonFromFile(source: String): JValue = parse(fromFile(source).mkString)
}
