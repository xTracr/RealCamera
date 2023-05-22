# Real Camera #

## [English](README.md) ##

使第一人称视角下的摄像头更加真实。  
支持的版本: 1.18-1.19 Forge & Fabric  
从[Releases](https://github.com/xTracr/RealCamera/releases)、[Modrinth](https://modrinth.com/mod/real-camera)或[CurseForge](https://curseforge.com/minecraft/mc-mods/real-camera)下载  

## 特性 ##

* 将摄像头绑定到身体的某个部位
* 自定义摄像头的位置和旋转角度
* 在第一人称视角下渲染玩家模型
* 按下F6来开关，另外一些键来调整摄像头
* 在Config界面配置以上特性（需Cloth Config）

> 详细了解[如何配置](https://github.com/xTracr/RealCamera/wiki/Configuration)

### 支持的游戏版本 ###

* Fabric:
  * `realcamera-1.19.4-fabric`: 1.19.4, 1.19.3
  * `realcamera-1.18.2-fabric`: 1.18.2, 1.19.2[^1]
* Forge:
  * `realcamera-1.19.4-forge`: 1.19.4
  * `realcamera-1.18.2-forge`: 1.18.2

[^1]:该模组的1.18.2-fabric版本已测试并支持1.19.2版本(尽管配置界面无法工作)，但并不保证其在1.19.2的完全兼容性，且不会特意维护其在1.19.2的支持。

### 依赖项目 ###

#### (建议使用最新版本) ####

* Fabric:
  * [Fabric Loader](https://fabricmc.net/use/installer/)
  * [Fabric API](https://modrinth.com/mod/fabric-api)
* Forge:
  * [Forge Mod Loader](https://files.minecraftforge.net/)
* Both:
  * (可选但建议)[Cloth Config API](https://modrinth.com/mod/cloth-config)

## 常见问题 ##

* 1.19.2 Forge?...
* 对于每个主要版本，我们只会对一个小型版本进行维护。如果未来玩家群体主要使用1.19.2，我们将停止对1.19.4版本的维护，并转而支持1.19.2。

### 兼容性 ###

* 兼容:
  * 大多数摄像头模组
  * Player Animation Lib
  * Not Enough Animations
  * First Person Model
  * Pehkui
  * Epic Fight
* 不兼容:
  * Yes Steve Model(GeckoLib?)
  * Customizable Player Models
* 引起崩溃:
  * OptiFine

## [更多信息](https://github.com/xTracr/RealCamera/wiki) ##

## [更新日志](changelog.md) ##
