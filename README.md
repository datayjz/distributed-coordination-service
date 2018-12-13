该项目是Zookeeper、Consul等常用分布式协调服务的常用使用方式，实现了选主等常用服务，可直接应用于要开发的分布式应用程序中，提高开发效率。

# Zookeeper
## Zookeeper介绍
Zookeeper是Google的Chubby项目的开源实现。
Zookeeper是一个为分布式应用程序提供协调服务的中间件。我们知道在分布式系统中，管理和协调服务是极其复杂的，需要考虑竞争条件、死锁等。而Zookeeper通过简单的架构和API就能为我们解决这些问题，这样我们就可以把大部分精力放在业务逻辑的实现上，而不需要关心如何解决、保证分布式协调服务。

Zookeeper能够提供的常见服务务：
* 命名服务：按名称标识应用集群的节点，类似于DNS。
* 配置管理：为应用系统添加配置信息，一般是应用程序的元数据信息。
* 集群管理：应用程序可以实时的加入/离开集群。
* 选举算法：选举一个节点作为应用程序协调的leader。
* 锁定和同步服务：在修改数据的同时锁定数据。
* 高度可靠的数据注册表：即使一个或几个节点关闭，也可以获取到数据。

## Zookeeper数据模型
Zookeeper采用了类似文件系统的树结构来存储数据，与文件系统不同的是，每个节点都可以存储数据(这些数据可以是状态信息、配置、位置信息等，一般是分布式应用系统的元数据信息)。Zookeeper数据模型中的每个节点称为znode，znode通过路径来唯一定位标识的(比如/app1/p_1)。

### Znode
每个znode维护一个stat结构，它里面不仅存储了数据信息，还提供了节点的元数据信息，包括：版本号、时间戳、数据长度、ACL等。下面是一个znode所存储的具体数据信息：
[zk: 192.168.0.1:2181(CONNECTED) 6] get /test
data2                                    #节点存储的数据信息
cZxid = 0x100000004                      #创建Znode时分配的zxid
ctime = Tue Dec 11 01:23:10 EST 2018     #节点创建时间
mZxid = 0x100000005                      #最后一次修改Znode所分配的zxid
mtime = Tue Dec 11 01:23:47 EST 2018     #节点最后一次修改时间
pZxid = 0x100000004                      #最后一次修改该Znode子节点的时的zxid
cversion = 0                             #该Znode子节点更改的版本号
dataVersion = 1                          #Znode更改的版本号(每次修改版本号都会自加1)
aclVersion = 0                           #ACL修改的版本号
ephemeralOwner = 0x0                     #如果是临时节点，则表示该Znode所有者的Session ID，如果不是则为0
dataLength = 5                           #存储数据长度
numChildren = 0                          #该节点存在的子节点个数

>zxid(Zookeeper Transaction ID)是指ZK事务ID，每次修改ZK状态，都会收到一个新的zxid，zxid暴露了Zookeeper中所有更改操作的顺序。可以理解Zookeeper为每次更新操作都提供了一个顺序ID，执行的时候是按照这个分配的顺序ID来执行。

每个Znode所能存储的数据最大为1M(使用中，实际数据应该远远小于1M)，以为Zookeeper并不是一个数据库。它一般存储的是配置、状态等协调信息。如果数据太大，可能会造成操作耗时或者对一些其它操作造成延迟，因为大数据在网络传输中是非常耗时的。一般如果我们有存储大数据需求，可以将它们存储在第三方存储中，然后在Zookeeper中存储指向这些数据的指针。


> Znode的路径名称不能使用”.”、”..”、“zookeeper“、“null”和Unicode码为\u0001 - \u0019、\u007F - \u009F、\ud800 -uF8FFF, \uFFF0 - uFFFF，但是”.”和”..”可以和其它字符一起使用。


Zookeeper提供了对Znode的原语操作：

* 创建节点
* 获取节点信息
* 修改节点
* 删除节点
* 权限控制
* 事件监听

实际上就是提供了对znode增删改查以及权限控制和监听，但是我们可以通过这些原语组合成不同的使用场景，比如我们上面所列举的常见服务。

Znode分为持久节点、临时节点和顺序节点。
* 临时节点：客户端在活跃时，该类节点有效，当客户端与ZK断开连接后，临时节点会被删除。临时节点不允许有子节点。临时节点leader选举过程中起到中起到重要作用。
* 持久节点：持久节点是相对临时节点来说的，即便客户端断开连接，该类节点也不会被删除。如果不指定，默认创建的都是持久节点。
* 顺序节点：顺序节点可以是临时节点也可以是持久节点。当创建一个顺序节点时，Zookeeper会将10为的顺序号附加到节点路径上，比如创建/myapp顺序节点，则ZK会将路径改为/myapp0000000001，并将下一个顺序号设置为/myapp0000000002。顺序节点在锁定和同步中起到重要作用。

### 会话(Session)

