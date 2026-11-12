function log(m){ console.log("[S] "+String(m)); }
log("start");
var M = Java.use("com.example.rfdemo.MainActivity");
log("wrapper ok");
var o = M.computeSum;
M.computeSum.implementation = function(a,b){ log("HIT "+a+","+b); return o.call(this,a,b); };
log("installed");
log("call: " + M.computeSum(1,2));
log("done");
