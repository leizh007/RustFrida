var M = Java.use("com.example.rfdemo.MainActivity");
var o = M.computeSum;
M.computeSum.implementation = function(a,b){ return 42; };
console.log("[R] hook returns constant, no orig, no js ops in cb");
var r = M.computeSum(1,2);
console.log("[R] r=" + r);
