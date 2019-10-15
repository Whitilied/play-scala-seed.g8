package controllers

import buildinfo.BuildInfo
import javax.inject._
import kamon.Kamon
import play.api.mvc._
import play.api.libs.json.Json


@Singleton
class InternalController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  private val prometheusReport = kamon.prometheus.PrometheusReporter.create()
  Kamon.registerModule("Prometheus Reporter", prometheusReport)

  def health = Action { implicit request =>
    Ok(Json.obj("status" -> "ok"))
  }

  def version() = Action{ implicit request =>
     Ok(BuildInfo.toJson).as(JSON)
  }

  def metrics = Action { implicit request =>
    Ok(prometheusReport.scrapeData()).as("text/plain; version=0.0.4; charset=utf-8")
  }
}
