# rfdemo — rustfrida 最小验证 APK

一个刻意做到最小的注入目标，用来在**没有加固、不含第三方 SDK、不是系统应用**的干净
环境下验证 rustfrida 的注入与 hook 能力。

Timemark / XCamera 那类真实目标会被 VMP 拦截（fault addr `0x88c` 就是它的 LSPosed
检测码），拿来测工具本身会混入无关变量。这个 demo 只做一件事：提供几个普通的
应用层方法当 hook 靶子。

## 靶子方法

| 方法 | 类型 | 用途 |
|---|---|---|
| `computeSum(int,int)` | static | 静态方法 hook |
| `getSecret()` | instance | 实例方法 hook（返回 String） |
| `instanceMethod(int)` | instance | 实例方法 hook（带参数 + 算术） |
| `tick()` | instance | 由 2 秒定时器反复调用，观察重复命中 |

`tick()` 在 `onCreate` 后就每 2 秒跑一次并打 `RFDemo: tick counter=N`，
是「app 本身还活着」的心跳证据。

## 构建

需要 `build-tools` 与 `platforms/android-33/android.jar`。JDK 23 上不能用
`-bootclasspath`（与 target 11 冲突），所以走 `--release 8` + `-classpath`：

```bash
BT=$HOME/Library/Android/sdk/build-tools/36.0.0
PLATFORM=$HOME/Library/Android/sdk/platforms/android-33/android.jar
cd demo/rfdemo && rm -rf build && mkdir -p build/gen build/classes build/dex

$BT/aapt2 link -o build/base.apk -I $PLATFORM --manifest AndroidManifest.xml --java build/gen
javac --release 8 -classpath $PLATFORM -d build/classes src/MainActivity.java
$BT/d8 --lib $PLATFORM --min-api 21 --output build/dex $(find build/classes -name '*.class')

cp build/base.apk build/final.apk
(cd build/dex && zip -q ../final.apk classes.dex)
$BT/apksigner sign --ks /path/to/demo.keystore \
    --ks-pass pass:demopass --key-pass pass:demopass \
    --out build/signed.apk build/final.apk
```

`prebuilt/rfdemo.apk` 是已签名的成品，签名用的是临时自签名密钥，只用于测试安装。

## 测试脚本

| 脚本 | 作用 |
|---|---|
| `h_static_only.js` | 只 hook `computeSum`，验证静态 hook 与 `$orig` |
| `h_inst.js` | 只安装 `instanceMethod` 的实例 hook，不主动调用 |
| `demo_hook.js` | 同时装 static + instance hook 并触发（用于观察崩溃边界） |
| `evalm.txt` | 测 `evalm` 在目标主线程执行 JS |

## 运行

```bash
DEV=...
adb -s $DEV install -r --no-streaming prebuilt/rfdemo.apk
adb -s $DEV shell "am start -n com.example.rfdemo/.MainActivity"
adb -s $DEV shell "am force-stop com.example.rfdemo"   # 每次注入前重启，避免残留 agent
adb -s $DEV shell "am start -n com.example.rfdemo/.MainActivity"

PID=$(adb -s $DEV shell pidof com.example.rfdemo | tr -d '\r' | awk '{print $NF}')
adb -s $DEV shell "su -c 'setenforce 0 2>/dev/null; \
  (sleep 40) | /data/local/tmp/rustfrida --pid $PID -l /data/local/tmp/h_static_only.js 2>&1'"
```

`evalm` 走 stdin 喂命令，注意不要用 `-l`：

```bash
adb -s $DEV push evalm.txt /data/local/tmp/evalm.txt
adb -s $DEV shell "su -c '(sleep 30) | /data/local/tmp/rustfrida --pid $PID < /data/local/tmp/evalm.txt 2>&1'"
```

## 已验证结论（2026-11-12，小米11 M2011K2C / Android 13 / SDK 33）

| 能力 | 结果 |
|---|---|
| 注入 + agent 加载 + 连接 | ✅ `Loader: agent 加载成功` / `Agent 已连接` |
| abstract socket 回退（pidfd_getfd 不可用时） | ✅ `pidfd_getfd 不可用，改用 abstract socket 回传 ctrl fd` |
| 纯 JS 执行 | ✅ |
| `Java.use` / `$new` / 实例方法普通调用 | ✅ |
| **静态方法 hook** | ✅ `HIT 1,2` 连续 7 次命中 |
| **`evalm` 主线程执行** | ✅ 返回 `EVALM_OK` |
| 实例 hook「仅安装不调用」 | ✅ 进程存活 |
| **实例 hook「调用触发」** | ❌ SIGSEGV / SIGABRT，见下 |

### 实例 hook 崩溃（未解决）

崩溃发生在**调用被 hook 的实例方法**时，不是安装时。真机与模拟器、当前 HEAD 与
8月8日的 legacy 归档版（md5 `9ed12abe`）**全部同样复现**，因此：

- 不是环境问题（自建 demo，无加固、非系统应用、非 intrinsic 方法）
- 不是零 TLS 修复引入的
- 不是 HEAD 相对 legacy 的回归

崩溃栈的两类形态：

```
#01 libsigchain.so  AddSpecialSignalHandlerFn+32      ← 主动 abort
#02 memfd:wwb_so (deleted) offset 0xd8000             ← agent 调用方
```

`AddSpecialSignalHandlerFn` 在 AOSP 里对入参有严格校验，参数非法会直接 `abort()`。
调用点集中在 `quickjs-hook/src/jsapi/java/art_controller.rs` 的 WalkStack SIGSEGV
guard 安装逻辑（`module_dlsym("libsigchain.so", "AddSpecialSignalHandlerFn")`
→ 构造 `SigchainAction` → `sc_sigaction`），以及 `agent/src/crash_handler.rs`。

下一步应核对该调用传入的 `SigchainAction` 各字段（尤其 handler 地址、`sa_flags`、
`sa_mask`）与 AOSP `libsigchain` 的校验条件是否一致。
