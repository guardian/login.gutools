package mailer

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService
import com.amazonaws.services.simpleemail.model._
import config.LoginConfig

class SES(sesClient: AmazonSimpleEmailService, loginConfig: LoginConfig) {
  def sendCookieEmail(token: String, sendTo: String): Unit = {

    val uri = loginConfig.tokenReissueUri

    val emailBody =
      s"""
         |<div>
         |  Hi,
         |  <p>
         |    The Guardian's Editorial Tools are currently experiencing issues with Google Authentication and so we cannot authenticate your access to the Tools.
         |  </p>
         |  <p>
         |    Click <a href=$uri$token>here</a> to obtain a new cookie to allow you to use them.
         |  </p>
         |  <p>
         |    <strong>Do NOT share this email.</strong>
         |  </p>
         |</div>
       """.stripMargin

    sesClient.sendEmail(new SendEmailRequest()
      .withDestination(new Destination().withToAddresses(sendTo))
      .withMessage(new Message()
        .withSubject(new Content("[emergency login] Guardian Editorial Tools - new cookie link"))
        .withBody(new Body().withHtml(new Content(emailBody)))
      )
      .withSource(loginConfig.emailSettings("from"))
      .withReplyToAddresses(loginConfig.emailSettings("replyTo"))
    )

  }
}