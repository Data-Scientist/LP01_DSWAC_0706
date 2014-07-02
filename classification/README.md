Classifying Users
============================================================

Step 1、Step 2、Step 3都比较简单，会粗略的说明一下，后面Step 4、Step 5、Step 6是算法的核心部分，会作较详细的介绍。


Step 1. Extract content items played
============================================================

```bash
hadoop jar $STREAMING -mapper kid_map.py -file kid_map.py -reducer kid_reduce.py -file kid_reduce.py -input clean -output kid

$ hadoop fs -cat kid/part-00000 | head -4  
10108881    9107,16316  
10142325    9614  
10151338a   34645  
10151338k   38467,33449,26266  
```

提取用户播放、评分、评论过的内容项。kid数据的第一列是用户ID，后跟字母'a'表示用户为'adult'，跟字母'k'表示用记为'kid'；第二列是对应用户播放、评分、评论过的内容项列表，以逗号相隔。


Step 2. Prepare the SimRank algorithm
============================================================

```bash
$ hadoop fs -cat kid/part-\* | cut -f1 | grep a > adults  
$ expr `wc -l adults | awk '{ print $1 }'` / 5  
20  
$ tail -n +21 adults | hadoop fs -put - adults_train  
$ head -20 adults | hadoop fs -put - adults_test  
$ hadoop fs -cat kid/part-\* | cut -f1 | grep k > kids  
$ expr `wc -l kids | awk '{ print $1 }'` / 5  
24  
$ tail -n +25 kids | hadoop fs -put - kids_train  
$ head -24 kids | hadoop fs -put - kids_test 
```

将kid数据'adult'用户以4:1的比例，分成训练数据集（training set）和测试数据集（test set）；以同样的方法分隔'kid'用户。顺便说下，只有这些被标记为'a'或'k'的用户，才可以作为起始结点，才可以瞬移（telport）。


Step 3. Build an adjacency matrix
============================================================

```bash
doop jar $STREAMING -mapper item_map.py -file item_map.py -reducer item_reduce.py -file item_reduce.py -input kid -output item  

$ hadoop fs -cat item/part-\* | head  
10081e1 85861225,78127887,83817844,67863534,79043502  
10081e10    10399917  
10081e11    58912004  
10081e2 58912004  
10081e3 10399917  
10081e4 10399917  
10081e5 10399917  
10081e7 10399917  
10081e8 58912004  
10081e9 58912004  
```

根据kid数据生成另外一种形式的数据item。item数据的第一列是内容项ID；第二列是播放、评分、评论过该内容项的用户ID列表。综合kid和item，就是一个邻接矩阵（adjacency matrix）。



