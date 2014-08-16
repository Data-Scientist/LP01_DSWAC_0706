package org.lords.classification

import java.io.PrintWriter

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD

import scala.util.parsing.json._
import scala.util.Random


object UserClassifier {
  /**
    Extract items from json string, which 'played'/'reviewed'/'rated' by user

    @param line, json string
    */
  def extractItems(line: String): Option[Tuple2[String, (Long, Long, Char, Iterable[String])]] = {
    def keys = (x: Any) => x.asInstanceOf[Map[String, List[String]]].keys

    // if scala version is smaller than 2.11, need this
    type StringMap = Map[String, Any]

    JSON.parseFull(line) match {
      case Some(m: StringMap) => 
        val user = m("user").asInstanceOf[String]
        val start = m("start").asInstanceOf[String].toLong
        val end = m("end").asInstanceOf[String].toLong
        val items = keys(m("played")) ++ keys(m("rated")) ++ keys(m("reviewed"))
        // n -> null, t -> true, f -> false
        val kid = if(m("kid") == null) 'n' else m("kid").toString.head

        Some((user, (start, end, kid, items)))

      case _ => None
    }
  }

  /**
    Merge all items in this group. If already known the user is 'kid', add suffix 'k',
    'adult' user add suffix 'a'. If seen a conflicting label, treat this user as two 
    different user.

    @param group, a group of sessions
   */
  def combine(group: Tuple2[String, Iterable[(Long, Long, Char, Iterable[String])]]) = {
    val user = group._1
    // sort sessions by time, x._1 is start time, x._2 is end time
    val sortedValue = group._2.toSeq.sortBy(x => (x._1, x._2))

    def emptyIter = Iterable.empty[String]

    // all data are belong to a same user, but this user can treat as two different user
    // items._1 current user type ('a' or 'k')
    // items._2 items belong to unlabeled user
    // items._3 items belong to kid user
    // items._4 items belong to adult user
    val items = sortedValue.foldLeft(('n', emptyIter, emptyIter, emptyIter)) { (acc, x) =>
      val unknown = acc._2
      val kid = acc._3
      val adult = acc._4

      val pre = acc._1
      val cur = x._3
      val newItems = x._4

      (pre, cur) match {
        case ('n', 'n') => ('n', newItems ++ unknown, kid, adult)
        case ('n', 't') | ('t', 'n') | ('t', 't') => ('t', emptyIter, newItems ++ kid ++ unknown, adult)
        case ('n', 'f') | ('f', 'n') | ('f', 'f') => ('f', emptyIter, kid, newItems ++ adult ++ unknown)
        case ('t', 'f') => ('f', emptyIter, kid, newItems ++ adult ++ unknown)
        case ('f', 't') => ('t', emptyIter, newItems ++ kid ++ unknown, adult)
      }
    }

    // merge all items, add 'a' or 'k' suffix to user name if need
    val data = (if(items._2.isEmpty) None else Some((user, items._2))) ::
      (if(items._3.isEmpty) None else Some((s"${user}k", items._3))) ::
      (if(items._4.isEmpty) None else Some((s"${user}a", items._4))) :: Nil

    // user -> list of items
    data.flatMap(x => x)
  }


  /**
    simrank implements
   */
  def simrank(matrix: RDD[(String, Iterable[String])], source: Array[String],
    beta: Double = 0.8, threshold: Double = 0.01): Map[String, Double] = {
    // initial probability which assignment to each source
    val N = source.size
    val prob = 1.0 / N
    val telport = (1.0 - beta) / N
    // v_0
    val v0 = source.foldLeft(Map.empty[String, Double])((acc, x) => acc.updated(x, prob))

    // recursively compute v_{k+1}
    def _simrank(v: Map[String,Double]): Map[String, Double] = {
      // compute the major distribution vector of next state
      val majorVn = matrix.flatMap { case (col, rows) =>
        rows.flatMap { row => if(v.contains(col)) Option((row, v(col) / rows.size)) else None }
      } .groupByKey().map(
        x => (x._1, if(source.contains(x._1)) x._2.sum * beta + telport else x._2.sum * beta)
      ) .collect

      // find all 'user' which appear in source, but not appear in 'majorVn'
      val noAppearSource = source.filter(x => !majorVn.contains(x))
      val onlyTelportV = noAppearSource.foldLeft(Map.empty[String, Double])((acc, x) => acc.updated(x, telport))
      // merge 'majorVn' and 'onlyTelportV', we got the final distribution vector of next state
      val nv = majorVn.foldLeft(onlyTelportV)((acc, x) => acc.updated(x._1, x._2))

      val diff = nv.foldLeft(0.0)((acc, x) => if(v.contains(x._1)) acc + Math.abs(v(x._1) - x._2) else acc + x._2)

      // if 'diff' bigger than threshold loop again, else return distribution vector
      if(diff > threshold) { _simrank(nv) } else { nv }
    }

    _simrank(v0)
  }
  
