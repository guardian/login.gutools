package utils

import java.net.URLEncoder

object DesktopTokenUtils {

  def desktopRedirectUrl(token: String, stageName: String): String =
    s"gu-panda://desktop?token=${URLEncoder.encode(token, "UTF-8")}&stage=${stageName.toLowerCase}"
}
