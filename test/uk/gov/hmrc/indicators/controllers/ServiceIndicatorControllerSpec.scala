/*
 * Copyright 2016 HM Revenue & Customs
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

package uk.gov.hmrc.indicators.controllers

import java.time.{LocalDate, YearMonth}

import akka.util.Timeout
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.iteratee.Iteratee
import play.api.mvc.{Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.indicators.TestImplicits._
import uk.gov.hmrc.indicators.service.{IndicatorsService, MeasureResult, ReleaseStabilityMetricResult, ReleaseThroughputMetricResult}

import scala.concurrent.{Await, Future}

class ServiceIndicatorControllerSpec extends PlaySpec with MockitoSugar {

  private val mockIndicatorsService = mock[IndicatorsService]

  val controller = new ServiceIndicatorController {
    override val indicatorsService: IndicatorsService = mockIndicatorsService
  }


  "ServiceIndicatorController.releaseStability" should {

    "return release stability metrics when there is no data" in {

      val date = LocalDate.of(2016, 9, 13)

      when(mockIndicatorsService.getReleaseStabilityMetrics("serviceName")).thenReturn(Future.successful(Some(List())))

      val result = controller.releaseStability("serviceName")(FakeRequest())

      contentAsJson(result) mustBe
        """[]""".stripMargin.toJson

      header("content-type", result).get mustBe "application/json"
    }


    "return release stability metrics when there is data" in {

      val date = LocalDate.of(2016, 9, 13)

      when(mockIndicatorsService.getReleaseStabilityMetrics("serviceName")).thenReturn(
        Future.successful(
          Some(
            List(ReleaseStabilityMetricResult(YearMonth.of(2016,9), from = LocalDate.of(2016,7,1), to = LocalDate.of(2016,9,30), None, None)))
        )
      )

      val result = controller.releaseStability("serviceName")(FakeRequest())

      contentAsJson(result) mustBe
        """[{"period":"2016-09","from":"2016-07-01","to":"2016-09-30"}]""".stripMargin.toJson

      header("content-type", result).get mustBe "application/json"
    }


  }

  "ServiceIndicatorController.releaseThroughput" should {


    "return Frequent release metric for a given service in json format" in {

      val date = LocalDate.of(2016, 9, 13)

      when(mockIndicatorsService.getReleaseThroughputMetrics("serviceName")).thenReturn(Future.successful(
        Some(List(
          ReleaseThroughputMetricResult(YearMonth.of(2016, 4), from = date, to = date, Some(MeasureResult(5)), Some(MeasureResult(4))),
          ReleaseThroughputMetricResult(YearMonth.of(2016, 5), from = date, to = date, Some(MeasureResult(6)), None)
        )))
      )

      val result = controller.releaseThroughput("serviceName")(FakeRequest())

      contentAsJson(result) mustBe
        """[
          |{"period" : "2016-04", "from" : "2016-09-13", "to" : "2016-09-13", "leadTime" : {"median" : 5}, "interval" : {"median" : 4}},
          |{"period" : "2016-05", "from" : "2016-09-13", "to" : "2016-09-13", "leadTime" : {"median" : 6}}
          |]""".stripMargin.toJson

      header("content-type", result).get mustBe "application/json"
    }


    "return NotFound if None lead times returned" in {
      when(mockIndicatorsService.getReleaseThroughputMetrics("serviceName")).thenReturn(Future.successful(None))

      val result = controller.releaseThroughput("serviceName")(FakeRequest())

      status(result) mustBe NOT_FOUND


    }


  }

  /**
    * http://stackoverflow.com/questions/28461877/is-there-a-bug-in-play2-testing-with-fakerequests-and-chunked-responses-enumera
    */
  def contentAsBytes(of: Future[Result])(implicit timeout: Timeout): Array[Byte] = {
    val r = Await.result(of, timeout.duration)
    val e = r.header.headers.get(TRANSFER_ENCODING) match {
      case Some("chunked") => {
        r.body &> Results.dechunk
      }
      case _ => r.body
    }
    Await.result(e |>>> Iteratee.consume[Array[Byte]](), timeout.duration)
  }


}
