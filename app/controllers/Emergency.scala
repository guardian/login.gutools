package controllers

import play.api.mvc.Controller

object Emergency extends Controller with PanDomainAuthActions {

  def reissue = {
    //TODO:
    /**
      * Only allow this process to happen if a config switch is on
      *
      * Reads cookies from request
      * Verifies the cookie
      * Reads the data out of the cookie
      * Creates a new cookie with same data
      * Writes cookie to user's browser
      *
      *
      */

  }

}
