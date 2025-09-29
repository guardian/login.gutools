package utils

import software.amazon.awssdk.services.ses.SesClient
import software.amazon.awssdk.services.ses.model._

import config.LoginConfig

class SES(sesClient: SesClient, loginConfig: LoginConfig) {
  def buildContent(data: String) = Content.builder().charset("UTF-8").data(data).build()

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


    sesClient.sendEmail( SendEmailRequest.builder()
      .destination(Destination.builder().toAddresses(sendTo).build())
      .message(Message.builder()
        .subject(buildContent("[emergency login] Guardian Editorial Tools - new cookie link"))
        .body(Body.builder().html(buildContent(emailBody)).build()).build()
      )
      .source(loginConfig.emailSettings("from"))
      .replyToAddresses(loginConfig.emailSettings("replyTo")).build()
    )
  }
}