  def beta = 0.8
  def threshold = 1E-6

  /**
    classifition

    @param data, data
    @param adult, labeled adult users
    @param kid, labeled kid users
   */
  def solve(data: RDD[(String, Iterable[String])], adult: Array[String], kid: Array[String]) = {
    // Step 3. Build an adjacency matrix
    val matrix = data.union(data.flatMap(x => x._2.map(item => (item, x._1))).groupByKey())

    // a regex pattern which match user name
    val pattern = """\d{7,8}[ak]?""".r.pattern

    val adultV = simrank(matrix, adult, beta, threshold).filter(x => pattern.matcher(x._1).matches)
    val kidV = simrank(matrix, kid, beta, threshold).filter(x => pattern.matcher(x._1).matches)

    // normalize
    val adultNum = adult.size
    val kidNum = kid.size
    val normalAdultV = adultV.map(x => (x._1, -1.0 * adultNum / kidNum))

    // merge
    val v = normalAdultV.foldLeft(kidV) { case (m, (key, value)) => m + (key -> (value + m.getOrElse(key, 0.0) + 1.0))}
    // drop duplicate user, remove 'a' or 'k' suffix
    val solution = v.keySet.toList.sorted.foldLeft(Map.empty[String, Int]) {(acc, k) =>
      val user = if(k.endsWith("k") || k.endsWith("a")) k.init else k
      acc.updated(user, v(k).toInt)
    }

    solution
  }

  def classify(data: RDD[(String, Iterable[String])], solutionFileName: String) = {
    val user = data.map(_._1)
    val adult = user.filter(_.endsWith("a")).collect
    val kid = user.filter(_.endsWith("k")).collect

    val solution = solve(data, adult, kid)

    val out = new PrintWriter(solutionFileName)
    solution.foreach(x => out.println(f"${x._1}\t${x._2}"))
    out.close()
  }

  def test(data: RDD[(String, Iterable[String])]) = {
    val user = data.map(_._1)

    // split adult users as training set and test set 
    val adult = user.filter(_.endsWith("a")).collect
    val (adultTrain, adultTest) = Random.shuffle(adult.toSeq).splitAt((adult.size / 5.0 * 4).toInt)

    // split kid users as training set and test set
    val kid = user.filter(_.endsWith("k")).collect
    val (kidTrain, kidTest) = Random.shuffle(kid.toSeq).splitAt((kid.size / 5.0 * 4).toInt)

    // convert test set to Map format, eg. 1234567 -> 0 or 7654321 -> 1
    val testSet = kidTest.map(_.init).foldLeft(adultTest.map(_.init -> 0).toMap) {
      (acc, x) => acc.updated(x, 1)
    }

    val solution = solve(data, adultTrain.toArray, kidTrain.toArray)

    val accuracy = testSet.filter{
      case(user, label) => if (solution.contains(user)) solution(user) == label else true
    }.size / testSet.size.toDouble * 100

    println("="*80)
    println(f"Accuracy: ${accuracy}%.2f %%")
    println("="*80)
  }

  def usage = {
    println("="*80)
    println("usage.")
    println("  UserClassifier classify data solution")
    println("  UserClassifier test data")
    println("="*80)
  }

  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName("User Classifier")
    val sc = new SparkContext(conf)

    args.toList match {
      case "classify" :: tail if tail.size == 2 => {
        val dataFileName = args(1)
        val solutionFileName = args(2)
        val data = sc.textFile(dataFileName).flatMap(extractItems).groupByKey().flatMap(combine)
        classify(data, solutionFileName)
      }
      case "test" :: tail if tail.size == 1 => {
        val dataFileName = args(1)
        val data = sc.textFile(dataFileName).flatMap(extractItems).groupByKey().flatMap(combine)
        test(data)
      }
      case _ => usage
    }

    // "hdfs://127.0.0.1:8020/user/cloudera/clean/"
  }
}
