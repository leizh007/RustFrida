var M = Java.use("com.example.rfdemo.MainActivity");
var o = M.computeSum;
M.computeSum.implementation = function(a,b){
    o.call(this, a, b);   // 调用 orig，但丢弃结果、不返回
    return 42;            // 返回常量，完全不碰 orig 的返回值
};
console.log("[R] installed (orig called, result discarded)");
var r = M.computeSum(1,2);
console.log("[R] r=" + r);
