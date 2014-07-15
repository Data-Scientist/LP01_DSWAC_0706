# Classifying Users

## 算法原理

算法原理简单通俗的来说，就是根据用户的相似性来进行分类，例如已知用户a为kid，如果用户b和用户a非常相似，
那么就认识用户b也是kid。两个用户的相似性是由他们共同看过的电影来决定的，共同看过的电影越多越相似。
这只是通俗的原理描述，具体的算法原理理解起来可能要稍困难一些，
当然[CCP Classifying](http://certification.cloudera.com/prep/dsc1sk/classifying.html)的代码本身是
比较简单的，就是使用下面的公式迭代循环计算，直到收敛。

![v_{k+1} = \beta M v_k + (1 - \beta) e / n](https://raw.githubusercontent.com/Data-Scientist/LP01_DSWAC_0706/master/classification/images/pagerank.png)

### SimRank

计算相似性的思路是[SimRank](http://en.wikipedia.org/wiki/SimRank)，SimRank的核心思想是
`two objects are considered to be similar if they are referenced by similar objects.` 这种思想体现在下面的公式中。

![s(a,b)=\frac {c} {\abs{I(a)}\abs{I(b)}} \sum_{i=1}^{\abs{I(a)}} \sum_{j=1}^{\abs{I(b)}} s(I_{i}(a), I_{j}(b))](https://raw.githubusercontent.com/Data-Scientist/LP01_DSWAC_0706/master/classification/images/simrank.png)

用户a和用户b的相似性`s(a,b)`由它们的`in-neighbors`的相似性来决定，而它们的`in-neighbors`的相似性又由`in-neighbors`的`in-neighbors`来决定，这是一个递归的过程。

### PageRank

SimRank是一般化的方法，更具体的来说，计算两个用户之间的相似性使用的是，略施trick的[PageRank](http://en.wikipedia.org/wiki/PageRank)。
PageRank是Google公司用来计算网页权重(Pr值)的，这貌似和计算结点之间的相似性没什么关系，别着急，先来了解下PageRank算法。

![graph 1](https://raw.githubusercontent.com/Data-Scientist/LP01_DSWAC_0706/master/classification/images/graph.png)

假设4个网页之间的链接关系如上图，图中的边表示链接关系，边的权值表示的是从一个页面转到另外一个页面的概率。
结点A有三条出边分别指向B、C、D，说明A页面中有B、C、D三条外链，
有三条外链，所以如果我们浏览的是页面A，那么从A随机转向B、C、D的概率都为1/3，所以三条外链对应的边的权值都为1/3。
结点B有两条出边分别指向C、D，说明页面B中有C、D两条外链，且外链对应的边的权值都为1/2。结点C有只一条出边指向A，说明
页面C中只有一条外链指向A，如果当前浏览的是页面C，那么只能转向A，这样外链对应的边的权值是1。同样结点D有两条出边分别指向
A、C，说明页面D中有A、C两条外链，且权值都为1/2。

![ M = \begin{bmatrix} 0 & 0 & 1 & \frac{1}{2} \\ \frac {1}{3} & 0 & 0 & 0 \\ \frac {1}{3} & \frac {1}{2} & 0 & \frac {1}{2} \\ \frac {1}{3} & \frac {1}{2} & 0 & 0 \end{bmatrix} ](https://raw.githubusercontent.com/Data-Scientist/LP01_DSWAC_0706/master/classification/images/matrix.png)

将图中的链接信息转换成矩阵形式（上图），这个矩阵称为transition matrix M，矩阵的每一列表示每个结点的outcoming links，每一行表示每个结点的incoming links，
为了便于理解，这里使用a、b、c、d作为矩阵的下标，例如，`M_{a,b}`表示第一行第二列的元素，也就是结点B到A的outcoming links，或者说是
结点A来自B的incoming links。A对应的行`M_{a,}`是结点A的所有incoming links，A对应的列`M_{,a}`是结点A的所有outcomoing links。

当你打开浏览器，你会先选择输入A、B、C、D哪个页面的网址呢？算法假设你会以同样的概率，随机打开它们中的任意一个。
共4个页面，所以刚开始的时候打开每个页面的概率都为1/4，这样就得到一个初始的distribution vector `v`。

![v_0 = \begin{bmatrix}\frac {1}{4} \\ \frac {1}{4} \\ \frac {1}{4} \\ \frac {1}{4} \end{bmatrix}](https://raw.githubusercontent.com/Data-Scientist/LP01_DSWAC_0706/master/classification/images/init-distribution-vector.png)

网站页面的重要性是由页面的incoming links来决定的，而矩阵M的每一行描述是每个结点的incoming links。
试想一下![v_1 = M v_0](https://raw.githubusercontent.com/Data-Scientist/LP01_DSWAC_0706/master/classification/images/mv0.png)是什么？
这个矩阵`M`与向量`v_{0}`相乘，可以看成是矩阵的每一行与向量做点积(dot product)运算，然后再将点积的结果作为新向量`v_1`中的值，
第一行的点积`M{a,} v_0`作为`v_1`的第一个值，第二行的点积`M{b,} v_0`作为`v_1`的第二值，．．．。更详细的来说，例如，矩阵的第一行`M{a,}`和`v_0`做点积运算，
`M{a,a}=0`，说明没有从A到A的外链，当我们浏览页面A时，就没有可能再转到A，开始时浏览页面A的概率为1/4，这样就有`0 * 1/4`结果为0
；`M{a,b} = 0`，说明没有从B指向A的外链（指向A的外链就是A的incoming link），开始时浏览页面B的概率同样为1/4，这样就有`0 * 1/4`结果为0；
`M{a,c}=1`，说明C有一条指向A的外链，且C只有这一条外链，如果当前浏览的是C，那么只能转向A，这样`1 * 1/4`结果为1/4；
`M{a,d}=1/2`，说明D有一条指向A的外链，开始时浏览页面D的概率为1/4，`1/2 * 1/4`结果为1/8。做点积时要将这些结果都加起来，
0 + 0 + 1/4 + 1/8 = 3/8，再将3/8赋值给v_1的第一个元素，这个值也就是下一个状态浏览结点A的概率为3/8。
其它行`M{b,}`、`M{c,}`、`M{d,}`的点积也是同样的道理。

这样就可以知道![v_1 = M v_0](https://raw.githubusercontent.com/Data-Scientist/LP01_DSWAC_0706/master/classification/images/mv0.png)
是下一个状态的distribution vector，也就是下一个状态我们可能访问各个页面的概率。从上面的计算描述中，可以知道如果
结点的incoming links越多，且incoming links对应的权值越大，就越可能被浏览到，页面的Pr值也就越高。上面这样计算一次，
也就相当于使用`M`影响一次初始的`v_0`，因为页面最终的权重(Pr值)是要由M来决定的，所以M影响的次数越多，最终的结果就越准确。
于是就有了下面的迭代计算公式。

![v_{k+1}=M v_k](https://raw.githubusercontent.com/Data-Scientist/LP01_DSWAC_0706/master/classification/images/mvk.png)

使用上面的公式迭代计算几十次之后，得到下面的结果，从结果中可以看出A的权重最高，这个结果是非常合理的，
虽然C的incoming links比A多，但如果当前浏览的是C那么只能转向A，也就是说A享有C享有的被访问概率，而且自己还有除C以外的incoming links。

![v = \begin{bmatrix} 0.3870968 \\ 0.1290323 \\ 0.2903226 \\ 0.1935484 \end{bmatrix}](https://raw.githubusercontent.com/Data-Scientist/LP01_DSWAC_0706/master/classification/images/distribution-vector-result.png)

#### 特殊情况处理

上面举例子是比较理想化的情况(结点之间是全连通)，实际的应用中会有Dead Ends、Spider Traps。

#### Dead Ends

![graph-dead-end](https://raw.githubusercontent.com/Data-Scientist/LP01_DSWAC_0706/master/classification/images/graph-dead-end.png)

像上图这样，把C的出边给拿掉，这样C就成dead end。这样使用上述公式计算时，结果向量v中的所有值都会越来越接近0，这样的结果显然是不合理的。
解决的办法是，先迭代拿掉所有的Dead End，然后再计算结果，之后用计算出的结果推导出dead end的权重。
因为CCP项目中，全部链接都是双向的，没有Dead Ends存在，这里具体的方法就不介绍了，有兴趣的朋友可以
查看[Link Analysis 5.1.4](http://infolab.stanford.edu/~ullman/mmds/ch5.pdf)。


#### Spider Trap

![graph-spider-trap](https://raw.githubusercontent.com/Data-Scientist/LP01_DSWAC_0706/master/classification/images/graph-spider-trap.png)

结点C没有任何outcoming links，但有一条指向自身的边，这种情况就是spider trap。对于这种情况，使用公式计算的时候，C结点对应的权重值会
越来越大（最大为1），其它结点对应的值会越来越接近0。这是因为算法会绕着这个trap一直转，消耗掉大量的概率值。上图是一个结点的trap，
还可能有多个结点组成的trap，概率值进入这个trap之后就出不去了，然后就绕着这几个结点组成的圈一直转。当然算法迭代的过程中，
还有trap以外的结点，概率还会通过这些结点来继续往外扩散，但进入trap的这部分概率会被这几个trap中的结点消耗掉。
因为trap只有入口，没有出口，所以迭代的过程中还会继续有概率进入这个trap，最终概率会被这几个trap中的结点耗尽。

```r
v0 = c(1/4, 1/4, 1/4, 1/4)
M = matrix(c(0, 1/3, 1/3, 1/3,  0, 0, 1/2, 1/2,  0, 0, 1, 0,  1/2, 0, 1/2, 0), nrow=4, ncol=4)
v = v0

cat(v, '\n')
for (i in 1:100) {
	v = M %*% v
	cat(v, '\n')
}
```

有兴趣的朋友可以使用R跑一下上面代码，你会发现第3个值，也就是结点C对应的值会越来大，最后几乎接就是1，其它结点对应的值
几乎变成了0。这也个结果也就是说你只有可能访问C页面，其它结点没有可能被访问，这个结果显然不合理。
解决的办法是，在状态改变时，让浏览者有一定的概率发生瞬移(telport)，这样就避免了一直在spider trap中线圈。
最终得到如下公式。

![pagerank final](https://raw.githubusercontent.com/Data-Scientist/LP01_DSWAC_0706/master/classification/images/pagerank.png)

`beta`是一个常量通常为0.8，`1 - beta`就是发生瞬移(telport)的机率，`e`是和`v`同大小的所有值都为1的向量，`n`是结点的数量。

### 分类

PageRank算法已经清楚了，现在说下具体怎样才能使用PageRank进行分类。[CCP Classifying](http://certification.cloudera.com/prep/dsc1sk/classifying.html)
中要对用户进行分类，分类是根据用户看过的电影来分的，这样整个数据构成的网络就与互联网不同了，
这个网络中有两种结点：用户结点、电影结点（而互联网中只有一种结点），相似的用户结点通过电影结点给连接了起来。
 两个用户越相似它们之间的链接就越多（两个相似用户之间并没有直接的链接，
是间接的通过电影结点连在一起的，而这条将两个相似用户连起来的链，基本上都是由很多用户电影结点给串起来的）。

从[Exploring the Data Set](http://certification.cloudera.com/prep/dsc1sk/exploring.html)我们已经知道，可以根据用户session发生的家长控制事件先标记分类出一些用户，
然后把这些用户作为训练样本，找出和训练样本相似的用户结点，那么就可以认为这些相似的结点和训练样本中的结点是同一类型，就可以分类了。
PageRank算法是给所有结点同样均等的概率值，然后根据网页链接的结构往外扩散这些概率值，最终获取概率值大的结点，权值就高。
试想一下，如果我们只给已经分类的同类型用户结点（训练样本）一定的概率值，然后再根据用户和电影之间的链接关系往外扩散，那么
最终概率值越大的用户结点，也就越和训练本中的结点相似。

首先我们先给训练样本中的所有kid结点一定的概率，迭代计算，找出所有和这些kid结点相似的用户结点，认为这些相似用户结点也是kid。
再给训练样本中的所有adult结点一定的概率，迭代计算，找出所有和这些adult结点相似的用户结点，认为这些的用户结点也是adult。
举个例子吧，为了简单，图中被标记出的用户结点只有u2k，以'k'为后缀，表示u2为kid。
u1看过电影m1和m2，同样u2k也看过电影m1和m2，u3看过电影m2和m3。

![graph ccp](https://raw.githubusercontent.com/Data-Scientist/LP01_DSWAC_0706/master/classification/images/graph-ccp.png)

根据图我们得到如下的transition matrix M，M的下标分别为u1、u2k、u3、m1、m2、m3。

![\begin{bmatrix} 0 & 0 & 0 & \frac{1}{2} & \frac{1}{3} & 0 \\ 0 & 0 & 0 & \frac{1}{2} & \frac{1}{3} & 0 \\ 0 & 0 & 0 & 0 & \frac{1}{3} & 1 \\ \frac{1}{2} & \frac{1}{2} & 0 & 0 & 0 & 0 \\ \frac{1}{2} & \frac{1}{2} & \frac{1}{2} & 0 & 0 & 0 \\ 0 & 0 & \frac{1}{2} & 0 & 0 & 0 \end{bmatrix} ](https://raw.githubusercontent.com/Data-Scientist/LP01_DSWAC_0706/master/classification/images/matrix-ccp.png)

因为只给训练样本中的同类型结点初始概率，也就是只能从u2k开始扩散，所以distribution vector v如下。

![\begin{bmatrix} 0 \\ 1 \\ 0 \\ 0 \\ 0 \\ 0 \end{bmatrix}](https://raw.githubusercontent.com/Data-Scientist/LP01_DSWAC_0706/master/classification/images/init-vector-cpp.png)

```r
M = matrix(c(  0,   0,   0, 1/2, 1/2,   0, 
	       0,   0,   0, 1/2, 1/2,   0,
	       0,   0,   0,   0, 1/2, 1/2, 
	     1/2, 1/2,   0,   0,   0,   0,
	     1/3, 1/3, 1/3,   0,   0,   0,
	       0,   0,   1,   0,   0,   0),
	   nrow=6, ncol=6)
v0 = c(0, 1, 0, 0, 0, 0)
v = v0
beta = 0.8
telport = (1.0 - beta) / length(v)

cat(v, '\n')
for (i in 1:50) {
	# 瞬移(telport)只能发生训练样本结点
	v_2 = beta * (M[2,] %*% v) + telport
	v = beta * (M %*% v)
	v[2] = v_2
	cat(v, '\n')
}
```

使用之前说的公式式来迭代计算50次（上面是一段迭代计算的R代码，有兴趣的朋友可以自己跑一下），结果如下。

![v_{50} = \begin{bmatrix} 0.02237178 \\ 0.05570512 \\ 0.01452865 \\ 0.03122695 \\ 0.03703651 \\ 0.005809555 \end{bmatrix}](https://raw.githubusercontent.com/Data-Scientist/LP01_DSWAC_0706/master/classification/images/distribution-vector-50-ccp.png)

distribution vector后3个数可以忽略，因为它们是电影对应的值，前3个数分别对应u1、u2k、u3的相似值，u2k对应的值最大，
这也可以理解，自己和自己当然是最相似的，其次是u1，这说明样本u2k和u1最相似，这从图中也可能直接看出，u1和u2k都
看过电影m1、m2，所以这里我们就可以认为u1也是kid。具体实现算法的时候会设置一个阀值(threshold)，
使用kid和adult的训练样本各迭代计算一次，然后将两个计算出的结果normalize之后合并，然后根据threshold进行分类。


## 具体实现

算法已经清楚了，具体的实现就简单了。

### Extract content items played

提取用户播放、评分、评论过的内容项。kid数据的第一列是用户ID，后跟字母`a`表示用户为`adult`，跟字母`k`表示用记为`kid`；第二列是对应用户播放、评分、评论过的内容项列表，以逗号相隔。


### Prepare the SimRank algorithm

将数据中的adult用户以4:1的比例，分成训练数据集（training set）和测试数据集（test set）；以同样的方法分隔kid用户。顺便说下，只有这些被标记为`a`或`k`的用户，才可以作为起始结点，才可以瞬移（teleport）。


### Build an adjacency matrix


根据kid数据生成另外一种形式的数据item。item数据的第一列是内容项ID；第二列是播放、评分、评论过该内容项的用户ID列表。综合kid和item，就是一个邻接矩阵（adjacency matrix）。


### Implement the SimRank algorithm

![v_{k+1} = \beta M v_k + (1 - \beta) e / n](https://raw.githubusercontent.com/Data-Scientist/LP01_DSWAC_0706/master/classification/images/pagerank.png)

`v` is distribution vector, `M` is transition matrix, `beta`是常量（这里取值0.8），`e`是和`v`同大小的所有值都为1的向量，`n`是训练样本的数量。
[CCP Classifying](http://certification.cloudera.com/prep/dsc1sk/classifying.html)整个代码就是围绕这个公式来计算的。

`simrank_map.py`和`simrank_reducer.py`实现了上述的公式，使用hadoop streaming调用`simrank_map.py`和`simrank_reducer.py`来进行
算法计算。 `simrank.sh`用来循环执行这一过程，循环一次迭代计算一次，循环的时候使用`simrank_diff.py`来
比较新生成的`v_{k+1}`和之前的`v_{k}`，如果差值足够小，就说明收敛了，结束循环。

```bash
# 根据kids训练样本计算，结果中，值越大就说明越可能是kid
./simrank.sh kids_train  
# 根据adults训练样本计算，结果中，值越大就说明越可能是adult
./simrank.sh adults_train  
```

这样我们得到adult_final和kid_final，再将这两个向量合并，因为训练样本数量大的，单个结点的影响力就小，
要使它们的影响力相同，需要normalize，`adult_v = adult_v * (len(adults_train) / len(kids_train))`，
因为adult和kid是对立的两个类型，合并的时候，要将adult向量中的值乘以-1。
再设定threshold进行分类，向量中值大于threshold的为kid，小于threshold的为adult。

分类结果出来之后，使用test set验证，正确率非常高，说明这种算法可行，没有必要再使用logistic regression了。
然后，使用全部的已经被标记的数据（训练集和测试集），作为训练集重新计算，得到最终的结果。

## 参考资料

[chapter 5 in Mining Massive Data Sets](http://i.stanford.edu/~ullman/mmds/ch5.pdf)。

## 其它

这个目录下的代码中添加了一点注释，有兴趣的可以看看。
