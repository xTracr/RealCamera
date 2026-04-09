# Real Camera

### [English](README.md)

使第一人称视角下的摄像头更加真实。  
支持的版本: 1.18.2 - 1.20.4 Forge，1.18.2 - 1.21.8 Fabric，1.21 - 1.21.8 NeoForge
从[Releases](https://github.com/xTracr/RealCamera/releases)、[Modrinth](https://modrinth.com/mod/real-camera)或[CurseForge](https://curseforge.com/minecraft/mc-mods/real-camera)下载  
快照版在[这里](https://github.com/xTracr/RealCamera/actions/workflows/build.yml)

## 特性

* 将摄像头绑定到身体的特定部位
* 自定义摄像头的位置和旋转角度
* 在第一人称视角下渲染玩家模型
* 按下F6来开关，另外一些键来调整摄像头
* 在配置界面（需要Cloth Config）和模型视图（0.6+）界面，配置以上特性

### 配置（0.6+）

* 理论上支持大多数模组模型，但需要手动进行配置：
* 首先设置`打开模型视图界面`的按键绑定
    * ![gui_configs](https://cdn.modrinth.com/data/fYYSAh4R/images/4625282a5683cd38eba2947be1ca82e595e18bad.png)
* 打开模型视图界面，左Alt+左键选择模型的对应的面，左Alt+滚轮可以在模型的不同层间切换
* 通过点击左侧的`选择`按钮，在三者间切换，选好`向前矢量`、`向上矢量`和`目标平面`
* 从右上角的按钮进入`预览`界面，在这里可以看到摄像头与模型的相对关系，并进行一定的调整（也可以通过按键绑定调整）
    * ![gui_preview](https://cdn.modrinth.com/data/fYYSAh4R/images/44c87a6f1750f8d1b03422120e6042d1098896cb.png)
* 输入名称并保存

#### Tips

* 可以更改配置的优先级，优先级越高的配置，在右侧排序越靠上
* 在`预览`界面可以更改禁用深度来禁用眼前的模型的渲染
* 关于`禁用`界面（当前版本尚不完善，操作较为繁琐）：
    * 在左侧材质id输入框为空时，可以使用Alt+左键选择材质
    * 被蓝框包含的部分不会被渲染。
    * 在左侧材质视图中鼠标左键框选，其它键取消选择
    * Alt+左键可快速选中被鼠标指针指向的部分
    * `全部`模式下，整个材质所对应的模型都不会被渲染（左侧会有一个包含整个材质的蓝框来表示这一点）
    * ![gui_disable](https://cdn.modrinth.com/data/fYYSAh4R/images/b49ac4da6bf8a59f13c7e93ca1ef76b73e5d23b4.png)

## 依赖项目

* Fabric:
    * [Fabric API](https://modrinth.com/mod/fabric-api)
* 所有平台:
    * (可选但建议)[Cloth Config API](https://modrinth.com/mod/cloth-config)

## 常见问题

* 模型的一部分（如头发）始终挡在面前，怎样隐藏它?
    * 增加这个值或许有所帮助，或者使用`禁用`功能
      ![disable_depth](https://github.com/xTracr/RealCamera/assets/57320980/78c246e8-34aa-4979-89de-780ee907870b)
* 按什么键打开模组视图界面?
    * 在按键绑定里自己设置
* 为什么打不开配置界面（模组设置）?
    * 没有安装[Cloth Config API](https://modrinth.com/mod/cloth-config)
* 为什么ysm模型有时候会弹出绑定失败?
    * 快照0.6.15“尝试”修复了ysm问题，但没有修复透彻
    * 如果Minecraft版本为1.20.1，那么需要将向上矢量绑定在头部的左面（或右面），并在预览界面中将翻滚角设置为90度（或-90度）
* 为什么配置ysm时候，模型视图界面中ysm模型头部消失?
    * 安装了Better Combat模组，去掉就可以了

### 兼容性

* 不兼容:
    * OptiFine
    * Armourer's Workshop（时装工坊）（真实相机0.6版本以上）
    * 基于GeckoLib的盔甲
    * Customizable Player Models（自定义玩家模型）
    * Epic Fight（史诗战斗）（真实相机0.6版本以下）
    * [TaCZ]永恒枪械工坊：零 1.1.4版本及以上
* 兼容:
    * 大多数修改玩家镜头的模组
    * 多数模型模组
    * Armourer's Workshop（时装工坊）（真实相机0.6版本以下）
    * Epic Fight（史诗战斗）（真实相机0.6版本以上）
    * First-person Model（更真实的第一人称模型）
    * Not Enough Animations（更多动画）
    * Player Animation Lib
    * Pehkui
    * ParCool！
    * [TaCZ]永恒枪械工坊：零 1.0.3版本及以下
    * Yes Steve Model（是，史蒂夫模型）（不能稳定兼容）
    * Superb Warfare（卓越前线）

* 模型模组与`Real Camera`兼容的必要条件（基于官方映射）：
* 顶点数据获取
    * `Real Camera`通过替换公共方法`EntityRenderDispatcher.render`的`MultiBufferSource multiBufferSource`参数实现顶点数据获取
    * 调用时机为`GameRenderer.renderLevel`流程中的`Camera.setup`阶段之前
    * *因此*，模组需要满足以下技术条件：
        * 渲染`Minecraft.getCameraEntity`时严格使用**传入的**`multiBufferSource`参数，没有通过其他途径获取或创建`MultiBufferSource`实例
        * 在调整后的时机渲染玩家模型，其整体表现不受影响
        * 所有顶点数据最终通过`MultiBufferSource.getBuffer`方法获取的`VertexConsumer`发送给GPU
* 渲染次数兼容
    * `Real Camera`通过调用`EntityRenderDispatcher.render`公共方法进行`Minecraft.getCameraEntity`的渲染
    * 这是在一帧中第二次调用`EntityRenderDispatcher.render`方法（第一次是为了解算摄像机参数）
    * *因此*，模组需要满足：同一帧内**多次**渲染玩家模型，其整体表现不受影响
