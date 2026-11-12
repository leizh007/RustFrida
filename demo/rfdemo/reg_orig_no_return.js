var M = Java.use("com.example.rfdemo.MainActivity");
var o = M.computeSum;
M.computeSum.implementation = function(a,b){
    var v = o.call(this, a, b);
    return 42;
};
console.log("[R] installed");
var r = M.computeSum(5,6);
console.log("[R] r=" + r);
