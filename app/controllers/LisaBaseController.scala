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

import config.FrontendAppConfig
import connectors.UserDetailsConnector
import models._
import play.api.mvc.{AnyContent, Request, Result}
import services.TaxEnrolmentService
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.Retrievals._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.frontend.Redirects
import uk.gov.hmrc.http.cache.client.ShortLivedCache
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

trait LisaBaseController extends FrontendController
  with AuthorisedFunctions
  with Redirects {

  val cache: ShortLivedCache
  val userDetailsConnector: UserDetailsConnector
  val taxEnrolmentService: TaxEnrolmentService

  def authorisedForLisa(callback: (String) => Future[Result])(implicit request: Request[AnyContent]): Future[Result] = {
    authorised(
      AffinityGroup.Organisation and AuthProviders(GovernmentGateway)
    ).retrieve(internalId and userDetailsUri) { case (id ~ userUri) =>
      val userId = id.getOrElse(throw new RuntimeException("No internalId for logged in user"))

      userUri match {
        case Some(url) => {
          userDetailsConnector.getUserDetails(url) flatMap { user =>
            user.groupIdentifier match {
              case Some(groupId) => {
                taxEnrolmentService.getLisaSubscriptionState(groupId) flatMap {
                  case TaxEnrolmentPending => Future.successful(Redirect(routes.ApplicationSubmittedController.pending()))
                  case TaxEnrolmentError => Future.successful(Redirect(routes.ApplicationSubmittedController.rejected()))
                  case TaxEnrolmentSuccess => Future.successful(Redirect(routes.ApplicationSubmittedController.successful()))
                  case _ => callback(s"$userId-lisa-registration")
                }
              }
              case None => {
                Future.successful(Redirect(routes.ErrorController.error()))
              }
            }
          }
        }
        case None => {
          Future.successful(Redirect(routes.ErrorController.error()))
        }
      }
    } recoverWith {
      handleFailure
    }
  }

  def handleFailure(implicit request: Request[_]): PartialFunction[Throwable, Future[Result]] = PartialFunction[Throwable, Future[Result]] {
    case _: NoActiveSession => Future.successful(toGGLogin(FrontendAppConfig.loginCallback))
    case _: AuthorisationException => Future.successful(Redirect(routes.ErrorController.accessDenied()))
    case _ => Future.successful(Redirect(routes.ErrorController.error()))
  }

  def hasAllSubmissionData(cacheId: String)(callback: (LisaRegistration) => Future[Result])(implicit request: Request[AnyContent]): Future[Result] = {
    // get organisation details
    cache.fetchAndGetEntry[BusinessStructure](cacheId, BusinessStructure.cacheKey).flatMap {
      case None => Future.successful(Redirect(routes.BusinessStructureController.get()))
      case Some(busData) => {

        cache.fetchAndGetEntry[OrganisationDetails](cacheId, OrganisationDetails.cacheKey).flatMap {
          case None => Future.successful(Redirect(routes.OrganisationDetailsController.get()))
          case Some(orgData) => {

            // get trading details
            cache.fetchAndGetEntry[TradingDetails](cacheId, TradingDetails.cacheKey).flatMap {
              case None => Future.successful(Redirect(routes.TradingDetailsController.get()))
              case Some(tradData) => {

                // get business structure

                // get user details
                cache.fetchAndGetEntry[YourDetails](cacheId, YourDetails.cacheKey).flatMap {
                  case None => Future.successful(Redirect(routes.YourDetailsController.get()))
                  case Some(yourData) => {
                    val data = new LisaRegistration(orgData, tradData, busData, yourData)
                    callback(data)
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  def handleRedirect(redirectUrl: String)(implicit request: Request[AnyContent]): Future[Result] = {
    val returnUrl: Option[String] = request.getQueryString("returnUrl")

    returnUrl match {
      case Some(url) if url.matches("^\\/lifetime\\-isa\\/.*$") => Future.successful(Redirect(url))
      case _ => Future.successful(Redirect(redirectUrl))
    }
  }

}
