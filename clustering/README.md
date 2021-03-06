## K-Means

[k-means](http://en.wikipedia.org/wiki/K-means_clustering)算法是最流行的数据挖掘算法之一，
算法也非常的简单，引用[Scalable K-Means++](http://theory.stanford.edu/~sergei/papers/vldb12-kmpar.pdf)
中的一段话来描述k-means算法。

Starting with a set of randomly chosen initial centers,
one repeatedly assigns each input point to its nearest center, and then recomputes the centers given the
point assignment.

另外wikipedia [k-means clustering](http://en.wikipedia.org/wiki/K-means_clustering)的三个公式对这个算法描述得也非常到位。


再引用[Scalable K-Means++](http://theory.stanford.edu/~sergei/papers/vldb12-kmpar.pdf)中的话来详细的描述一下k-means，
里面提到的公式定义会在下面描述K-means++算法时使用到。

Let <span class='inline-formula'>![X = \{x_1, ... , x_n\}](http://heming-keh.github.io/assets/formula/2014/07/25/kmeans/1456c838119b250b0f0f79a7187019cb.png)</span> be a set of points in the d-dimensional Euclidean space and
let *k* be a positive integer specifying the number of clusters. Let 
<span class='inline-formula'>![\|x_i - x_j\|](http://heming-keh.github.io/assets/formula/2014/07/25/kmeans/189d65ea689ab8abd354c7c4883f9241.png)</span> denote the Euclidean distance between <span class='inline-formula'>![x_i](http://heming-keh.github.io/assets/formula/2014/07/25/kmeans/1ba8aaab47179b3d3e24b0ccea9f4e30.png)</span> and <span class='inline-formula'>![x_j](http://heming-keh.github.io/assets/formula/2014/07/25/kmeans/1f89889020cdc84d9e1c35237cb62f65.png)</span>.
For a point *x* and a subset <span class='inline-formula'>![Y \subseteq X](http://heming-keh.github.io/assets/formula/2014/07/25/kmeans/d15027b469fb5bca4fc0d2c8aefc8f71.png)</span> of points, the distance is
defined as <span class='inline-formula'>![d(x, Y) = min_{y \in Y} \ \|x - y\|](http://heming-keh.github.io/assets/formula/2014/07/25/kmeans/3872f327bea103b05558609a6c718189.png)</span>. For a subset <span class='inline-formula'>![Y \subseteq X](http://heming-keh.github.io/assets/formula/2014/07/25/kmeans/d15027b469fb5bca4fc0d2c8aefc8f71.png)</span>
of points, let centroid be given by

<div class='formula'><img src='http://heming-keh.github.io/assets/formula/2014/07/25/kmeans/0f04bffb8ff8ddf05bb24e453e9f7be2.png' alt=' centroid(Y) = \frac {1} {\abs{Y}} \sum_{y \in Y} y'></img></div>

Let <span class='inline-formula'>![C = \{c_1, ..., c_k\}](http://heming-keh.github.io/assets/formula/2014/07/25/kmeans/14b35c77879f55a3fac89e8ad7024b11.png)</span> be a set of points and let <span class='inline-formula'>![Y	\subseteq X](http://heming-keh.github.io/assets/formula/2014/07/25/kmeans/9a3b89cefa1f5cdef76ea75510732616.png)</span>.
We define the cost of *Y* with respect to *C* as

<div class='formula'><img src='http://heming-keh.github.io/assets/formula/2014/07/25/kmeans/2add97353a4f23e6063bce1667531500.png' alt='\phi_{Y}(C) = \sum_{y \in Y} d^{2} (y, C) = \sum_{y \in Y} \min_{i=1,...,k} \|y - c_i\|^2'></img></div>

The goal of k-means clustering is to choose a set *C* of *k* centers
to minimize <span class='inline-formula'>![\phi_{X}(C)](http://heming-keh.github.io/assets/formula/2014/07/25/kmeans/669f4d6d9f7f55799f3c871d192fccb7.png)</span>.


但在使用k-means算法之前，你需要先解决两个问题：**选择k值**、**初始化centroids**。 
k-means算法最后能否得到满意的结果，很大程度上取决于这两个问题解决的得当与否。


## 初始化centroids

随机选择centroids是种比较简单常见的方法，但它会出现下面这样的情况，选择出的k个centroids没有正好
分别落在k个clusters中，像下面第一个图那样，对于随机选择来说，这种情况太常见了。这样就导致最终的结果只是一个局部最优解，如下面第二个图。
图是使用[K-means applet](http://www.math.le.ac.uk/people/ag153/homepage/KmeansKmedoids/Kmeans_Kmedoids.html)生成的。

![random-init-clusters](http://heming-keh.github.io/assets/images/2014/07/25/kmeans/random-init-clusters.png)

![random-init-clusters-result](http://heming-keh.github.io/assets/images/2014/07/25/kmeans/random-init-clusters-result.png)


### k-means++

K-means++算法解决了这个问题，算法刚开始时先随机选择出一个centroid，放到集合*C*中，
随后选择centroids时，会参照集合*C*，距离*C*中的点远的*x*拥有更高的被选择的机率，每次新选出的centroid
都要放到集合*C*中，供后续选择centroids作参考。这样选择出的k个centroids就会尽可能的分散到
数据的每个cluters。

![k-means++](http://heming-keh.github.io/assets/images/2014/07/25/kmeans/k-means++.png)

但这个算法每次选择centroid都需要遍历整个数据集*X*，去计算<span class='inline-formula'>![\phi_{X}(C)](http://heming-keh.github.io/assets/formula/2014/07/25/kmeans/669f4d6d9f7f55799f3c871d192fccb7.png)</span>，以及相关的数据，效率很低。

### k-means II

k-menasII算法在每次循环中选取多个（<span class='inline-formula'>![l](http://heming-keh.github.io/assets/formula/2014/07/25/kmeans/2db95e8e1a9267b7a1188556b2013b33.png)</span>个）points作为准centroid（准centroid是将来有可能会成为centroid的点），循环*n*次之后，会选取
足够多的准centroids。准centroid的数量要远大于*k*，而且在每次循环中选取的准centroid数量一般也会非常多，
例如每次选1000个，这样循环的次数要比*k*小很多，计算效率就会高很多。最后对*C*中的准centroid再进行聚类（可以使用k-means++算法），将聚类结果中的k个centroid作为原数据的k个
centroid。这样不仅选centroid时的计算效率提高了，而且选出的k个centroid的位置也会比较好，因为是再聚类生成的centroid。

![k-means II](http://heming-keh.github.io/assets/images/2014/07/25/kmeans/k-meansII.png)

k-means II算法相比k-means++算法更适合并行计算，因为它没有要求去严格的选取k个点作为centroids，
只是预选，这样把1-5步放到不同的机器去计算，最后把选取的所有点在reducer中聚到一起，
再聚类，结果和非并行计算也是差不多的。


## 选择k值

### Elbow method

当选择的k值小于真正的<span class='inline-formula'>![k_0](http://heming-keh.github.io/assets/formula/2014/07/25/kmeans/3a3a31c01221cd0fa25152cb1c38f56c.png)</span>时，k每增加1，cost值就会大幅的减小；当选择的k值大于真正的<span class='inline-formula'>![k_0](http://heming-keh.github.io/assets/formula/2014/07/25/kmeans/3a3a31c01221cd0fa25152cb1c38f56c.png)</span>时，
k每增加1，cost值的变化就不会那么明显。这样，正确的k值就会在这个转折点，类似elbow的地方。
如下图（图取自Ng的[Machine Learning](https://www.coursera.org/course/ml)课程）。

![elbow](http://heming-keh.github.io/assets/images/2014/07/25/kmeans/elbow.png)


### Prediction Strength

Elbow method虽然可行，但你会发现很难用Elbow method去单独衡量选取的某个k值的优劣，
它需要求出多个k值对应的cost，并把图画出来找转折点。
Prediction Strength就可以做到这一点，下面来说明一下这个算法。

将要聚类的数据分成training set和test set两部分。使用选取的k值分别对这
两部分进行聚类，聚类完后，每个数据就都被分类了，也就相当于每个*x*有了对应的*y*值，
这样training set和test set就成了真正监督学习可用的training set和test set。
然后使用supervisied classification的方法（Logistic Regression或Artificial Neural Networks etc.），
利用training set中的数据训练出一个classifier，这个classifier和聚类方法一样，都可以
为数据分类。使用这个classifier对test set中的数据进行predicted，
将predicted的结果和聚类的结果做比较，根据匹配度来衡量k值选择的优劣。
衡量优劣在[Cluster Validation by Prediction Strength](https://www.stat.washington.edu/wxs/Stat592-w2011/Literature/tibshirani-walther-prediction-strength-2005.pdf)
这篇论文中是有一个公式的，但这个公式写得有点让人费解（感觉不合理），其实用你自己的方法衡量优劣也没什么不可以，
一般情况下也不需要自己实现这些东西，算法理解了就Ok。

![ps](http://heming-keh.github.io/assets/images/2014/07/25/kmeans/ps.png)

这种方法为什么可行，看上图。第一行，k值选对了，这样training set和test set的聚类结果就会非常相似，
使用training set训练出的classifier，就能很好的predicted测试集合中的数据。
第二行，k值选错了，这样聚类的结果差别就会很大，使用training set训练出来的数据就不能很好的predicted测试集合中的数据。

## CCP Clustering

```bash
ml ksketch --format avro --input-paths part2normalized
  --output-file part2sketch.avro
  --points-per-iteration 1000 --iterations 10 --seed 1729 
```

命令执行K-means II（别称Scaleable k-means++）算法的1-5步，循环n=10次，每次选出1000个点，
这样就创建出了一个sketch。使用sketch来做试验性的计算，例如决定features、选k值，比直接用原数据计算
要高效很多（计算时间方面），当数据量非常大的时候，使用原始数据做这些试验性的计算，那几乎就是扯蛋。

```bash
ml kmeans --input-file part2sketch.avro
  --centers-file part2centers.avro
  --clusters 40,60,80,100,120,140,160,180,200
  --best-of 3 --seed 1729 --num-threads 1
  --eval-details-file part2evaldetails.csv
  --eval-stats-file part2evalstats.csv  
```

命令使用Prediction Strength算法去衡量每个k值（40,60,...）的表现，同时会把kassign需要的centroids给计算出来，存放到part2centers.avro中。
Prediction Strength算法里面也是需要小规模的聚类计算的（对training set和test set进行聚类），这次聚类计算使用的是
[Lloyd's algorithm](http://en.wikipedia.org/wiki/Lloyd's_algorithm)，也就是我们通常讲的k-means算法。
根据k值的表现去选择features时，需要反复的运行这个命令，设置seed值，可以对比多次运行kmeans命令的结果，
随机选择所有的centroids时，用的Random对象估计都是用这个seed生成的。

```bash
ml kassign --input-paths part2normalized --format avro
  --centers-file part2centers.avro
  --center-ids 22 --output-path part2assigned --output-type csv 
```

centroids、k值确定了，这一步再执行传统k-means算法来聚类，得出最终结果。

