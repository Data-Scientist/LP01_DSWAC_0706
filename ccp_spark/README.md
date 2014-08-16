# 使用spark重写ccp

## Spark安装

### 二进制版本

根据目标系统的hadoop版本，下载对应的spark二进制版本。

```bash
# 查看hadoop版本
hadoop version
```

[Spark-1.0.2-bin-cdh4.tgz](http://d3kbcqa49mib13.cloudfront.net/spark-1.0.2-bin-cdh4.tgz)


### 自己编译spark (可选)

下载源代码[Spark 1.0.2](http://d3kbcqa49mib13.cloudfront.net/spark-1.0.2.tgz)。

将project/SparkBuild.scala中的hadoop版本信息，修改成目标系统中的hadoop版本。

```bash
val DEFAULT_HADOOP_VERSION = "2.0.0-cdh4.4.0"
```

使用sbt编译，编译的过程中需要下载依赖的类库，如果网速较慢，编译可能会需要比较长的时间。


```bash
# 编译（需要切换到源代码的根目标）
sbt/sbt assembly
```

使用maven也可以编译，参见[Building Spark with Maven](https://spark.apache.org/docs/latest/building-with-maven.html)。

### 使用（可选）

参见[Quick Start](https://spark.apache.org/docs/latest/quick-start.html)


## Classifying Users

目前只使用spark重写了这一部分，具体参见源代码，时间仓促，写得不妥的地方，欢迎大家指正。

### 运行

我的写代码的环境是vim + sbt，写完代码使用`sbt`打包生成jar，然后再把jar包传到spark服务器，再使用spark-submit来运行jar包。
也就是说写代码的环境（只需要sbt），可以和服务器分离。如果不习惯使用vim写代码，可以使用
[sbteclipse](https://github.com/typesafehub/sbteclipse)生成eclipse项目文件，再导入eclipse来编写代码。

```bash
# 打包
sbt package
```

把jar包上传到服务器，使用winscp或其它的什么方法，方法太多了，在服务器中用spark-submit来运行。

```bash
# 运行，测试算法预测的准确性
./bin/spark-submit --class "org.lords.classification.UserClassifier" --master local[4]
  ~/classifying-users_2.10-1.0.jar test hdfs://127.0.0.1:8020/user/cloudera/clean/
```

`--master local[4]`是[master-urls](https://spark.apache.org/docs/latest/submitting-applications.html#master-urls)，
`~/classifying-user_2.10-1.0.jar`是打包生成的jar文件，`test`是传给`UserClassifier`的第一个参数，表示要测试算法的预测准确率，
`hdfs://127.0.0.1:8020/user/cloudera/clean/`是传给`UserClassifier`的第二参数，这里是
[Cleaning the Data](http://certification.cloudera.com/prep/dsc1sk/cleaning.html)生成的干净数据。

```bash
# 运行，计算分类
./bin/spark-submit --class "org.lords.classification.UserClassifier" --master local[4]
  ~/classifying-users_2.10-1.0.jar classify hdfs://127.0.0.1:8020/user/cloudera/clean/ solution
```

`classify`表示要进行分类计算，`solution`是生成解的存放文件，其它同上。

## Clustering the Sessions

这周时间有限，只实现了很少的一点代码，下周可能时间也不会太多，下下周估计会好一些。
