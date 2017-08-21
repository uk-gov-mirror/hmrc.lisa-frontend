/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import config.{LisaSessionCache, LisaShortLivedCache}
import models.ApplicationSent
import org.apache.commons.io.FileUtils
import services.{AuthorisationService, NotificationService}
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc._
import play.api.{Configuration, Environment, Logger, Play}

import scala.concurrent.Future

trait ApplicationSubmittedController extends LisaBaseController {

  val notificationService: NotificationService

  def get(): Action[AnyContent] = Action.async { implicit request =>
    authorisedForLisa((_) => {
      sessionCache.fetchAndGetEntry[ApplicationSent](ApplicationSent.cacheKey).map {
        case Some(application) =>
          notificationService.sendMail(application.subscriptionId, application.email)
          Ok(views.html.registration.application_submitted(application.email, application.subscriptionId))

      }
    }, checkEnrolmentState = false)
  }

  def pending(): Action[AnyContent] = Action.async { implicit request =>
    authorisedForLisa((_) => {
      Future.successful(Ok(views.html.registration.application_pending()))
    }, checkEnrolmentState = false)
  }

  def successful(): Action[AnyContent] = Action.async { implicit request =>
    authorisedForLisa((_) => {
      sessionCache.fetchAndGetEntry[String]("lisaManagerReferenceNumber").flatMap {
        case Some(lisaManagerReferenceNumber) =>
          Future.successful(Ok(views.html.registration.application_successful(lisaManagerReferenceNumber)))
      }
    }, checkEnrolmentState = false)
  }

  def rejected(): Action[AnyContent] = Action.async { implicit request =>
    authorisedForLisa((_) => {
      Future.successful(Ok(views.html.registration.application_rejected()))
    }, checkEnrolmentState = false)
  }
}

object ApplicationSubmittedController extends ApplicationSubmittedController {
  override val notificationService = NotificationService
  val config: Configuration = Play.current.configuration
  val env: Environment = Environment(Play.current.path, Play.current.classloader, Play.current.mode)
  override val sessionCache = LisaSessionCache
  override val shortLivedCache = LisaShortLivedCache
  override val authorisationService = AuthorisationService
}