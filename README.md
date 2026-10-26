# 🛡️ PicSeed Vault

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android_8.0+-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-M3_Expressive-brightgreen.svg)](https://developer.android.com/jetpack/compose)
[![Release](https://img.shields.io/badge/Release-v1.0.0-blue.svg)](https://github.com/icyamire/PicSeed-Vault/releases/tag/v1.0.0)

**PicSeed Vault** (基于有序文件与PIN码的确定性加密助记词生成器 / Deterministic Multi-Chain Crypto Seed Generator) 是一个离线且高度安全的 Android 应用程序。它通过用户选择的**有序媒体文件**（图片、视频、文档等）结合**个人 PIN 码**，使用确定性加密引擎衍生出符合工业标准的 **BIP-39** 助记词及多链加密货币地址（Bitcoin、Ethereum、Monero 等）。

---

## 🌟 核心特性 / Key Features

- 🧙 **向导式交互 (Wizard UI)**
  四步简洁向导（文件选择与排序 ➔ PIN 码设置 ➔ 确定性计算 ➔ 密钥库生成），体验流畅。
- 🔐 **确定性种子引擎 (Deterministic Seed Engine)**
  基于文件的 SHA-256 规范散列与拼接，配合 **PBKDF2-HMAC-SHA512**（带有 PIN 码加盐与多轮密钥拉伸），确保相同的输入文件序列与 PIN 码必定生成完全相同的助记词与私钥。
- 🌐 **标准 BIP-39 & 多链扩展 (Multi-Chain Support)**
  - **BIP-39**: 12/24 词标准助记词生成与校验。
  - **Bitcoin (BTC)**: 支持 **BIP-44 Legacy** (`1...`) 及 **BIP-84 Native SegWit** (`bc1q...`) 派生路线。
  - **Ethereum (ETH / EVM)**: 支持 **BIP-44** (`m/44'/60'/0'/0/0`) 导出与 EIP-55 校验和格式。
  - **Monero (XMR)**: 支持 Monero 25 词衍生助记词、Primary Address (`4...`)、Private Spend Key 及 Private View Key 导出。
- 🛡️ **硬件与系统级防截屏保护 (FLAG_SECURE)**
  全局启用 `FLAG_SECURE`，禁止系统截图、屏幕录制及最近任务预览，防止恶意外界窥探。
- ✈️ **零网络依赖与绝对离线 (100% Offline-First)**
  无需申请任何网络权限，所有密码学计算均在本地设备 CPU 完成，绝不上传任何凭据。
- 📖 **交互式安全备份引导 (Interactive Backup Guidance)**
  内置备份风险警示与规范指导，提示用户妥善保存文件原始副本与 PIN 码。

---

## 🔗 标准兼容性 / Standard Compatibility

PicSeed Vault 生成的助记词与私钥完全遵循标准规范，可无缝导入市面上主流去中心化钱包与硬钱包：

| 链 / 资产 (Asset) | 标准 / 路径 (Standard / Path) | 兼容钱包 (Compatible Wallets) |
| :--- | :--- | :--- |
| **Ethereum / EVM** | BIP-39, BIP-44 (`m/44'/60'/0'/0/0`) | MetaMask, OKX Wallet, Trust Wallet, TokenPocket, Rainbow |
| **Bitcoin (SegWit)** | BIP-39, BIP-84 (`m/84'/0'/0'/0/0`) | Electrum, Sparrow Wallet, OKX Wallet, Trust Wallet |
| **Bitcoin (Legacy)** | BIP-39, BIP-44 (`m/44'/0'/0'/0/0`) | Electrum, Trust Wallet, Coinomi |
| **Monero (XMR)** | Electrum 25-word Seed, Primary Addr | Feather Wallet, Cake Wallet, Monero GUI |

---

## 🏗️ 架构与技术栈 / Architecture & Tech Stack

项目采用现代 Android 架构设计模式与声明式 UI 范式搭建：

- **UI 框架**: [Jetpack Compose](https://developer.android.com/jetpack/compose) + Material Design 3 Expressive 风格，支持动态配色与 Edge-to-Edge 沉浸式布局。
- **导航框架**: [androidx.navigation3](https://developer.android.com/guide/navigation) (`NavDisplay` & `NavBackStack`) 状态驱动导航。
- **密码学引擎 (Pure Kotlin Cryptography)**:
  - SHA-256 / PBKDF2-HMAC-SHA512
  - Secp256k1 公钥衍生 & 点乘计算
  - Keccak-256 (Ethereum) & Ripemd160 (Bitcoin)
  - Bech32 / Base58Check 编解码器
- **异步处理**: Kotlin Coroutines & `StateFlow` / `ViewModel` 响应式数据流。

---

## 🛠️ 构建与运行 / How to Build & Run

### 编译前提 / Prerequisites
- JDK 17+
- Android Studio Ladybug (2024.2.1) 或更高版本
- Android SDK Platform 35 (Android 15)

### 编译步骤 / Build Steps

```bash
# 1. 克隆代码库 / Clone the repository
git clone https://github.com/icyamire/PicSeed-Vault.git
cd PicSeed-Vault

# 2. 编译 Debug APK / Build Debug APK
./gradlew assembleDebug

# 3. APK 输出位置 / Output path
# app/build/outputs/apk/debug/app-debug.apk
```

运行单元测试与密码学引擎验证测试：
```bash
./gradlew test
```

---

## ⚠️ 安全免责声明与备份警告 / Security Disclaimer & Backup Warnings

> [!IMPORTANT]
> **确定性计算原理与备份要求**：
> 1. **文件顺序敏感**：选择的文件**顺序改变**、**文件内容发生哪怕 1 bit 的修改**，生成的助记词与地址将完全不同！
> 2. **备份核心文件与 PIN**：请务必备份所选文件的**原图/原文件**（避免微信、QQ 等社交软件压图修改文件元数据）以及记牢您的 PIN 码。
> 3. **免责声明**：本软件按“原样”提供，不提供任何形式的明示或暗示担保。使用者需自行承担因文件丢失、PIN 码遗忘或误操作导致的加密资产损失风险。

---

## 📄 许可证 / License

```text
Copyright 2025 icyamire

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations oars under the License.
```
