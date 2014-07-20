## Clustering the Session
日志记录了用户浏览电影网站的行为，我们可以通过日志的session分析，提取用户的行为的特征，将有相似行为的用户进行聚类。我们可以从聚类的结果得到有用的信息，比如有大量的用户搜索失败后，继而转向观看主页推荐的电影，还比如一些聚类的离散点可能是系统错误。
    
聚类的方法有很多。这里，我们选用cloudera ML聚类工具包。整个过程分成两个步骤：一、选取特征；二、进行聚类。大致的方法是先粗略地选取一组特征，用一个算法评估这组特征的有效性，剔除不太有用的特征，增加有用的特征，再次评估新的特征，如果新增的特征提高了整体的质量则保留，否则就剔除。通过多次的筛选，评估结果达到收敛，这时就得到了最优的特征组，再用工具包中的方法进行聚类，得到最优的聚类数。

###步骤1:  观察session有那些特征  
这里用的数据集是在之前的数据清洗步骤里得到的干净的数据集。  
hadoop fs -cat clean/part\* | head -1  

{"session": "2b5846cb-9cbf-4f92-a1e7-b5349ff08662", "hover": ["16177", "10286", "8565", "10596", "29609", "13338"],"end": "1368189995", "played": {"16316": "4990"}, "browsed": [], "recommendations": ["13338", "10759", "39122", "26996", "10002", "25224", "6891", "16361", "7489", "16316", "12023", "25803", "4286e89", "1565", "20435", "10596", "29609", "14528", "6723", "35792e23", "25450", "10143e155", "10286", "25668", "37307"], "actions": ["login"], "reviewed": {}, "start": "1368189205", "recommended": ["8565", "10759", "10002", "25803", "10286"], "rated": {}, "user": "10108881", "searched": [], "popular": ["16177", "26365", "14969", "38420", "7097"], "kid": null, "queued": ["10286", "13338"], "recent": ["18392e39"]}  

#### 我们可以得到直接的特征有：  
Actions (a feature for each, except login and logout)
Number of items hovered over  
Session duration  
Number of items played  
Number of items browsed  
Number of items reviewed  
Number of items rated  
Number of items searched  
Number of recommendations that were reviewed  
Kid (parental controls)  
Number of items queued  

一些特征很明显可以忽略掉，比如actions，用户是否登录或登出，这个对我们分析session内的用户行为不具分析价值。  

#### 我们还可以间接得到的特征有：
Mean play time  
Shortest play time  
Longest play time  
Total play time  
Total play time as fraction of session duration  
Longest play: less than 5 minutes, between 5 and 60 minutes, more than 60 minutes  
Shortest play: less than 5 minutes, between 5 and 60 minutes, more than 60 minutes  
Number of items played less than 5 minutes  
Number of items played more than 60 minutes  
Number of items hovered over that were played  
Number of browsed items that were played  
Number of reviewed items that were played  
Number of recommended items that were played  
Number of rated items that were played  
Number of searched items that were played  
Number of popular items that were played  
Number of queued items that were played  
Number of recent items that were played  
Number of recent items that were reviewed  
Number of recent items that were rated    
 
###步骤2：合并session数据
在数据清洗部分，我们根据parental controls将数据分成两个不同的部分，现在需要将它们合并起来。  
1，写一个python脚本做mapper： merge_map.py  
2，写一个python脚本做reducer: merge_reduce.py  
3，执行MapReduce

$ hadoop jar $STREAMING -mapper merge_map.py -file merge_map.py -reducer merge_reduce.py -file merge_reduce.py -input clean -output merged  

clean文件夹是数据清洗部分的结果，它的目录结构如下：  
$ hadoop fs -ls -h clean    
Found 3 items    
-rw-r--r--   1 daniel supergroup          0 2013-11-10 01:49 clean/_SUCCESS    
drwxrwxrwx   - daniel supergroup          0 2013-11-10 01:46 clean/_logs   
-rw-r--r--   1 daniel supergroup       3.1m 2013-11-10 01:49 clean/part-00000  

###步骤3：生成特征向量
####1. 写一个mapper：features_map.py
####2. 执行map-only任务，获得的特征存到features.csv中  

$ hadoop jar $STREAMING -D mapred.reduce.tasks=0 -D mapred.textoutputformat.separator=, -D stream.map.output.field.separator=, -mapper features_map.py -file features_map.py -input merged -output features0    
$ hadoop fs -getmerge features0 features.csv    
$ hadoop fs -put features.csv   

###步骤4：开始Cloudera ML工作流
####1. 新建一个header.csv文件，内容为：
session_id,identifier  
updatePassword,categorical  
updatePaymentInfo,categorical  
verifiedPassword,categorical  
reviewedQueue,categorical  
kid,categorical  
num_plays 
####2.生成概要文件
$ ml summary --summary-file summary.json --header-file header.csv --format text --input-paths features.csv  

