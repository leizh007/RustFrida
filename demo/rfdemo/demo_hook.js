function log(m){ console.log("[T] "+String(m)); }
log("start");

// 1) 静态方法 hook
var M = Java.use("com.example.rfdemo.MainActivity");
log("class wrapper ok");
var oSum = M.computeSum;
M.computeSum.implementation = function(a, b){
    var r = oSum.call(this, a, b);
    log("computeSum(" + a + "," + b + ") orig=" + r + " -> 999");
    return 999;
};
log("static hook installed");

// 2) 实例方法 hook
var oInst = M.instanceMethod;
M.instanceMethod.implementation = function(x){
    var r = oInst.call(this, x);
    log("instanceMethod(" + x + ") orig=" + r + " -> 777");
    return 777;
};
log("instance hook installed");

// 3) 直接触发
log("trigger static: " + M.computeSum(2, 3));
log("done");
