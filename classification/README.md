# Classifying Users

## 算法原理

这部分的代码本身其实是很简单的，只要对Python和Bash有一定的了解就应该能看得懂代码本身。
重要的是代码背后的算法原理，通俗的来说算法是根据两个用户的相似性来进行分类的，
例如：已知用户a为kid，如果用户b和用户a非常相似，那么就认为用户b也是kid。

### SimRank

计算相似性使用的算法是[SimRank](http://en.wikipedia.org/wiki/SimRank)，这个算法的核心思想就是
`two objects are considered to be similar if they are referenced by similar objects.`，这种思想体现在下面的公式中。

![s(a,b)=\frac {c} {\abs{I(a)}\abs{I(b)}} \sum_{i=1}^{\abs{I(a)}} \sum_{j=1}^{\abs{I(b)}} s(I_{i}(a), I_{j}(b))](https://raw.githubusercontent.com/Data-Scientist/LP01_DSWAC_0706/master/classification/images/simrank.png)

用户a和用户b的相似性s(a,b)由它们的`in-neighbors`的相似性来决定，而它们的`in-neighbors`的相似性又由`in-neighbors`的`in-neighbors`来决定，这是一个递归的过程。

### PageRank

项目中用的思路是SimRank算法的，这没错，但更具体的来说，这个算法其实是加了点trick的[PageRank](http://en.wikipedia.org/wiki/PageRank)。
PageRank算法是衡量网页的权重的，也就是做网络优化时经常提到的pr值。通常权重是衡量网页的重要性的，但这里为什么可以用来衡量结点之前的相似性，这是因为使用了一个trick：
起始结点只能是训练样本（training set）中的用户结点，这些结点是已经被标记为kid或adult的结点，瞬移(teleport)的目标结点也只能是这些结点。
这样以来整个过程就是从已经被标记为kid(adult)的结点出发，根据结点之间的关系链（电影被某用户看过，那么这电影就和用户之间有了关系链，链是双向的），
不断向外扩散，扩散的同时会不断更新计算扩散到的结点的可能被访问的概率。我不知道在这里用`扩散`来形容这个过程恰不恰当，
这么说吧，训练样本决定了初始的distribution vector（简称`v`），所有的用户结点和电影结点组成的拓扑结构决定了transition matrix（简称`M`），
这个过程也是`v`和`M`不断相乘的过程`v = vM`，相乘的过程中不断的用`M`的结构信息影响更新`v`的值，使`v`的值更加合理，
最终`v`中的值就会是一个精确度比较高的distribution信息。
这个过程也类似于[Markov chain](http://en.wikipedia.org/wiki/Markov_chain)，每扩散一次，相当于在Markov Chain中从一个状态转到另一个状态。
因为这个过程是从已经被标记为kid(adult)的结点出发的，所以最终结果中可能被访问的概率越大的结点，就越和训练样本中的结点相似，也就越可能是kid(adult)。

 
算法更详细的描述 [PageRank Algorithm - The Mathematics of Google Search](http://www.math.cornell.edu/~mec/Winter2009/RalucaRemus/Lecture3/lecture3.html)、[chapter 5 in Mining Massive Data Sets](http://i.stanford.edu/~ullman/mmds/ch5.pdf)。


## 具体实现

### Extract content items played

提取用户播放、评分、评论过的内容项。kid数据的第一列是用户ID，后跟字母`a`表示用户为`adult`，跟字母`k`表示用记为`kid`；第二列是对应用户播放、评分、评论过的内容项列表，以逗号相隔。


### Prepare the SimRank algorithm

将数据中的adult用户以4:1的比例，分成训练数据集（training set）和测试数据集（test set）；以同样的方法分隔kid用户。顺便说下，只有这些被标记为`a`或`k`的用户，才可以作为起始结点，才可以瞬移（teleport）。


### Build an adjacency matrix


根据kid数据生成另外一种形式的数据item。item数据的第一列是内容项ID；第二列是播放、评分、评论过该内容项的用户ID列表。综合kid和item，就是一个邻接矩阵（adjacency matrix）。


### Implement the SimRank algorithm

![v\prime = \beta M v + (1 - \beta)e/n](https://raw.githubusercontent.com/Data-Scientist/LP01_DSWAC_0706/master/classification/images/pagerank.png)

`v` is distribution vector, `M` is transition matrix. 
为了避免没有出边的结点(dead ends)和Spider traps造成的计算错误，公式后半部分添加了teleport。就这个项目来讲，是没有dead ends，因为所有边都是双向的，
但又可能出现孤立的结点，例如一个用户没有看过、评价、评论过任何电影，那么这个用户就是一个孤立的结点。

举个例子，u1k看过m1和m2两部电影，u2看过m1/m2/m3三部电影，u3k看过电影m3，那么对应的`kid`中的数据为：

```csv
u1k  m1,m2
u2   m1,m2,m3
u3k  m3
```

生成的`item`中的数据为：

```csv
m1  u1k,u2
m2  u1k,u2
m3  u2,u3k
```

已经u1k和u3k为kid，那么训练集(training set)中的数据就是：

```csv
u1k
u3k
```

因为训练集中只有两个用户，起始结点只能选择训练集中的用户，所以选择它们任何一个的概率都为`1/2`，所以初始的distribution vector为：

![v = \begin{bmatrix}\frac {1}{2} \\ 0 \\ \frac {1}{2} \\ 0 \\ 0 \\ 0 \end{bmatrix}](https://raw.githubusercontent.com/Data-Scientist/LP01_DSWAC_0706/master/classification/images/v.png)

v(1)是选择u1k的概率，... ，v(4)是选择m1的概率，...。类推，先`user`，后`item`。

从`kid`和`item`的信息中可知，u1k的两条出边(out-links)分别指向m1和m2，这样从u1k到m1和m2的概率都是1/2，其它结点类推。这样可知transition matrix为：

![ v = \begin{bmatrix} 0 & 0 & 0 & \frac {1}{2} & \frac{1}{2} & 0 \\ 0 & 0 & 0 & \frac {1}{2} & \frac {1} {2} & \frac {1}{2} \\ 0 & 0 & 0 & 0 & 0 & \frac {1}{2} \\ \frac {1}{2} & \frac {1}{3} & 0 & 0 & 0 & 0 \\ \frac {1}{2} & \frac {1}{2} & 0 & 0 & 0 & 0 \\ 0 & \frac {1}{3} & 1 & 0 & 0 & 0 \end{bmatrix} ](https://raw.githubusercontent.com/Data-Scientist/LP01_DSWAC_0706/master/classification/images/transition_matrix.png)

实现代码中 ![\beta = 0.8](https://raw.githubusercontent.com/Data-Scientist/LP01_DSWAC_0706/master/classification/images/beta.png) ，公式中的`n`也就是训练样本的大小，这样所有的数据都有了，就可以进行计算了。

`simrank.sh` 循环调用hadoop streaming，每循环一次，就使用Page Rank的公式计算一次distribution vector，也就相当于使用`M`的拓扑结构信息影响了一次`v`，也就相当于Markov chain改变了一次状态。当distribution vector的改变足够小，就说明算法收敛了，计算结束。

因为kids_train和adults_train的大小不一样，所以需要normalize，也就是将adults_train的distribution vector中的每个值乘以factor，factor就是adults_trains的大小除以kids_trains的大小。
adult和kid是对立的，我们将adults的distribution vector值乘以-1，这样值越大就说明越和kid相似，越小就越和adult相似。
我们可以使用一个threshold，当小于这个值，为归类到adult，大小这个值就归类到kid。
