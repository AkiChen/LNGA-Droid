# 第三方来源与版权说明

## 上游项目

本项目是 Android 客户端，基于并参考了 [LNGA HarmonyOS](https://github.com/apap6628114/lnga_harmony)。上游仓库 README 标注其许可证为 GNU GPL v2.0；本仓库保留 GPL-2.0 许可证文本。请在分发本项目源码或二进制时同时提供适用的许可证和对应源码，并保留上游已有的版权、许可和免责声明。

当前 Android 源码树没有检索到逐文件的版权/修改日期声明。若某文件属于 GPL 覆盖的上游派生文件，发布前应依据适用条款补齐相应声明。这里没有擅自填写个人或组织版权人信息。

## NGA 表情图片

应用内置的 `app/src/main/res/drawable-nodpi/nga_emotion_*.png` 是 NGA 表情资源；仓库中的 `tools/import_harmony_emotions.py` 用于从 LNGA HarmonyOS 仓库的 `entry/src/main/resources/rawfile/emotions` 导入这些图片。本项目选择沿用上游做法，将表情文件保留在源码仓库中，并随应用一起打包发布。上游 README 列出了内置的 a2/ac/dt/ng/pg/pst 六套表情，并将仓库标注为 GPL-2.0。

目前没有找到逐张图片的作者或独立授权声明；上述说明记录的是上游的仓库级发布方式，不构成对每张图片权利来源的独立确认。表情资源来源于 [LNGA HarmonyOS](https://github.com/apap6628114/lnga_harmony)，相关标识和权利仍归其各自权利人所有。

## 构建工具与依赖

Gradle Wrapper 脚本自身包含 Apache-2.0 版权/许可标记。AndroidX、Kotlin、OkHttp、Coil、Media3 及测试库等依赖由 Gradle 构建文件声明，各自受其上游许可证约束；本说明不替代对最终依赖清单及传递依赖的完整许可证审查。
