package services

import org.scanamo.{Scanamo, Table}
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import org.scanamo._
import org.scanamo.syntax._
import org.scanamo.generic.auto._
import scala.concurrent.ExecutionContext
import scala.util.Either


case class NewCookieIssue(id: String, email: String, requested: Long, used: Boolean)

/**
  * Service to manage persistence of user cookies.
  */
class TokenDBService(
  client: DynamoDbClient,
  tableName: String
)(implicit executionContext: ExecutionContext) {

  private val table = Table[NewCookieIssue](tableName)
  private def run[T] = Scanamo(client).exec[T] _

  def getCookieIssueForUserToken(userToken: String): Option[Either[DynamoReadError, NewCookieIssue]] =
    run(table.get("id" === userToken))

  def createCookieIssue(cookieIssue: NewCookieIssue): Unit = {
    run(table.put(cookieIssue))
  }

  def expireCookieIssue(cookieIssue: NewCookieIssue): Unit = {
    run(table.put(cookieIssue.copy(used = true)))
  }
}
