Classifying Users
============================================================

这部分的代码本身其实是很简单的，只要对Python和Bash有一定的了解就应该能看得懂代码本身。重要的是代码背后的算法原理，所以下面先来介绍算法原理。

分类使用的算法是[SimRank](http://en.wikipedia.org/wiki/SimRank)，这个算法的核心思想就是"two objects are considered to be similar if they are referenced by similar objects."，这种思想体现在下面的公式中。

![s(a,b)=\frac {c} {\abs{I(a)}\abs{I(b)}} \sum_{i=1}^{\abs{I(a)}} \sum_{j=1}^{\abs{I(b)}} s(I_{i}(a), I_{j}(b))](http://www.sciweavers.org/tex2img.php?eq=s%28a%2Cb%29%3D%5Cfrac%20%7Bc%7D%20%7B%5Cabs%7BI%28a%29%7D%5Cabs%7BI%28b%29%7D%7D%20%5Csum_%7Bi%3D1%7D%5E%7B%5Cabs%7BI%28a%29%7D%7D%20%5Csum_%7Bj%3D1%7D%5E%7B%5Cabs%7BI%28b%29%7D%7D%20s%28I_%7Bi%7D%28a%29%2C%20I_%7Bj%7D%28b%29%29&bc=White&fc=Black&im=jpg&fs=12&ff=arev&edit=0)

a和b的相似性s(a,b)由它们的in-neighbors的相似性来决定，而它们的in-neighbors的相似性又由in-neighbors的in-neighbors来决定，这是一个递归的过程。

Extract content items played
------------------------------------------------------------

```bash
$ hadoop jar $STREAMING -mapper kid_map.py -file kid_map.py -reducer kid_reduce.py -file kid_reduce.py -input clean -output kid

$ hadoop fs -cat kid/part-00000 | head -4  
10108881    9107,16316  
10142325    9614  
10151338a   34645  
10151338k   38467,33449,26266  
```

提取用户播放、评分、评论过的内容项。kid数据的第一列是用户ID，后跟字母'a'表示用户为'adult'，跟字母'k'表示用记为'kid'；第二列是对应用户播放、评分、评论过的内容项列表，以逗号相隔。


Prepare the SimRank algorithm
------------------------------------------------------------

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

将kid数据中的'adult'用户以4:1的比例，分成训练数据集（training set）和测试数据集（test set）；以同样的方法分隔'kid'用户。顺便说下，只有这些被标记为'a'或'k'的用户，才可以作为起始结点，才可以瞬移（telport）。


Build an adjacency matrix
------------------------------------------------------------

```bash
$ hadoop jar $STREAMING -mapper item_map.py -file item_map.py -reducer item_reduce.py -file item_reduce.py -input kid -output item  

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



