package config.aws

import config.LoginConfig
import org.scalatest.{FreeSpec, Matchers}


class LoginConfigTest extends FreeSpec with Matchers {
  import LoginConfig._

  "createLoginConfig" - {
    "uses DEV if no stage is present" in {
      createLoginConfig(None).stage shouldEqual "DEV"
    }

    "uses lower-case custom stage for domain, where provided" in {
      createLoginConfig(Some("CODE")).domain should startWith("code.")
    }
  }

  "isValidUrl" - {
    "returns false if no domain is configured" in {
      isValidUrl(None, "https://example.com/returnUrl") shouldEqual false
    }

    "if a domain is configured" - {
      val domain = Some("example.com")

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
