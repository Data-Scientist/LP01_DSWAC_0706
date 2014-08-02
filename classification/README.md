# Classifying Users

## 算法原理

算法原理简单通俗的来说，就是根据用户的相似性来进行分类，例如已知用户a为kid，如果用户b和用户a非常相似，
那么就认识用户b也是kid。两个用户的相似性是由他们共同看过的电影来决定的，共同看过的电影越多越相似。
这只是通俗的原理描述，具体的算法原理理解起来可能要稍困难一些，
当然[CCP Classifying](http://certification.cloudera.com/prep/dsc1sk/classifying.html)的代码本身是
比较简单的，就是使用下面的公式迭代循环计算，直到收敛。

<div class='formula'><img src='/assets/formula/2014/07/11/classifying_users/041c98133c9accd63c865657ab3278e3.png' alt=' v_{k+1} = \beta M v_k + (1 - \beta) e / n '></img></div>

### SimRank

计算相似性的思路是[SimRank](http://en.wikipedia.org/wiki/SimRank)，SimRank的核心思想是
*two objects are considered to be similar if they are referenced by similar objects.* 这种思想体现在下面的公式中。

<div class='formula'><img src='/assets/formula/2014/07/11/classifying_users/ce0a931de702324e9bdc280af4e9bbf0.png' alt='s(a,b)=\frac {c} {\abs{I(a)}\abs{I(b)}} \sum_{i=1}^{\abs{I(a)}} \sum_{j=1}^{\abs{I(b)}} s(I_{i}(a), I_{j}(b))'></img></div>

用户a和用户b的相似性s(a,b)由它们的in-neighbors的相似性来决定，而它们的in-neighbors的相似性又由in-neighbors的in-neighbors来决定，这是一个递归的过程。

### PageRank

SimRank是一般化的方法，更具体的来说，计算两个用户之间的相似性使用的是，略施trick的[PageRank](http://en.wikipedia.org/wiki/PageRank)。
PageRank是Google公司用来计算网页权重(Pr值)的，这貌似和计算结点之间的相似性没什么关系，别着急，先来了解下PageRank算法。

![graph 1](https://raw.githubusercontent.com/Data-Scientist/LP01_DSWAC_0706/master/classification/images/graph.png)

假设4个网页之间的链接关系如上图，图中的结点表示网页，边表示链接，边的权值表示的是从一个页面转到另外一个页面的概率。
结点A有三条出边分别指向B、C、D，说明A页面中有B、C、D三条外链，
因为是三条外链，所以如果我们浏览的是页面A，那么从A随机转向B、C、D的概率都为1/3，所以三条边的权值都为1/3。
结点B有两条出边分别指向C、D，说明页面B中有C、D两条外链，当然边的权值也都是1/2。结点C有只一条出边指向A，说明
页面C中只有一条外链指向A，如果当前浏览的是页面C，那么只能转向A，这样边的权值为1。同样结点D有两条出边分别指向
A、C，说明页面D中有A、C两条外链，边的权值都为1/2。

<div class='formula'><img src='/assets/formula/2014/07/11/classifying_users/a4c5868841ca107f4a206739143ea5ef.png' alt=' M = \begin{bmatrix}   0 & 0 & 1 & \frac{1}{2} \\   \frac {1}{3} & 0 & 0 & 0 \\   \frac {1}{3} & \frac {1}{2} & 0 & \frac {1}{2} \\    \frac {1}{3} & \frac {1}{2} & 0 & 0 \end{bmatrix} '></img></div> 

将图中的链接信息转换成矩阵形式（上图），这个矩阵称为transition matrix *M*，矩阵的每一列表示每个结点的outcoming links，每一行表示每个结点的incoming links，
为了便于理解，这里使用a、b、c、d作为矩阵的下标，例如，<span class='inline-formula'>![M_{(b,a)}](/assets/formula/2014/07/11/classifying_users/9c3947168f3cd5e0fa6b5b92fa4ff9f1.png)</span>是第二行第一列的元素，也就是结点A到B的outcoming links，或者说是
结点B来自A的incoming links。A对应的行<span class='inline-formula'>![M_{(a,)}](/assets/formula/2014/07/11/classifying_users/4f4e3ddba02045a7f2b0f3bf921f7a83.png)</span>是结点A的所有incoming links，A对应的列<span class='inline-formula'>![M_{(,a)}](/assets/formula/2014/07/11/classifying_users/405ce28308cb7961f8630bb5f59f4a78.png)</span>是结点A的所有outcomoing links。

当你打开浏览器，你会先选择输入A、B、C、D哪个页面的网址呢？算法假设你会以同样的概率，随机打开它们中的任意一个。
共4个页面，所以刚开始的时候打开每个页面的概率都为1/4，这样就得到一个初始的distribution vector <span class='inline-formula'>![v_0](/assets/formula/2014/07/11/classifying_users/8bcda5f030288c05bb245be5d42b3c07.png)</span>。

<div class='formula'><img src='/assets/formula/2014/07/11/classifying_users/0be3a6b3800afef405d394b869687e74.png' alt=' v_0 = \begin{bmatrix}\frac {1}{4} \\ \frac {1}{4} \\ \frac {1}{4} \\ \frac {1}{4} \end{bmatrix} '></img></div>

网站页面的重要性是由页面的incoming links来决定的，而矩阵M的每一行描述是每个结点的incoming links，
<span class='inline-formula'>![v_0](/assets/formula/2014/07/11/classifying_users/8bcda5f030288c05bb245be5d42b3c07.png)</span>描述是当前状态访问每个页面的概率，试想一下<span class='inline-formula'>![v_1 = M v_0](/assets/formula/2014/07/11/classifying_users/59470fdec511b0161f2d6b34cfee417c.png)</span>是什么？显然是下一个状态，我们浏览每个页面的概率。
矩阵*M*与向量<span class='inline-formula'>![v_{0}](/assets/formula/2014/07/11/classifying_users/f32423d2b9868cef26c41e39c2d1edc3.png)</span>相乘，可以看成是矩阵的每一行与向量做点积(dot product)运算，然后再将点积的结果作为新向量<span class='inline-formula'>![v_1](/assets/formula/2014/07/11/classifying_users/84fc825e5c5d6969221754059de4a804.png)</span>中的值，
第一行的点积<span class='inline-formula'>![M_{(a,)} \cdot v_0](/assets/formula/2014/07/11/classifying_users/945338438f59bd2770028ff0df720fbf.png)</span>作为<span class='inline-formula'>![v_1](/assets/formula/2014/07/11/classifying_users/84fc825e5c5d6969221754059de4a804.png)</span>的第一个值，第二行的点积<span class='inline-formula'>![M_{(b,)} \cdot v_0](/assets/formula/2014/07/11/classifying_users/a3b14ae22055a2f36455785285760a7e.png)</span>作为<span class='inline-formula'>![v_1](/assets/formula/2014/07/11/classifying_users/84fc825e5c5d6969221754059de4a804.png)</span>的第二值，．．．。更详细的来说，例如，矩阵的第一行<span class='inline-formula'>![M_{(a,)}](/assets/formula/2014/07/11/classifying_users/4f4e3ddba02045a7f2b0f3bf921f7a83.png)</span>和<span class='inline-formula'>![v_0](/assets/formula/2014/07/11/classifying_users/8bcda5f030288c05bb245be5d42b3c07.png)</span>做点积运算，
<span class='inline-formula'>![M_{(a,a)}=0](/assets/formula/2014/07/11/classifying_users/72b3c3d91642074ca161033ca612e598.png)</span>，说明没有从A到A的外链，当我们浏览页面A时，就没有可能再转到A，开始时浏览页面A的概率为1/4，这样就有0 * 1/4结果为0
；<span class='inline-formula'>![M_{(a,b)} = 0](/assets/formula/2014/07/11/classifying_users/457d7857bd6b11fa06420ff8cfe88efd.png)</span>，说明没有从B指向A的外链（指向A的外链就是A的incoming link），开始时浏览页面B的概率同样为1/4，这样就有0 * 1/4结果为0；
<span class='inline-formula'>![M_{(a,c)}=1](/assets/formula/2014/07/11/classifying_users/2b69d838dffe7e0209ba5cc249f4fd5e.png)</span>，说明C有一条指向A的外链，且C只有这一条外链，如果当前浏览的是C，那么只能转向A，这样1 * 1/4结果为1/4；
<span class='inline-formula'>![M_{(a,d)}=1/2](/assets/formula/2014/07/11/classifying_users/2a7d50b7de901f6270d0bccad4914461.png)</span>，说明D有一条指向A的外链，开始时浏览页面D的概率为1/4，1/2 * 1/4结果为1/8。做点积时要将这些结果都加起来，
0 + 0 + 1/4 + 1/8 = 3/8，再将3/8赋值给<span class='inline-formula'>![v_1](/assets/formula/2014/07/11/classifying_users/84fc825e5c5d6969221754059de4a804.png)</span>的第一个元素，这个值也就是下一个状态浏览结点A的概率：3/8。
其它行<span class='inline-formula'>![M_{(b,)}](/assets/formula/2014/07/11/classifying_users/ce8bb394ca838027a4f02b1b8c3bcfb4.png)</span>、<span class='inline-formula'>![M_{(c,)}](/assets/formula/2014/07/11/classifying_users/d17b359e85bd124d3ba31f2f8c3a77e7.png)</span>、<span class='inline-formula'>![M_{(d,)}](/assets/formula/2014/07/11/classifying_users/b2d98f5e36b81b09fc952721bc76bf5b.png)</span>的点积也是同样的道理。

我们已经知道知道<span class='inline-formula'>![v_1 = M v_0](/assets/formula/2014/07/11/classifying_users/59470fdec511b0161f2d6b34cfee417c.png)</span>
是下一个状态的distribution vector，也就是下一个状态我们可能访问各个页面的概率。从上面的计算描述中，可以知道如果
结点的incoming links越多，且incoming links对应的权值越大，就越可能被浏览到，页面的Pr值也就越高。上面这样计算一次，
也就相当于使用*M*影响一次初始的<span class='inline-formula'>![v_0](/assets/formula/2014/07/11/classifying_users/8bcda5f030288c05bb245be5d42b3c07.png)</span>，因为页面最终的权重(Pr值)是要由*M*来决定的，所以*M*影响的次数越多，最终的结果就越准确。
于是就有了下面的迭代计算公式。这个迭代计算的过程类似[Examples of Markov Chain)](http://en.wikipedia.org/wiki/Markov_chain#Example)，
每相乘一次，状态改变一次。

<div class='formula'><img src='/assets/formula/2014/07/11/classifying_users/f2646e475d4ba66821bb9e3c723d4797.png' alt=' v_{k+1} = M v_k '></img></div>

使用上面的公式迭代计算几十次之后，得到下面的结果，从结果中可以看出A的权重最高。这个结果是非常合理的，
虽然C的incoming links比A多，但如果当前浏览的是C那么只能转向A，也就是说A享有C享有的被访问概率，而且自己还有除C以外的incoming links。

<div class='formula'><img src='/assets/formula/2014/07/11/classifying_users/35c1133ad399721f0846ae826e2edf6e.png' alt=' v = \begin{bmatrix} 0.3870968 \\ 0.1290323 \\ 0.2903226 \\ 0.1935484 \end{bmatrix} '></img></div>

#### 特殊情况处理

上面举的例子是比较理想化的情况(结点之间是全连通)，实际的应用中会有Dead Ends、Spider Traps。

#### Dead Ends

![graph-dead-end](https://raw.githubusercontent.com/Data-Scientist/LP01_DSWAC_0706/master/classification/images/graph-dead-end.png)

像上图这样，把C的出边给拿掉，这样C就成dead end。这样使用上述公式迭代计算的过程中，向量v中的所有值都会越来越接近0，这样的结果显然是不合理的。
解决的办法是，先迭代拿掉所有的Dead End，然后再计算结果，之后用计算出的结果推导出dead end的权重。
因为CCP项目中，全部链接都是双向的，没有Dead Ends存在，这里具体的方法就不介绍了，有兴趣的朋友可以
查看[Link Analysis 5.1.4](http://infolab.stanford.edu/~ullman/mmds/ch5.pdf)。


#### Spider Trap

![graph-spider-trap](https://raw.githubusercontent.com/Data-Scientist/LP01_DSWAC_0706/master/classification/images/graph-spider-trap.png)

结点C没有任何outcoming links，但有一条指向自身的边，这就是一种spider trap。
对于这种情况，使用公式迭代计算的时，向量<span class='inline-formula'>![v_{k}](/assets/formula/2014/07/11/classifying_users/9be2b312aeb2085a6811d75d276a406c.png)</span>中C结点对应的权重值会
越来越大，最后几乎为1，其它结点对应的权重会越来越小，最后几乎为0。
这是因为这个trap只有入口没有出口，进入trap的概率值会绕着trap一直转，出不去。
而同时每次迭代又会有新的概率值进入这个trap，这样概率值最终会被trap中的C结点占尽，其它结点分不到任何的概率值。

```r
v0 = c(1/4, 1/4, 1/4, 1/4)
M = matrix(c(
  0,   1/3, 1/3, 1/3,
  0,   0,   1/2, 1/2,
  0,   0,   1,   0, 
  1/2, 0,   1/2, 0), nrow=4, ncol=4)
v = v0

cat(v, '\n')
for (i in 1:100) {
	v = M %*% v
	cat(v, '\n')
}
```

有兴趣的朋友可以使用R跑一下上面的代码，你会发现第3个值，也就是结点C对应的值会越来大，最后几乎就是1，其它结点对应的值
几乎变成了0。这也个结果也就是说你只有可能访问C页面，其它结点没有可能被访问，这个结果显然不合理。
解决的办法是，在状态改变时，让浏览者有一定的概率发生瞬移(telport)，这样就避免了一直在spider trap中绕圈。
最终得到如下公式。

<div class='formula'><img src='/assets/formula/2014/07/11/classifying_users/9c5d3c2fecd1490d025bd2c5476b52ca.png' alt=' v_{k+1} = \beta M v_{k} + (1 - \beta) e / n '></img></div>

<span class='inline-formula'>![\beta](/assets/formula/2014/07/11/classifying_users/b0603860fcffe94e5b8eec59ed813421.png)</span>是一个常量通常为0.8，<span class='inline-formula'>![1 - \beta](/assets/formula/2014/07/11/classifying_users/cbc32e6db60f2d35c9048c3dc9757292.png)</span>就是发生瞬移(telport)的机率，*e*是和*v*同大小的所有值都为1的向量，*n*是结点的数量。

### 分类

PageRank算法已经清楚了，现在说下具体怎样才能使用PageRank进行分类。[CCP Classifying](http://certification.cloudera.com/prep/dsc1sk/classifying.html)
中要对用户进行分类，分类是根据用户看过的电影来分的，这样整个数据构成的网络就与互联网不同了，
这个网络中有两种结点：用户结点、电影结点（而互联网中只有一种结点），相似的用户结点通过电影结点连接起来。
 两个用户越相似它们之间的链接就越多（两个相似用户之间并没有直接的链接，
是间接的通过电影结点连在一起的，这条链中可能还存在其它的用户结点电影结点，或许会是一条很长的链）。

从[Exploring the Data Set](http://certification.cloudera.com/prep/dsc1sk/exploring.html)我们已经知道，可以根据用户session发生的家长控制事件先标记分类出一些用户，
然后把这些用户作为训练样本，找出和训练样本相似的用户结点，那么就可以认为这些相似的结点和训练样本中的结点是同一类型，这也就是分类。
PageRank算法开始时是给所有结点同样均等的概率值，然后根据网页链接的结构往外扩散这些概率值，最终获取概率值大的结点，权值就高。
试想一下，如果我们只给已经分类的同类型用户结点（训练样本）一定的概率值，然后再根据用户和电影之间的链接关系往外扩散，那么
最终概率值越大的用户结点，是不是就越和训练本中的结点相似。

首先我们先给训练样本中的所有kid结点一定的概率，迭代计算，找出所有和这些kid结点相似的用户结点，认为这些相似用户结点也是kid。
再给训练样本中的所有adult结点一定的概率，迭代计算，找出所有和这些adult结点相似的用户结点，认为这些的用户结点也是adult。
举个例子吧，为了简单，图中被标记出的用户结点只有u2k，以'k'为后缀，表示u2为kid。
u1看过电影m1和m2，同样u2k也看过电影m1和m2，u3看过电影m2和m3。

![graph ccp](https://raw.githubusercontent.com/Data-Scientist/LP01_DSWAC_0706/master/classification/images/graph-ccp.png)

根据图我们得到如下的transition matrix M，M的下标分别为u1、u2k、u3、m1、m2、m3。

<div class='formula'><img src='/assets/formula/2014/07/11/classifying_users/cacafa24212e5d321b4f5b2c0537fb00.png' alt=' M = \begin{bmatrix} 0 & 0 & 0 & \frac{1}{2} & \frac{1}{3} & 0 \\ 0 & 0 & 0 & \frac{1}{2} & \frac{1}{3} & 0 \\ 0 & 0 & 0 & 0 & \frac{1}{3} & 1 \\ \frac{1}{2} & \frac{1}{2} & 0 & 0 & 0 & 0 \\ \frac{1}{2} & \frac{1}{2} & \frac{1}{2} & 0 & 0 & 0 \\ 0 & 0 & \frac{1}{2} & 0 & 0 & 0 \end{bmatrix} '></img></div>

因为只给训练样本中的同类型结点初始概率，也就是只能从u2k开始扩散，所以distribution vector <span class='inline-formula'>![v_0](/assets/formula/2014/07/11/classifying_users/8bcda5f030288c05bb245be5d42b3c07.png)</span>如下。

<div class='formula'><img src='/assets/formula/2014/07/11/classifying_users/0360c6fdd10e37f5b77799e215b497ba.png' alt=' v_0 = \begin{bmatrix} 0 \\ 1 \\ 0 \\ 0 \\ 0 \\ 0 \end{bmatrix} '></img></div>

```r
M = matrix(c(0,   0,   0, 1/2, 1/2,   0, 
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

<div class='formula'><img src='/assets/formula/2014/07/11/classifying_users/431cb0a97fd13949d7cfdc807c5bd9b2.png' alt=' v_{50} = \begin{bmatrix} 0.02237178 \\ 0.05570512 \\ 0.01452865 \\ 0.03122695 \\ 0.03703651 \\ 0.005809555 \end{bmatrix} '></img></div>

distribution vector后3个数可以忽略，因为它们是电影对应的值，前3个数分别对应u1、u2k、u3的相似值，u2k对应的值最大，
这也可以理解，自己和自己当然是最相似的，其次是u1，这说明样本u2k和u1最相似，这从图中也可能直接看出，u1和u2k都
看过电影m1、m2，所以这里我们就可以认为u1也是kid。具体实现算法的时候会设置一个阀值(threshold)，
使用kid和adult的训练样本各迭代计算一次，然后将两个计算出的结果normalize之后合并，然后根据threshold进行分类。


## 具体实现

算法已经清楚了，具体的实现就简单了。

### Extract content items played

提取用户播放、评分、评论过的内容项。kid数据的第一列是用户ID，后跟字母*a*表示用户为*adult*，跟字母*k*表示用记为*kid*；第二列是对应用户播放、评分、评论过的内容项列表，以逗号相隔。


### Prepare the SimRank algorithm

将数据中的adult用户以4:1的比例，分成训练数据集（training set）和测试数据集（test set）；以同样的方法分隔kid用户。顺便说下，只有这些被标记为**a**或**k**的用户，才可以作为起始结点，才可以瞬移（teleport）。


### Build an adjacency matrix


根据kid数据生成另外一种形式的数据item。item数据的第一列是内容项ID；第二列是播放、评分、评论过该内容项的用户ID列表。综合kid和item，就是一个邻接矩阵（adjacency matrix）。


### Implement the SimRank algorithm

<div class='formula'><img src='/assets/formula/2014/07/11/classifying_users/b41bd5a009fba8ad6c8bdf599d08ae24.png' alt=' v_{k+1} = \beta M v_k + (1 - \beta) e / n '></img></div>

*v* is distribution vector, *M* is transition matrix, <span class='inline-formula'>![\beta](/assets/formula/2014/07/11/classifying_users/b0603860fcffe94e5b8eec59ed813421.png)</span>是常量（这里取值0.8），*e*是和*v*同大小的所有值都为1的向量，*n*是训练样本的数量。
[CCP Classifying](http://certification.cloudera.com/prep/dsc1sk/classifying.html)整个代码就是围绕这个公式来计算的。

*simrank_map.py*和*simrank_reducer.py*实现了上述的公式，使用hadoop streaming调用*simrank_map.py*和*simrank_reducer.py*来进行
算法计算。 *simrank.sh*用来循环执行这一过程，循环一次迭代计算一次，循环的时候使用*simrank_diff.py*来
比较新生成的<span class='inline-formula'>![v_{k+1}](/assets/formula/2014/07/11/classifying_users/77041514f9eb2d370bff2c8e300590cf.png)</span>和之前的<span class='inline-formula'>![v_{k}](/assets/formula/2014/07/11/classifying_users/9be2b312aeb2085a6811d75d276a406c.png)</span>，如果差值足够小，就说明收敛了，结束循环。

```bash
# 根据kids训练样本计算，结果中，值越大就说明越可能是kid
./simrank.sh kids_train  
# 根据adults训练样本计算，结果中，值越大就说明越可能是adult
./simrank.sh adults_train  
```

这样我们得到adult\_final和kid\_final，再将这两个向量合并，因为训练样本数量大的，单个结点的影响力就小，
要使它们的影响力相同，需要normalize，*adult\_v = adult\_v (len(adults\_train) / len(kids\_train))*，
因为adult和kid是对立的两个类型，合并的时候，要将adult向量中的值乘以-1。
再设定threshold进行分类，向量中值大于threshold的为kid，小于threshold的为adult。

分类结果出来之后，使用test set验证，正确率非常高，说明这种算法可行，没有必要再使用logistic regression了。
然后，使用全部的已经被标记的数据（训练集和测试集），作为训练集重新计算，得到最终的结果。

## 参考资料

[chapter 5 in Mining Massive Data Sets](http://i.stanford.edu/~ullman/mmds/ch5.pdf)。

## 其它

这里我只重点描述说明算法，具体实现过程，上面只简要的说了一下。代码的实现本身非常简单，
原理清楚之后，原文[Classifying Users](http://certification.cloudera.com/prep/dsc1sk/classifying.html)
的内容的看起来就会非常顺畅，这里就没有必要再神叨叨了。
另外这个目录下放的代码中会添加一些注释，有兴趣的朋友可以看看。
