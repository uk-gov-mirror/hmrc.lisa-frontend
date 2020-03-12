/*
 * Copyright 2020 HM Revenue & Customs
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

package connectors

import config.AppConfig
import metrics.EmailMetrics
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.JsValue
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.Future

class EmailConnectorSpec extends PlaySpec with MockitoSugar with BeforeAndAfterEach {

  val mockHttpClient: HttpClient = mock[HttpClient]
  val mockAppConfig: AppConfig = mock[AppConfig]
  val mockMetrics: EmailMetrics = mock[EmailMetrics]

  val testEmailConnector = new EmailConnector(mockHttpClient, mockAppConfig, mockMetrics)

  override def beforeEach() {
    reset(mockHttpClient)
  }

  "EmailConnector" must {

    "return a 202 accepted" when {

      "correct emailId Id is passed" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val emailString = "test@mail.com"
        val templateId = "lisa_application_submit"
        val params = Map("testParam" -> "testParam")

        when(mockHttpClient.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(),
          ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse(202, responseJson = None)))

        val response = testEmailConnector.sendTemplatedEmail(emailString, templateId, params)
        await(response) must be(EmailSent)

      }

    }

    "return other status" when {

      "incorrect email Id are passed" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val invalidEmailString = "test@test1.com"
        val templateId = "lisa_application_submit"
        val params = Map("testParam" -> "testParam")

        when(mockHttpClient.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(),
          ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse(404, responseJson = None)))

        val response = testEmailConnector.sendTemplatedEmail(invalidEmailString, templateId, params)
        await(response) must be(EmailNotSent)

      }

    }

  }
}