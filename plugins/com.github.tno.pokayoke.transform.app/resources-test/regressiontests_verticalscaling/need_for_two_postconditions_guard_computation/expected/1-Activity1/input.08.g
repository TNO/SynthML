.model minimal
.dummy __start __end __node__InitialNode__32__0 __node__CallBehaviorAction__36 __node__ActivityFinalNode__34__0 __loop
.state graph
s1 __start s2
s2 __end s3
s2 __node__InitialNode__32__0 s4
s4 __node__CallBehaviorAction__36 s6
s5 __node__ActivityFinalNode__34__0 s4
s6 __node__ActivityFinalNode__34__0 s2
s6 __node__InitialNode__32__0 s5
s3 __loop s3
.marking {s1}
.end
