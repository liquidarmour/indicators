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

package uk.gov.hmrc.indicators.service

import java.time.{ZonedDateTime, LocalDate}

import com.github.tomakehurst.wiremock.http.RequestMethod.GET
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}
import play.api.test.FakeApplication
import play.api.test.Helpers._
import uk.gov.hmrc.indicators.{DefaultPatienceConfig, Configs, IndicatorsConfigProvider, WireMockSpec}

class ReleasesClientSpec extends WordSpec with Matchers with WireMockSpec with ScalaFutures with DefaultPatienceConfig{



  val releasesClient = new ReleasesClient(endpointMockUrl)



  "ReleasesClient.getAllReleases" should {
    "get all releases from the releases app and return all releases for the service in production" in {
      running(FakeApplication()) {
        val `release 11.0.0 date` = ZonedDateTime.now().minusDays(5)
        val `release 8.3.0 date` = ZonedDateTime.now().minusMonths(5)

        givenRequestExpects(
          method = GET,
          url = s"$endpointMockUrl/apps",
          willRespondWith = (200,
            Some(
              s"""
              |[
              |    {
              |        "an": "appA",
              |        "env": "prod-something",
              |        "fs": ${`release 11.0.0 date`.toEpochSecond},
              |        "ls": 1450877349,
              |        "ver": "11.0.0"
              |    },
              | {
              |         "an": "appA",
              |         "env": "prod-somethingOther",
              |         "fs": ${`release 11.0.0 date`.toEpochSecond},
              |        "ls": 1450877349,
              |        "ver": "11.0.0"
              |    },
              |    {
              |        "an": "appA",
              |        "env": "qa",
              |        "fs": 1449489675,
              |        "ls": 1450347910,
              |        "ver": "7.3.0"
              |    },
              |    {
              |        "an": "appB",
              |        "env": "qa",
              |        "fs": 1449489675,
              |        "ls": 1450347910,
              |        "ver": "7.3.0"
              |    },
              |    {
              |        "an": "appB",
              |        "env": "prod-something",
              |        "fs": 1449491521,
              |        "ls": 1450879982,
              |        "ver": "5.0.0"
              |    },
              |    {
              |        "an": "appA",
              |        "env": "prod-something",
              |        "fs": ${`release 8.3.0 date`.toEpochSecond},
              |        "ls": 1450347910,
              |        "ver": "8.3.0"
              |    }
              |]
            """.stripMargin
          ))


      )
        releasesClient.getAllReleases.futureValue.size shouldBe 6
        }

    }
  }

}