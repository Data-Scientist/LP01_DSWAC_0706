# Classifying Users

## 算法原理

这部分的代码本身其实是很简单的，只要对Python和Bash有一定的了解就应该能看得懂代码本身。
重要的是代码背后的算法原理。算法是根据两个用户的相似性来进行分类的。
例如：已知用户a为kid，如果b和a非常相似，那么就认为b也是kid。

### SimRank

计算相似性使用的算法是[SimRank](http://en.wikipedia.org/wiki/SimRank)，这个算法的核心思想就是
"two objects are considered to be similar if they are referenced by similar objects."，这种思想体现在下面的公式中。

![s(a,b)=\frac {c} {\abs{I(a)}\abs{I(b)}} \sum_{i=1}^{\abs{I(a)}} \sum_{j=1}^{\abs{I(b)}} s(I_{i}(a), I_{j}(b))](https://raw.githubusercontent.com/Data-Scientist/LP01_DSWAC_0706/master/classification/images/simrank.png)

a和b的相似性s(a,b)由它们的in-neighbors的相似性来决定，而它们的in-neighbors的相似性又由in-neighbors的in-neighbors来决定，这是一个递归的过程。

### PageRank

用的思路是SimRank的，但更具体的来说，这个算法其实是加了点trick的[PageRank](http://en.wikipedia.org/wiki/PageRank)。
PageRank算法是衡量网页的权重的，也就是网络优化时经常讲到的pr值。权重为什么可以衡量结点之前的相似性，这是因为使用了一个trick：
起始结点只能是训练样本（training set）中的用户结点，这些结点是已经被标记出'kid'或'adult'的，瞬移(teleport)的目标结点也只能是这些结点。
这样以来整个过程就是从已经被标记为'kid'（'adult'）的结点出发，不断向外扩散，扩散的同时会不断更新计算扩散到的结点的被访问的概率。
这个过程也类似于[Markov chain](http://en.wikipedia.org/wiki/Markov_chain)。
因为是从训练样本中的用户结点出发的，所以最终结果中被访问的概率越大的结点，就越和训练样本中的结点相似，也就越可能是'kid'（'adult'）。

 
更详细的描述，参见[Lecture #3: PageRank Algorithm - The Mathematics of Google Search](http://www.math.cornell.edu/~mec/Winter2009/RalucaRemus/Lecture3/lecture3.html)、[chapter 5 in Mining Massive Data Sets](http://i.stanford.edu/~ullman/mmds/ch5.pdf)。


## 具体实现

### Extract content items played

提取用户播放、评分、评论过的内容项。kid数据的第一列是用户ID，后跟字母'a'表示用户为'adult'，跟字母'k'表示用记为'kid'；第二列是对应用户播放、评分、评论过的内容项列表，以逗号相隔。


### Prepare the SimRank algorithm

将kid数据中的'adult'用户以4:1的比例，分成训练数据集（training set）和测试数据集（test set）；以同样的方法分隔'kid'用户。顺便说下，只有这些被标记为'a'或'k'的用户，才可以作为起始结点，才可以瞬移（telport）。


### Build an adjacency matrix


根据kid数据生成另外一种形式的数据item。item数据的第一列是内容项ID；第二列是播放、评分、评论过该内容项的用户ID列表。综合kid和item，就是一个邻接矩阵（adjacency matrix）。


### Implement the SimRank algorithm

![v\prime = \beta M v + (1 - \beta)e/n](https://raw.githubusercontent.com/Data-Scientist/LP01_DSWAC_0706/master/classification/images/pagerank.png)

'v' is distribution vector, 'M' is transition matrix.

为了处理没有出边的结点(dead ends)和Spider traps，公式后半部分添加了teleport。

举个例子，假设kid中的数据为:

```csv
u1k  m1,m2
u2   m1,m2,m3
u3k  m3
```

item中的数据为：

```csv
m1  u1k,u2
m2  u1k,u2
m3  u2,u3k
```

那么训练集(training set)中的数据就是:

```csv
u1k
u3k
```

因为训练集中只有两个用户，选择它们任何一个的概率都为1/2，所以初始的distribution vector为：

![v = \begin{bmatrix}\frac {1}{2} \\ 0 \\ \frac {1}{2} \\ 0 \\ 0 \\ 0 \end{bmatrix}](https://raw.githubusercontent.com/Data-Scientist/LP01_DSWAC_0706/master/classification/images/v.png)

v(1)是选择u1k的概率，... ，v(4)是选择m1的概率，...。类推，先user，后item。

从kid和item的信息中可知，u1k的两条出边(out-links)分别指向m1和m2，这样从u1k到m1和m2的概率都是1/2，其它结点类推。这样可知transition matrix为：

![ v = \begin{bmatrix} 0 & 0 & 0 & \frac {1}{2} & \frac{1}{2} & 0 \\ 0 & 0 & 0 & \frac {1}{2} & \frac {1} {2} & \frac {1}{2} \\ 0 & 0 & 0 & 0 & 0 & \frac {1}{2} \\ \frac {1}{2} & \frac {1}{3} & 0 & 0 & 0 & 0 \\ \frac {1}{2} & \frac {1}{2} & 0 & 0 & 0 & 0 \\ 0 & \frac {1}{3} & 1 & 0 & 0 & 0 \end{bmatrix} ](https://raw.githubusercontent.com/Data-Scientist/LP01_DSWAC_0706/master/classification/images/transition_matrix.png)

实现代码中 ![\beta = 0.8](https://raw.githubusercontent.com/Data-Scientist/LP01_DSWAC_0706/master/classification/images/beta.png) ，n也就是训练样本的大小，这样所有的数据都有了，就可以进行计算了。

`simrank.sh` 循环调用hadoop streaming，每循环一次，就使用Page Rank的公式计算一次distribution vector，也就相当于向外扩散了一次，也就相当于Markov chain改变了一次状态。当distribution vector的改变足够小时，说明收敛了，计算结束。

因为kids_train和adults_train的大小不一样，所以需要normalize，也就是将adults_train的distribution vector中的每个值乘以factor，factor就是adults_trains的大小除以kids_trains的大小。
adult和kid是对立的，我们将adults的distribution vector值乘以-1，这样值越大就说明越和kid相似，越小就越和adult相似。
我们可以使用一个threshold，当小于这个值，为归类到adult，大小这个值就归类到kid。