####3. 标准化特征
$ ml normalize --summary-file summary.json --format text --id-column 0 --transform Z --input-paths features.csv --output-path part2normalized --output-type avro

###步骤5: 生成k-means++概要文件
####1. 执行ksketch,生成概要文件
$ ml ksketch --format avro --input-paths part2normalized --output-file part2sketch.avro --points-per-iteration 1000 --iterations 10 --seed 1729 
####2. 执行kmeans
$ ml kmeans --input-file part2sketch.avro --centers-file part2centers.avro --clusters 40,60,80,100,120,140,160,180,200 --best-of 3 --seed 1729 --num-threads 1 --eval-details-file part2evaldetails.csv --eval-stats-file part2evalstats.csv  
####3. 修改header.csv,增加与feature_map.py对应的新的特征
session_id,identifier  
updatePassword,categorical  
updatePaymentInfo,categorical  
verifiedPassword,categorical  
reviewedQueue,categorical  
kid,categorical  
session_duration
num_plays  
num_browsed
num_hovered
num_queued
num_recommendations
num_rated
num_reviewed
num_searched
####4.修改feature_map.py，增加新添的特征的解析
####5. 重新执行cloudear ml工作流
$ hadoop jar $STREAMING -D mapred.reduce.tasks=0 -D mapred.textoutputformat.separator=, -D stream.map.output.field.separator=, -mapper features_map.py -file features_map.py -input merged -output features1  
$ hadoop fs -getmerge features1 features.csv  
$ hadoop fs -rm features.csv  
$ hadoop fs -put features.csv  
$ ml summary --summary-file summary.json --header-file header.csv --format text --input-paths features.csv  
$ ml normalize --summary-file summary.json --format text --id-column 0 --transform Z --input-paths features.csv --output-path part2normalized --output-type avro  
$ ml ksketch --format avro --input-paths part2normalized --output-file part2sketch.avro --points-per-iteration 1000 --iterations 10 --seed 1729  
$ ml kmeans --input-file part2sketch.avro --centers-file part2centers.avro --clusters 40,60,80,100,120,140,160,180,200 --best-of 3 --seed 1729 --num-threads 1 --eval-details-file part2evaldetails.csv --eval-stats-file part2evalstats.csv  
####6. 修改header.csv，删除掉一些的特征，剩下的特征如下：
session_id,identifier  
updatePassword,categorical  
updatePaymentInfo,categorical  
verifiedPassword,categorical  
reviewedQueue,categorical  
kid,categorical  
num_plays  
num_recommendations  
num_rated  
num_reviewed  
num_searched 
####7. 修改features_map.py，仅保留6的特征的处理代码
####8. 重新执行工作流
$ hadoop jar $STREAMING -D mapred.reduce.tasks=0 -D mapred.textoutputformat.separator=, -D stream.map.output.field.separator=, -mapper features_map.py -file features_map.py -input merged -output features2  
$ hadoop fs -getmerge features2 features.csv  
$ hadoop fs -rm features.csv  
$ hadoop fs -put features.csv  
$ ml summary --summary-file summary.json --header-file header.csv --format text --input-paths features.csv  
$ ml normalize --summary-file summary.json --format text --id-column 0 --transform Z --input-paths features.csv --output-path part2normalized --output-type avro  
$ ml ksketch --format avro --input-paths part2normalized --output-file part2sketch.avro --points-per-iteration 1000 --iterations 10 --seed 1729  
$ ml kmeans --input-file part2sketch.avro --centers-file part2centers.avro --clusters 40,60,80,100,120,140,160,180,200 --best-of 3 --seed 1729 --num-threads 1 --eval-details-file part2evaldetails.csv --eval-stats-file part2evalstats.csv  
####9. 第八步的执行结果尚可，现在要一个一个增加特征，并查看特征增加后的效果，最后得到的特征集如下：
Actions (a feature for each, except login and logout)  
Number of items played  
Number of items browsed  
Number of items reviewed  
Number of items rated  
Number of items searched  
Number of recommendations that were reviewed  
Kid (parental controls)  
Total play time as fraction of session duration  
Longest play: less than 5 minutes, between 5 and 60 minutes, more than 60 minutes  
Shortest play: less than 5 minutes, between 5 and 60 minutes, more than 60 minutes  
Number of items played less than 5 minutes  
Number of items played more than 60 minutes  
Number of browsed items that were played  
Number of reviewed items that were played  
Number of recommended items that were played  
Number of rated items that were played  
Number of searched items that were played  
Number of popular items that were played  
Number of queued items that were played  
Number of recent items that were played  
Number of recent items that were reviewed  
Number of recent items that were rated  
####10. 用第九步的特征生成聚类
$ ml kassign --input-paths part2normalized --format avro --centers-file part2centers.avro --center-ids 22 --output-path part2assigned --output-type csv 
$ hadoop fs -cat part2assigned/part\* | cut -d, -f1,3 > Task2Solution.csv