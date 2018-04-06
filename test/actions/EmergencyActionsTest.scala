package actions

import controllers.{EmergencyActionsException, EmergencySwitchChangeAccess}
import org.scalatest.{FreeSpec, Matchers}
import play.api.test.FakeHeaders

class EmergencyActionsTest extends FreeSpec with Matchers {
  "EmergencySwitchChangeAccess" - {
    "Should throw an exception" - {
      "if Authorization header is missing" in {
        val headers = Seq(("some-header", "some-header-value"))
        val thrownException = the[EmergencyActionsException] thrownBy EmergencySwitchChangeAccess.getBasicAuthDetails(FakeHeaders(headers))
        thrownException.getMessage should equal("Basic authorization header is missing")
      }
      "if Basic is missing in the Authorization header" in {
        val headers = Seq(("Authorization", "joe.bloggs@guardian.co.uk:some-password"))
        val thrownException = the[EmergencyActionsException] thrownBy EmergencySwitchChangeAccess.getBasicAuthDetails(FakeHeaders(headers))
        thrownException.getMessage should equal("Basic authorization header is missing")
      }
      "if email and password are not separate by a colon in the Authorization header" in {
        val headers = Seq(("Authorization", "Basic joe.bloggs@guardian.co.uk some-password"))
        val thrownException = the[EmergencyActionsException] thrownBy EmergencySwitchChangeAccess.getBasicAuthDetails(FakeHeaders(headers))
        thrownException.getMessage should equal("Authorization header value is not the correct format.")
      }
      "if authorization header is incorrectly formatted" in {
        val headers = Seq(("Authorization", "Basic joe.bloggs@guardian.co.uk:some:password"))
        val thrownException = the[EmergencyActionsException] thrownBy EmergencySwitchChangeAccess.getBasicAuthDetails(FakeHeaders(headers))
        thrownException.getMessage should equal("Authorization header value is not the correct format.")
      }
      "if email is missing @guardian.co.uk in the Authorization header" in {
        val headers = Seq(("Authorization", "Basic joe.bloggs:some-password"))
        val thrownException = the[EmergencyActionsException] thrownBy EmergencySwitchChangeAccess.getBasicAuthDetails(FakeHeaders(headers))
        thrownException.getMessage should equal("Authorization header value is not the correct format.")
      }
    }
  }
}