当客户端连接Zookeeper集群，就会与ZK集群建立一个会话连接，ZK会为该会话连接分配一个Session ID。Session有三种状态，CONNECTING、CONNECTED和CLOSE，只有客户端与ZK集群会话连接正常的情况下才能操作Zookeeper。
客户端是通过发送心跳的形式与Zookeeper保持会话有效的，如果Zookeeper集群在规定的心跳间隔时间内没有收到客户端发送的心跳，就会认为客户端断开连接。这时候Zookeeper会删除会话期间所创建的临时节点。

>在Zookeeper 3.2.0版本以添加了chroot特性，当为客户端提供的连接为:host:port/xxx时，则该客户端所能访问的命名空间都是/xxx下面的子空间。比如get /app1/p_1，则实际获取的是/xxx/app1/p_1。

### 监听(Watches)

Zookeeper提供为Znode添加监听的机制，当Znode节点发生改变时，会为客户端发送通知。我们可以在获取特定Znode时设置Watches，当Znode数据更改或Znode子节点发生更改时，Watcher会向客户端发送一个更改通知。注意这个Watcher只能触发一次，
我们可以通过exit()、getData()和getChildren()三种方式设置监听，它们分别对应的不同的监听事件：
* Create Event 将会触发exit()设置的监听。
* Delete Event 将会触发exit()、getData()和getChildren()设置的监听。
* Change Event 将会触发getData()监听事件。
* Children Event 将会触发getChildren()监听事件。


### 访问控制列表(ACL)
Zookeeper提供了针对Znode的ACL，ACL类似UNIX文件系统权限，提供了几种操作方式，但是Zookeeper并没有owner、group的概念。需要注意的是ACL指针对特定的Znode，权限并不能下放到该Znode的子节点，也就是ACL并不是递归的(Zookeeper采用的是精细化权限管理)。
Zookeeper使用”Schema:Ids,perms”的形式设置权限，比如”ip:19.22.0.0/16,READ”表示为任何以19.22开头的ip提供读权限。
Zookeeper提供了下面四种Schema形式：
* world：默认权限，表示任何人都可以访问。
* auth：不使用任何id，表示经过人份验证的用户。
* digest：使用用户名:密码的方式认证，业务系统中最常用的。使用username:password生成MD5哈希值，然后将其作为ACL ID标识。通过以明文的形式发送来完成认证，比如"yjz:123”。
* ip：使用IP地址认证。

Zookeeper提供五种权限类型：
* CREATE：能够创建子节点。
* READ：能够获取节点数据，也可以列出子节点列表(不能查看内容)。
* WRITE：能够修改数据。
* DELETE：能够删除子节点。
* ADMIN：能够设置权限。

Zookeeper提供的权限访问控制是可插拔式的，所以我们也可以根据我们自己的业务场景进行权限管理控制。具体可以查看http://zookeeper.apache.org/doc/current/zookeeperProgrammers.html 中的Pluggable ZooKeeper authentication文档说明来设置。


## Zookeeper API使用
Zookeeper提供了Java和C的官方API，但是Zookeeper社区为大多数编程语言提供了非官方API。使用Zookeeper API可以与Zookeeper集群建立建立、交互、操作数据、协调，以及断开与Zookeeper集合(Zookeeper集群也称为Zookeeper集合)的连接。
客户端使用Zookeeper交互流程：
* 连接到Zookeeper集合，Zookeeper集合为客户端分配会话ID。
* 定期向Zookeeper服务器发送心跳，否则，Zookeeper集合会将会话ID过期，客户端需要重新连接。
* 只要会话处于活动状态，就可以获取/设置Znode。
* 完成任务后，断开与Zookeeper集合的连接。

Zookeeper客端API的主类是Zookeeper类，如果想要与Zookeeper集合交互，就需要创建一个Zookeeper类的实例，创建实例的过程就是与Zookeeper集合建立连接的过程。Zookeeper类中的方法基本都是线程安全的。
Zookeeper API提供了同步(synchronous)和异步(asynchronous)方法：同步方法阻塞直到server端结果返回；异步方法将请求提交到任务队里后就立即返回，异步方法通过callback对象能够知道操作结果。


### Jar包引入
这里采用最目前最新版3.4.13版本。
```
<dependency>
    <groupId>org.apache.zookeeper</groupId>
    <artifactId>zookeeper</artifactId>
    <version>3.4.13</version>
</dependency>
```


### 创建Zookeeper连接
创建Zookeeper连接需要通过Zookeeper类的构造方法```ZooKeeper(String connectString, int sessionTimeout, Watcher watcher)```来创键，它需要三个参数：
* connectString：Zookeeper连接地址，比如“192.168.0.1：2181”。
* sessionTimeout：会话超时时间，单位毫秒。
* watcher：实现Watcher接口的类对象，用于监听与Zookeeper集合连接的状态信息。当连接状态信息发生改变时，该监听器会被触发。

Zookeeper zooKeeper = new ZooKeeper("10.5.234.238:2182", 1000, clientWatcher);
该构造方法连接过程是异步的，也就是说他不会等到真正建立连接后才会返回。
