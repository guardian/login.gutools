package mailer

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService
import com.amazonaws.services.simpleemail.model._
import config.LoginConfig

class SES(sesClient: AmazonSimpleEmailService, loginConfig: LoginConfig) {
  def sendCookieEmail(token: String, sendTo: String): Unit = {

    val uri = loginConfig.tokenReissueUri

    val emailBody = s"<div>Click <a href=$uri$token>here</a> here to obtain a new cookie</div>"

    sesClient.sendEmail(new SendEmailRequest()
      .withDestination(new Destination().withToAddresses(sendTo))
      .withMessage(new Message()
        .withSubject(new Content("Gutools new cookie link"))
        .withBody(new Body().withHtml(new Content(emailBody)))
      )
      .withSource(loginConfig.emailSettings.get("from").get)
      .withReplyToAddresses(loginConfig.emailSettings.get("replyTo").get)
    )

  }
}