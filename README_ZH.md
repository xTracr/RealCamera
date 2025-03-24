# Real Camera #

## [English](README.md) ##

使第一人称视角下的摄像头更加真实。  
支持的版本: 1.20-1.21 Fabric & NeoForge  
从[Releases](https://github.com/xTracr/RealCamera/releases)、[Modrinth](https://modrinth.com/mod/real-camera)或[CurseForge](https://curseforge.com/minecraft/mc-mods/real-camera)下载  
快照版在[这里](https://github.com/xTracr/RealCamera/actions/workflows/build.yml)

## 特性 ##

* 将摄像头绑定到身体的特定部位
* 自定义摄像头的位置和旋转角度
* 在第一人称视角下渲染玩家模型
* 按下F6来开关，另外一些键来调整摄像头
* 在配置界面配置以上特性（需Cloth Config）

### 配置 ###

* 理论上支持大多数模组模型，但需要手动进行配置：
* 首先设置`打开模型视图界面`的按键绑定
  * ![model view screen](https://cdn.modrinth.com/data/fYYSAh4R/images/cc484d54238992077ab3632c274a2631efeca35f.png)
* 打开模型视图界面，左Alt+左键选择模型的对应的面，左Alt+滚轮可以在模型的不同层间切换
* 通过点击左侧的`选择`按钮，在三者间切换，选好`向前矢量`、`向上矢量`和`目标平面`
* 进入`预览`部分，在这里可以看到摄像头与模型的相对关系并进行一定的调整（也可以通过按键绑定调整）
  * ![preview](https://cdn.modrinth.com/data/fYYSAh4R/images/22cfcf444bbf2d3c0d0280e470a29f01b9308617.png)
* 输入名称并保存（如果需要，还有其他设置如优先级可以更改）

## 依赖项目 ##

* Fabric:
  * [Fabric API](https://modrinth.com/mod/fabric-api)
* 所有平台:
  * (可选但建议)[Cloth Config API](https://modrinth.com/mod/cloth-config)

## 常见问题 ##

* 模型的一部分（如头发）始终挡在面前，怎样隐藏它?
  * 增加这个值或许有所帮助  
    ![screenshot_2024_6_2_22_42](https://github.com/xTracr/RealCamera/assets/57320980/78c246e8-34aa-4979-89de-780ee907870b)

### 兼容性 ###

* 不兼容:
  * OptiFine
  * Armourer's Workshop
  * 基于GeckoLib的盔甲
* 兼容:
  * 多数摄像头模组
  * 多数模型模组


* 模型模组与`Real Camera`兼容的必要条件（基于官方映射）：
* 渲染时序兼容
  * `Real Camera`通过调用`EntityRenderDispatcher.render`公共方法进行`Minecraft.getCameraEntity`的渲染[（源码位置）](https://github.com/xTracr/RealCamera/blob/main/common/src/main/java/com/xtracr/realcamera/RealCameraCore.java#L88)
  * 调用时机调整至`GameRenderer.renderLevel`流程中的`Camera.setup`阶段之前[（源码位置）](https://github.com/xTracr/RealCamera/blob/main/common/src/main/java/com/xtracr/realcamera/mixin/MixinGameRenderer.java#L48)
  * *因此*，模组需确保在此时序调整后，玩家模型渲染的整体表现不受影响
* 顶点数据获取
  * `Real Camera`通过替换公共方法`EntityRenderDispatcher.render`的`MultiBufferSource multiBufferSource`参数实现顶点数据获取[（源码位置）](https://github.com/xTracr/RealCamera/blob/main/common/src/main/java/com/xtracr/realcamera/RealCameraCore.java#L88)
  * *因此*，模组需要满足以下技术条件：
    * 渲染`Minecraft.getCameraEntity`时严格使用**传入的**`multiBufferSource`参数
    * 没有通过其他途径获取或创建`MultiBufferSource`实例
    * 所有顶点数据最终通过`MultiBufferSource.getBuffer`方法获取的`VertexConsumer`发送给GPU
