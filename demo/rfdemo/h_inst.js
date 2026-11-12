function log(m){ console.log("[I] "+String(m)); }
log("start");
var M = Java.use("com.example.rfdemo.MainActivity");
var o = M.instanceMethod;
M.instanceMethod.implementation = function(x){ log("HIT x="+x); return o.call(this,x); };
log("instance hook installed");
var act = Java.use("android.app.ActivityThread");
log("ActivityThread ok");
log("done");
