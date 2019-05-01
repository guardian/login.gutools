package services.switches

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.github.t3hnar.bcrypt._
import com.gu.scanamo._
import com.gu.scanamo.syntax._
import config.LoginConfig
import model.SwitchError

case class EmergencyUser(userId: String, passwordHash: String)

class SwitchAccess(config: LoginConfig, dynamoDbClient: AmazonDynamoDB) {
  def checkAccess(username: String, password: String): Either[SwitchError, Unit] = {
    val tableName = config.emergencyAccessTableName

    Scanamo.get[EmergencyUser](dynamoDbClient)(tableName)('userId -> username).map {
      case Right(dbUser: EmergencyUser) =>
        // Check the provided password against the DBs hashed version
        if (password.isBcrypted(dbUser.passwordHash)) {
          Right(())
        } else {
          Left(SwitchError.AccessBadPasswordError)
        }
      case Left(_) => Left(SwitchError.UsersTableError)
    }.getOrElse(Left(SwitchError.UserNotFoundError))
  }
}
