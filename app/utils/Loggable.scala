package utils

import org.slf4j.{Logger, LoggerFactory}

trait Loggable {
  protected lazy val log: Logger = LoggerFactory.getLogger(getClass)
}
