package org.lords.clustering

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD

import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._

object SessionClusterer {

  implicit val formats = DefaultFormats

  def session(jsonObject: JObject): String = (jsonObject \ "session").extract[String]

  def actions(action: String)(jsonObject: JObject): Double = {
    if ((jsonObject \ "actions").extract[List[String]].contains(action)) 1 else 0
  }

  def updatePassword(jsonObject: JObject) = actions("updatePassword")_

  def updatePaymentInfo(jsonObject: JObject) = actions("updatePaymentInfo")_

  def verifiedPassword(jsonObject: JObject) = actions("verifiedPassword")_

  def reviewedQueue(jsonObject: JObject) = actions("reviewedQueue")_

  def kid(jsonObject: JObject): Double = (jsonObject \ "kid").extractOpt[Boolean] match {
    case Some(x) => if (x) 1 else 0
    case None => -1
  }

  /**
    Number of items played
   */
  def numOfItemsPlayed(jsonObject: JObject): Double = (jsonObject \ "played").extract[List[String]].length


  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName("Session Clusterer")
    val sc = new SparkContext(conf)

    val data = sc.textFile("hdfs://127.0.0.1:8020/user/cloudera/clean/")
    data.groupBy { // group by session
      x => """.*"session": ?"([^"]+)".*""".r.findAllIn(x).matchData.map(_.group(1)).toList.head
    } flatMap { // merge
      case (key: String, value: Iterable[String]) =>
        // foldLeft 'acc' must be TraversableOnce, this make me crazy, so I use Option[JValue] instead JValue
        value.map(parse(_)).foldLeft[Option[JValue]](Some(JNothing)) {
          (acc, x) => {
            // kid |= kid
            ((acc.get \ "kid").extractOpt[Boolean], (x \ "kid").extractOpt[Boolean]) match {
              case (Some(a), Some(b)) => Some(acc.get merge x merge parse(s"""{"kid": ${a | b}}"""))
              case _ => Some(acc.get merge x)
            }
          }
        }
    }
  }
}
