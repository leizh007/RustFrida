# dist — 当前构建产物

对应源码提交：`24fa815`（分支 `leizh007/andorid16`）

| 文件 | 大小 | md5 | 说明 |
|---|---|---|---|
| `rustfrida` | 5,996,880 | `80e01c27573cfed88eff2bf8effd95ac` | **唯一需要部署的二进制** |
| `libagent.so` | 3,891,896 | `746d0d5d9dc7cdcf87ab1d8fd11dc61f` | agent，已内嵌进 `rustfrida` |
| `bootstrapper.bin` | 8,073 | `7dcbcf092ccafb371dd070610edf7b38` | 已内嵌 |
| `rustfrida-loader.bin` | 14,095 | `2caf5abc18de228bd6da216aab54a4a5` | 已内嵌 |

`rustfrida` 通过 `include_bytes!` 把后三个打包进自身，所以推送单个文件即可：

```bash
adb -s $DEV push dist/rustfrida /data/local/tmp/rustfrida
adb -s $DEV shell chmod 755 /data/local/tmp/rustfrida
```

其余三个文件放在这里只为核对内嵌内容与核对 md5，单独部署没有意义。

## 重新生成

```bash
export PATH=$HOME/.cargo/bin:$PATH
python3 loader/build_helpers.py                       # bootstrapper.bin / rustfrida-loader.bin
cargo build --release --target aarch64-linux-android -p agent
cargo build --release --target aarch64-linux-android --bin rustfrida
cp target/aarch64-linux-android/release/rustfrida       dist/
cp target/aarch64-linux-android/release/libagent.so     dist/
cp loader/build/bootstrapper.bin loader/build/rustfrida-loader.bin dist/
```

构建可重现：同样源码重复构建，四个文件的 md5 保持不变。
