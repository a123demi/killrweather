/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.datastax.killrweather.actor

import java.util.concurrent.TimeoutException

import scala.concurrent.duration._
import akka.actor.SupervisorStrategy._
import akka.actor._
import akka.util.Timeout
import org.apache.spark.util.StatCounter
import org.joda.time.{DateTimeZone, DateTime}
import org.joda.time.format.DateTimeFormat

/** A base actor for weather data computation. */
trait WeatherActor extends Actor with ActorLogging {

  implicit val timeout = Timeout(5.seconds)

  implicit val ctx = context.dispatcher

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case _: ActorInitializationException => Stop
      case _: IllegalArgumentException => Stop
      case _: NullPointerException     => Resume
      case _: IllegalStateException    => Restart
      case _: TimeoutException         => Escalate
      case _: Exception                => Escalate
    }

  def toStatCounter(values: Seq[Double]): StatCounter = StatCounter(values)

  /** Creates a timestamp for the current date time in UTC. */
  def timestamp: DateTime = new DateTime(DateTimeZone.UTC)

  /** Creates a lazy date stream, where elements are only evaluated when they are needed. */
  def streamDays(from: DateTime): Stream[DateTime] = from #:: streamDays(from.plusDays(1))

  def isValid(current: DateTime, start: DateTime): Boolean =
    current.getYear == start.getYear && current.isBeforeNow

  /** Creates timestamp for a given year and day of year. */
  def dayOfYearForYear(doy: Int, year: Int): DateTime =
    timestamp.withYear(year).withDayOfYear(doy)

  /** Creates timestamp for a given year and day of year. */
  def monthOfYearForYear(month: Int, year: Int): DateTime =
    timestamp.withYear(year).withMonthOfYear(month)

  def toDateFormat(dt: DateTime): String = DateTimeFormat.forPattern("EEEE, MMMM dd, yyyy").print(dt)

}

trait DailyWeatherActor extends WeatherActor {

  def year: Int

  def weatherStationActor: ActorRef
}
