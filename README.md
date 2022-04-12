# LFUCache
---
高性能二级缓存实现，内存LRU缓存+磁盘文件持久化缓存。
* 支持过期（Expiration）清除;
* 支持LFU ~ 如果超过内存缓存容量，最近不常使用的项将被剔除（Eviction）;
* 支持剔除（Eviction）到二级可持久化缓存;
* 支持回写（WriteBehind）到后端持久化存储，例如DB。
* BigCache的Key常驻内存，Value可持久化
* BigCache支持存磁盘文件，内存映射+磁盘文件，和堆外内存+磁盘文件3种模式。
---
补：核心内容参考自：archbobo老师的okcache。
