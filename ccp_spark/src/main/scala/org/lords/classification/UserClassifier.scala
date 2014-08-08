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
    归并用户相关的items，给已标出是'kid'或'adult'的用户添加'k'或'a'后缀，
    如果'kid'/'adult'标记前后矛盾，拆分用户
   */
  def combine(group: Tuple2[String, Iterable[(Long, Long, Char, Iterable[String])]]) = {
    val user = group._1
    // 将session对应的值按时间排序
    val sortedValue = group._2.toSeq.sortBy(x => (x._1, x._2))

    def emptyIter = Iterable.empty[String]

    // 这里的数据都属于同一用户（可能同时被两个人使用的同一账户），但要根据行为进行拆解用户
    // items._1 标记当前处理的用户类型
    // items._2 未知类型的用户对应的item
    // items._3 kid用户对应的item
    // items._4 adult用户对应的item
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

    // 合并所有的items，并根据是否为kid，给user加'a'/'k'后缀
    val data = (if(items._2.isEmpty) None else Some((user, items._2))) ::
      (if(items._3.isEmpty) None else Some((s"${user}k", items._3))) ::
      (if(items._4.isEmpty) None else Some((s"${user}a", items._4))) :: Nil

    data.flatMap(x => x)
  }


  /**
    simrank算法
   */
  def simrank(matrix: RDD[(String, Iterable[String])], source: Array[String],
    beta: Double = 0.8, threshold: Double = 0.01): Map[String, Double] = {
    // 初始分配给每个source的概率
    val prob = 1.0 / source.size
    val telport = (1.0 - beta) / source.size
    // v_0
    val v0 = source.foldLeft(Map.empty[String, Double])((acc, x) => acc.updated(x, prob))

    // recursively compute v_{k+1}
    def _simrank(v: Map[String,Double]): Map[String, Double] = {
      // 计算出下一步distribution vector的部分值
      val partialVn = matrix.flatMap { case (col, rows) =>
        rows.flatMap { row => if(v.contains(col)) Option((row, v(col) / rows.size)) else None }
      } .groupByKey().map(
        x => (x._1, if(source.contains(x._1)) x._2.sum * beta + telport else x._2.sum * beta)
      ) .collect

      // 找出在partialVn中没有出现，但在source中存在的结点
      val noAppearSource = source.filter(x => !partialVn.contains(x))
      val onlyTelportV = noAppearSource.foldLeft(Map.empty[String, Double])((acc, x) => acc.updated(x, telport))
      // 合并partialVn和onlyTelportV，得到最终的vn
      val nv = partialVn.foldLeft(onlyTelportV)((acc, x) => acc.updated(x._1, x._2))

      // 计算差值
      val diff = nv.foldLeft(0.0)((acc, x) => if(v.contains(x._1)) acc + Math.abs(v(x._1) - x._2) else acc + x._2)
      if(diff > threshold) { _simrank(nv) } else { nv }
    }

    _simrank(v0)
  }
  
  def beta = 0.8
  def threshold = 1E-6

  /**
    根据给出的数据，求解分类
   */
  def solve(data: RDD[(String, Iterable[String])], adult: Array[String], kid: Array[String]) = {
    // Step 3. Build an adjacency matrix
    val matrix = data.union(data.flatMap(x => x._2.map(item => (item, x._1))).groupByKey())

    val pattern = """\d{7,8}[ak]?""".r.pattern

    val adultV = simrank(matrix, adult, beta, threshold).filter(x => pattern.matcher(x._1).matches)
    val kidV = simrank(matrix, kid, beta, threshold).filter(x => pattern.matcher(x._1).matches)

    // normalize
    val adultNum = adult.size
    val kidNum = kid.size
    val normalAdultV = adultV.map(x => (x._1, -1.0 * adultNum / kidNum))

    // merge, /: like is alternate syntax of foldLeft
    val v = (normalAdultV /: kidV) { case (m, (key, value)) => m + (key -> (value + m.getOrElse(key, 0.0) + 1.0))}
    // 丢弃重复的user id
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

    // 将已知类型的用户按4:1的比例分成training set和test set
    val adult = user.filter(_.endsWith("a")).collect
    val (adultTrain, adultTest) = Random.shuffle(adult.toSeq).splitAt((adult.size / 5.0 * 4).toInt)

    val kid = user.filter(_.endsWith("k")).collect
    val (kidTrain, kidTest) = Random.shuffle(kid.toSeq).splitAt((kid.size / 5.0 * 4).toInt)

    // 将test set的数据转换成map形式，方便与solution对比，这一步会把'a'和'k'后缀移除
    val testSet = kidTest.map(_.init).foldLeft(adultTest.map(_.init -> 0).toMap) {
      (acc, x) => acc.updated(x, 1)
    }

    val solution = solve(data, adultTrain.toArray, kidTrain.toArray)

    // 预测正确的数量除以总数量
    val accuracy = testSet.filter{
      // solution不存在的user算作预测正确
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
