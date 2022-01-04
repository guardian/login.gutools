package services

import org.scanamo.{Scanamo, Table}
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import org.scanamo._
import org.scanamo.syntax._
import org.scanamo.generic.auto._
import scala.concurrent.ExecutionContext
import scala.util.Either


case class EmergencyUser(userId: String, passwordHash: String)

/**
  * Service to manage persistence of emergency user records.
  */
class EmergencyUserDBService(
  client: DynamoDbClient,
  tableName: String
)(implicit executionContext: ExecutionContext) {

  private val table = Table[EmergencyUser](tableName)
  private def run[T] = Scanamo(client).exec[T] _

  def getUser(userId: String): Option[Either[DynamoReadError, EmergencyUser]] = run(table.get("userId" === userId))
}
