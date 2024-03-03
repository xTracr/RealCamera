# Change Log #

## English ##

### Releases ###

### Test Versions ###

* 0.6:
* 0.6.0-alpha.1:
  * Added model view screen
  * Allowed to select the model part you want to bind directly in the screen
* 0.6.0-alpha.2:
  * Fixed #41, #46 and #48
  * Fix minor grammar errors (#44)
  * Optimized the GUI display and changed logic of selection button
  * Changed the way to render player
  * Added pause button and show cube button
  * Allowed to disable rendering vertices based on depth relative to screen
  * Added 'AutoBind' feature
* 0.6.0-alpha.3:
  * Changed to bind based on UV coordinates and improved GUI
  * Fixed #54
  * Improved Gui and Fixed a bug that would cause crash
  * Added more icons and allowed selection or deletion of saved configs in the gui
  * Changed the 'Offset Model' option to 'Bind X/Y/Z Coordinate' options
  * Improved logic for 'Auto Bind' mode
  * Combined the 'Bind Pitch/Yaw/Roll' options
  * Added 'preview' feature
  * Removed commands
  * Improved default configs and reformatted code
* 0.6.0-beta:
  * Optimized some codes


* 0.5:
* 0.5.0-beta:
  * Redesigned config screen
  * Removed commands because it's no longer useful
  * Allowed disabling rendering partial model
* 0.5.1-beta:
  * Divided `Bind Rotation` option into three options
* 0.5.2-beta:
  * Fixed #7, #8 and clipping issue
* 0.5.3-beta:
  * Compatible with Physics Mod ocean physics (#6)
  * Allowed disabling when holding a specific item
  * Added support for 1.19.2
  * Upgraded to 1.20
* 0.5.4-beta:
  * Added feature #15, fixed #35
  * Changed the way of clipping to space to avoid #23 and #32
* 0.5.5-beta:
  * Changed the way of clipping to space to avoid #37
  * Deleted the option of disabling "clip to space"
  * Separate the part that modifies camera from the part that computes it


* 0.4:
* 0.4.0-alpha:
  * Fixed an issue where the camera would not follow the model action when the player started/ended sneaking
  * Compatible with Pehkui
  * API for others to compat with
* 0.4.1-alpha:
  * Compatible with EpicFight
  * Added commands for debugging
* 0.4.2-alpha:
  * Fixed an issue where extra camera rotation cannot be applied correctly in specific situations
  * Allowed adjusting camera rotation with hotkeys
* 0.4.3-alpha:
  * Allowed adjusting camera rotation in classic mode
  * Improved calculation of crosshair hit result
  * Bug fixes:
    * model sometimes isn't rendered when flying with elytra
    * leash position isn't correct
* 0.4.4-alpha:
  * Added "dynamic crosshair" feature
  * Added "clip to space" feature


* 0.3:
* 0.3.0-alpha:
  * Made the hit result of the player's crosshair match the actual position of the crosshair
  * Attempted to implement the effect of binding mode
* 0.3.1-alpha:
  * Added camera position correction for other actions
  * Improved the effect of binding mode
* 0.3.2-alpha:
  * Added more binding mode configurations
  * Attempted to bind camera direction to the model part
* 0.3.3-alpha:
  * Allowed camera direction to be bound to the model part
* 0.3.4-alpha:
  * Allowed crosshair direction to follow the camera
  * Changed the mod's name
  * Fixed two binding mode issues:
    * Camera and model animation are not completely synchronized
    * When the inventory is opened, the camera follows the character model in the inventory
* 0.3.5-alpha:
  * Optimized code logic
  * Allowed to disable in specfic conditions
* 0.3.6-alpha:
  * Optimized code logic
  * Upgraded to 1.19.4
  * Supported Fabric


* 0.2:
* 0.2.0-alpha:
  * Added player model rendering mode
* 0.2.1-alpha:
  * Added camera position correction for sneaking action
  * Can render crosshairs in model rendering mode


* 0.1.0-alpha:
  * Added feature to move camera and its rotation center

## 中文 ##

### 正式版 ###

### 测试版 ###

* 0.6:
* 0.6.0-alpha.1:
  * 添加了模型视图界面
  * 允许在界面中选择想要绑定的模型部位
* 0.6.0-alpha.2:
  * 修复了#41、#46和#48
  * 修复了一些语法问题（#44）
  * 优化了GUI显示效果以及修改了选择按钮的逻辑
  * 修改了渲染玩家的方式
  * 添加了暂停按钮和显示立方体按钮
  * 允许根据相对于屏幕的距离禁用渲染部分顶点
  * 添加了'自动绑定'功能
* 0.6.0-alpha.3:
  * 改为基于UV坐标绑定并且改进了GUI
  * 修复了#54
  * 改进了GUI，修复了一个会导致崩溃的问题
  * 添加了更多图标，允许在GUI内选择和删除已保存的配置
  * 将'偏移模型'选项改为'绑定X/Y/Z坐标'选项
  * 改进了'自动绑定'的逻辑
  * 合并了'绑定俯仰角/偏航角/翻滚角'选项
  * 添加了'预览'功能
  * 移除了命令
  * 改进了默认配置，重新格式化代码
* 0.6.0-beta:
  * 优化了部分代码


* 0.5:
* 0.5.0-beta:
  * 重新设计了配置屏幕
  * 删除了命令，因为它不再有用
  * 允许禁用渲染部分模型
* 0.5.1-beta:
  * 把`绑定旋转`选项分为了三个选项
* 0.5.2-beta:
  * 修复了#7, #8以及clip的问题
* 0.5.3-beta:
  * 兼容Physics Mod的海洋物理 (#6)
  * 允许在手持特定物品时禁用
  * 添加了对1.19.2的支持
  * 更新至1.20
* 0.5.4-beta:
  * 添加了功能#15, 修复#35
  * 修改了clip to space的方式来避免#23和#32
* 0.5.5-beta:
  * 修改了clip to space的方式来避免#37
  * 删除了禁用"clip to space"的选项
  * 将修改摄像头的部分与计算它的部分分离开来


* 0.4:
* 0.4.0-alpha:
  * 修复了玩家开始/结束潜行时摄像头未跟上模型动作的问题
  * 兼容Pehkui
  * 用于兼容的API
* 0.4.1-alpha:
  * 兼容EpicFight
  * 新增用于调试的命令
* 0.4.2-alpha:
  * 修复了额外的摄像头旋转在特定情况下无法正确应用的问题
  * 允许用按键调整摄像头旋转
* 0.4.3-alpha:
  * 允许调整经典模式下的摄像头旋转
  * 改进了准心命中结果的计算
  * Bug修复:
    * 鞘翅飞行时模型有时不渲染
    * 缰绳位置不正确
* 0.4.4-alpha:
  * 添加了动态十字准心功能
  * 添加了防止摄像头进入方块内部(Clip to space)的功能


* 0.3:
* 0.3.0-alpha:
  * 使玩家十字准心的命中结果与十字准心的实际位置相匹配
  * 尝试实现绑定模式的效果
* 0.3.1-alpha:
  * 添加了对其余动作的摄像头位置修正
  * 初步完善了绑定模式
* 0.3.2-alpha:
  * 添加了更多绑定模式的配置
  * 尝试将摄像头方向绑定在模型上
* 0.3.3-alpha:
  * 允许将摄像头方向绑定在模型上
* 0.3.4-alpha:
  * 允许让十字准心的方向跟随摄像头
  * 修改了MOD名字
  * 修复了两个绑定模式下的问题:
    * 摄像头与模型动画不完全同步
    * 开启物品栏时摄像头跟随物品栏内人物模型转动
* 0.3.5-alpha:
  * 代码逻辑优化
  * 允许在特定情况下禁用mod部分功能
* 0.3.6-alpha:
  * 代码逻辑优化
  * 更新至1.19.4
  * 支持Fabric


* 0.2:
* 0.2.0-alpha:
  * 添加了玩家模型渲染模式
* 0.2.1-alpha:
  * 添加了对潜行动作的摄像头位置的修正
  * 可以在玩家模型渲染模式下渲染十字准心


* 0.1.0-alpha:
  * 添加了移动摄像头及其旋转中心的功能
