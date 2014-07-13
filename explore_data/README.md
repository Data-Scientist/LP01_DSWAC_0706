Exploring the Data Set
=====================
step1. Profile the data
----------------------
任何数据分析之前，都要对数据有一个大概的了解。

*基本思想就是*：
1.分析数据大小，几乎是例行要做的
2.分析各个特征的含义和分布

这里面需要用到很多linux的命令行，用熟悉了之后感觉还是挺方便的。先大体介绍几个简单函数： 

```
man cmd:  显示对cmd的所有解释。不懂的就man一下 :)
head:  当数据量很大的时候，先看前面几行文件会很快捷
tail:  和head对应，显示最后几行内容。head和tail都可以通过-n参数指定显示几行
du -sh dir/file:  以人类可读的形式显示目录或file的总大小
wc -l file:  显示文件file的行数，file可以是一个文件，也可以带通配符，如*.cpp
ls -Rlh:  ls应该是linux里面最常用的功能了，就是显示文件列表。-Rlh其实是-R -l -h的简写。linux的很多命令都可以这样。具体什么意思man ls一下	
find:  linux里面比较强大的命令了，可以根据文件类型、正则表达式、时间戳、权限等多种条件综合搜索。比较常见的几种用法汇总如下：
find . -type f:  找出所有文件，不包括目录、软链等其他类型
find . -name "*.cpp":  找出所有以cpp为后缀的文件（可能是目录），会遍历目录下的所有子目录
find . -name "excercise*" -maxdepth 1:  仅在当前目录，找出所有以excercise为前缀的文件
xargs:  linux内部最强大的中介，和其他命令组合可以实现很多及其复杂的功能。xargs会把管道中上一个命令的输出作为其他命令的参数，示例如下：
find . -type f -name "*.cpp"|xargs grep "interesting": 在所有cpp后缀的文件中，grep含interesting的文件 
echo -e "kid\nrank"|xargs -i -n1 grep "{}" */*.sh:  等效于在当前所有子目录的以.sh为后缀的文件中，分别执行grep kid和grep rank两个命令。所以xargs可以有效减少loop的使用。当然效率可能会低一些。
grep:  正则匹配查找。上面也讲了一些，但是只是九牛一毛。最好的方法还是man一下  
```
有了以上的工具之后，你就可以尽情的去欣赏数据了。
在本例中，你可以这样：
```
$ cd ~/data 
$ du -sh . # 查看目录大小
201M .
$ ls -Rlh  # 递归显示目录及子目录大小
./heckle:
total 103M
-rw-r--r-- 1 cloudera cloudera  18M Oct 18  2013 web.log
-rw-r--r-- 1 cloudera cloudera  18M Oct 18  2013 web.log.1
-rw-r--r-- 1 cloudera cloudera 253K Oct 18  2013 web.log.2
-rw-r--r-- 1 cloudera cloudera  14M Oct 18  2013 web.log.3
-rw-r--r-- 1 cloudera cloudera  12M Oct 18  2013 web.log.4
-rw-r--r-- 1 cloudera cloudera 2.1M Oct 18  2013 web.log.5
-rw-r--r-- 1 cloudera cloudera 656K Oct 18  2013 web.log.6
-rw-r--r-- 1 cloudera cloudera  11M Oct 18  2013 web.log.7
-rw-r--r-- 1 cloudera cloudera  15M Oct 18  2013 web.log.8
-rw-r--r-- 1 cloudera cloudera  14M Oct 18  2013 web.log.9

./jeckle:
total 99M
-rw-r--r-- 1 cloudera cloudera  18M Oct 18  2013 web.log
-rw-r--r-- 1 cloudera cloudera  16M Oct 18  2013 web.log.1
-rw-r--r-- 1 cloudera cloudera 3.5K Oct 18  2013 web.log.2
-rw-r--r-- 1 cloudera cloudera  15M Oct 18  2013 web.log.3
-rw-r--r-- 1 cloudera cloudera 9.2M Oct 18  2013 web.log.4
-rw-r--r-- 1 cloudera cloudera 2.1M Oct 18  2013 web.log.5
-rw-r--r-- 1 cloudera cloudera 505K Oct 18  2013 web.log.6
-rw-r--r-- 1 cloudera cloudera  11M Oct 18  2013 web.log.7
-rw-r--r-- 1 cloudera cloudera  15M Oct 18  2013 web.log.8
-rw-r--r-- 1 cloudera cloudera  14M Oct 18  2013 web.log.9

$ cd heckle
$ wc -l * ＃ 文件行数
    50905 web.log
    48230 web.log.1
      726 web.log.2
    39978 web.log.3
    32779 web.log.4
     6673 web.log.5
     2015 web.log.6
    36169 web.log.7
    49729 web.log.8
    46273 web.log.9
   313477 total
$ head -5 heckle/web.log ＃ 大体看一下
{"auth": "15a63c4:e66189ba", "createdAt": "2013-05-12T00:00:01-08:00", "payload": {"itemId"": "15607", "marker": 240}, "refId": "47c7e2f6", "sessionID": "82ada851-0b3c-4e9d-b8cf- 0f0a2ebed278", "type": "Play", "user": 22700996, "userAgent": "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; .NET CLR 1.1.4322; .NET CLR 2.0.50727; InfoPath.1)"}
{"auth": "1547142:7d3d41c7", "createdAt": "2013-05-12T00:00:03-08:00", "payload": {"itemId"": "6210", "marker": 3420}, "refId": "141ac867", "sessionID": "d95bc727-033f-4f62-831a- 2f8d6740a364", "type": "Play", "user": 22311234, "userAgent": "Mozilla/5.0 (Windows NT 6.0) AppleWebKit/535.7 (KHTML, like Gecko) Chrome/16.0.912.75 Safari/535.7"}
{"auth": "30af4f8:2527ff80", "createdAt": "2013-05-12T00:00:09-08:00", "payload": {"itemId"": "32009", "marker": 2760}, "refId": "fdec4481", "sessionID": "673ee60a-0aa2-4eac-a6fb- 8a68d053dbf3", "type": "Play", "user": 51049720, "userAgent": "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.142 Safari/535.19"}
{"auth": "6f691c:455e17cb", "createdAt": "2013-05-12T00:00:10-08:00", "payload": {"itemId"": "7347", "marker": 1059}, "refId": "4b5021f4", "sessionID": "2d3aef1d-ec8d-4053-8c40- e8579e547745", "type": "Play", "user": 7301404, "userAgent": "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.1; Trident/4.0; WDL6.1.1; .NET CLR 1.1.4322; .NET CLR 2.0.50727)"}
{"auth": "1208d4c:279737f7", "createdAt": "2013-05-12T00:00:11-08:00", "payload": {"itemId"": "3702e4", "marker": 780}, "refId": "7586e549", "sessionID": "d4a244cb-d502-4c94-a80d- 3d26ca54a449", "type": "Play", "user": 18910540, "userAgent": "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.1; Trident/4.0; GTB7.2; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0; InfoPath.2)"}

```


step2. Load data into hdfs
--------------------------
调用如下命令把data目录整个copy到虚拟机的hadoop集群：

hadoop fs -copyFromLocal data

命令原型如下：data就是此时的localsrc，des不指定就是hadoop的home目录

hadoop fs [generic options] -copyFromLocal <localsrc> ... <dst>

上传完之后使用`hadoop fs -ls`命令看一下data目录是否存在

`hadoop fs -help`可以看到所有和文件系统（fs）相关的命令函数帮助信息


