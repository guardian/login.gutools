package config.aws

import config.LoginConfig
import org.scalatest.{FreeSpec, Matchers}


class LoginConfigTest extends FreeSpec with Matchers {
  import LoginConfig._

  "createLoginConfig" - {
    "uses DEV if no stage is present" in {
      LoginConfig.forStage(None).stage shouldEqual "DEV"
    }

    "uses lower-case custom stage for domain, where provided" in {
      LoginConfig.forStage(Some("CODE")).domain should startWith("code.")
    }
  }

  "isValidUrl" - {
    "if a domain is configured" - {
      val domain = "example.com"

      "returns false if the return URL's host does not match the configured domain" in {
        isValidUrl(domain, "https://different-domain.com/returnUrl") shouldEqual false
      }

      "returns true if the return URL matches the domain" in {
        isValidUrl(domain, "https://test.example.com/returnUrl") shouldEqual true
      }

      "returns false if the return URL's is not secure" in {
        isValidUrl(domain, "http://test.example.com/returnUrl") shouldEqual false
      }

      "returns false if the URL is not valid" in {
        isValidUrl(domain, "Not a URL at all!") shouldEqual false
      }
    }
  }
}
