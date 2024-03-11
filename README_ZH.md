# Real Camera #

## [English](README.md) ##

使第一人称视角下的摄像头更加真实。  
支持的版本: 1.18-1.20 Forge & Fabric  
从[Releases](https://github.com/xTracr/RealCamera/releases)、[Modrinth](https://modrinth.com/mod/real-camera)
或[CurseForge](https://curseforge.com/minecraft/mc-mods/real-camera)下载  
快照版在[这里](https://github.com/xTracr/RealCamera/actions/workflows/build.yml)

## 特性 ##

* 将摄像头绑定到身体的特定部位
* 自定义摄像头的位置和旋转角度
* 在第一人称视角下渲染玩家模型
* 按下F6来开关，另外一些键来调整摄像头
* 在配置界面配置以上特性（需Cloth Config）
* 理论上支持大多数模组模型，但需要手动进行配置：
  * 首先设置`打开模型视图界面`的按键绑定
  * 打开模型视图界面，左Alt+左键选择模型的对应的面，左Alt+滚轮可以在模型的不同层间切换
  * 通过点击左侧的`选择`按钮，在三者间切换，选好`向前矢量`、`向上矢量`和`目标平面`
  * 进入`预览`部分，在这里可以看到摄像头与模型的相对关系并进行一定的调整（也可以通过按键绑定调整）
  * 输入名称并保存（如果需要，还有其他设置可以更改）

### 依赖项目 ###

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

* 不兼容:
  * OptiFine
* 兼容:
  * 多数摄像头模组
  * 多数模型模组

## [更新日志](changelog.md#中文) ##
