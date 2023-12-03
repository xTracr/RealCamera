# Real Camera #

## [English](README.md) ##

使第一人称视角下的摄像头更加真实。  
支持的版本: 1.18-1.20 Forge & Fabric  
从[Releases](https://github.com/xTracr/RealCamera/releases)、[Modrinth](https://modrinth.com/mod/real-camera)或[CurseForge](https://curseforge.com/minecraft/mc-mods/real-camera)下载  
快照版在[这里](https://github.com/xTracr/RealCamera/actions/workflows/build.yml)  

## 特性 ##

* 将摄像头绑定到身体的某个部位
* 自定义摄像头的位置和旋转角度
* 在第一人称视角下渲染玩家模型
* 按下F6来开关，另外一些键来调整摄像头
* 在Config界面配置以上特性（需Cloth Config）

> 详细了解[如何配置](https://github.com/xTracr/RealCamera/wiki/Configuration)

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

* 施工中

### 兼容性 ###

* 引起崩溃:
  * OptiFine
* 不兼容:
  * Yes Steve Model(GeckoLib?)
  * Customizable Player Models
* 兼容:
  * 大多数摄像头模组
  * Player Animation Lib
  * Not Enough Animations[^1]
  * First Person Model
  * Pehkui
  * Epic Fight

[^1]:建议在 *更多动画* 的配置内禁用`Animation Smoothing`选项。

## [更多信息](https://github.com/xTracr/RealCamera/wiki) ##

## [更新日志](changelog.md#中文) ##
