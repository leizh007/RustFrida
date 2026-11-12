var M = Java.use("com.example.rfdemo.MainActivity");
var o = M.computeSum;
M.computeSum.implementation = function(a,b){ return o.call(this,a,b); };
console.log("[R] static hooked, now invoking from JS");
var r = M.computeSum(1,2);
console.log("[R] r=" + r);
