LP01_DSWAC_0706
===============

Lords first big data demo project, learning from cloudera's ccp-2013: Data Scientist Web Analytics Challenge: Classification, Clustering, and Collaborative Filtering.

1. Project introduction
---------------------------
Lords Movie 是一个提供电影服务的网站，他们的团队一直致力于为用户提供最好的服务。为此他们建立了一个日志系统记录用户的行为，包括用户id、浏览行为、电影评分、session等等。

现在为了进一步改善他们的服务，他们雇佣你来为他们解决一些关键问题，他们希望你能创建一幅用户图谱，设计一个强大的推荐系统从而准确的描述用户喜好，为此你必须设计一款“数据产品（data product）”，解决数据科学里面经典的3个问题：

1. 分类器。Lords Movie的法律团队希望你能根据现有的日志准确的区分出成人和未成年人

2. 聚类。产品团队希望根据session，将用户的行为进行聚类，从而提高站点的易用性。

3. 推荐系统。产品团队希望在网站上部署一个推荐引擎，提高用户黏度

2. Data Description
---------------------------
你的数据是最近4周的json日志文件，除此之外再无其他。其中一行的格式如下
```
{"created_at": "2013-05-08T08:00:00Z", "payload": {"item_id": "11086", "marker": 3540}, "session_id": "b549de69-a0dc-4b8a-8ee1-01f1a1f5a66e", "type": "Play", "user": 81729334, "user_agent": "Mozilla/5.0 (iPad; CPU OS 5_0_1 like Mac OS X) AppleWebKit/534.46 (KHTML, like Gecko) Mobile/9A405"}
```
数据细节如下：

* 日志文件有一部分含家长控制，所以你可以利用这部分信息区分哪些浏览行为是成人，哪些是未成年人。当打开家长控制后，用户能浏览的内容会被严格控制
* 在和播放（play）相关的event里面，`marker`表示播放文件的位置
* 打分有1-5档，5表示最好
* Content ID中含`e`的部分表示电视剧，`e`后面的数字表示集数
* 如果视频播到最后，`stop`事件会被记录；而如果在此之前，用户离开页面，该事件是不会记录的
* 所有时间戳都是Lords Movie服务器的本地时间


3. Work with VM
--------------------------
Virtual Box和VMWare的地址
// todo

在虚拟机里面数据和脚本都准备好了：
数据在 /home/cloudera/data
脚本在 /home/cloudera/dscripts
也可以去[github](https://github.com/clouderacertifiedprofessional/) clone
