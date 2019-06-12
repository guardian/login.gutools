package utils

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
         |    The Guardian's Editorial Tools are experiencing login issues. This is likely due to a Google issue (please refer to emails from Central Production). You must perform an additional step to login.
         |  </p>
         |  <p>
         |    Click <a href=$uri$token>here</a> to login and continue using the Tools.
         |  </p>
         |  <p>
         |    <strong>Do NOT share this email with others. The link is private and for your use only.</strong>
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