/*
 * Copyright 2019 HM Revenue & Customs
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

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.test.FakeRequest
import play.api.test.Helpers._

class ErrorControllerSpec extends PlaySpec with GuiceOneAppPerSuite {

  val fakeRequest = FakeRequest("GET", "/")

  "GET /access-denied" should {
    "return 403" in {
      val result = ErrorController.accessDenied(fakeRequest)
      status(result) mustBe Status.FORBIDDEN
      val content = contentAsString(result)

      content must include("There is a problem</h1>")
      content must include("You signed in as an individual or agent.")
    }
  }

}