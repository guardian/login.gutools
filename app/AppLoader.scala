import play.api.{Application, ApplicationLoader, LoggerConfigurator}

class AppLoader extends ApplicationLoader {
  override def load(context: ApplicationLoader.Context): Application = {
    // TODO MRB: kinesis logging?
    LoggerConfigurator(context.environment.classLoader)
      .foreach(_.configure(context.environment))

    new AppComponents(context).application
  }
}
