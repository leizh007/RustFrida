// 只装 hook，回调只返回常量（已知安全），但用 tick() 高频自触发
var M = Java.use("com.example.rfdemo.MainActivity");
var n = 0;
M.tick.implementation = function(){
    n = n + 1;
    return;
};
console.log("[R] tick hooked, callback has zero orig and no JS property reads");
console.log("[R] ticker fires every 2s from the app itself");
