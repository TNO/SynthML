.model minimal
.dummy __start __node__InitialNode__109__0 flip2 __node__CallBehaviorAction__113 __node__CallBehaviorAction__113__na_result_1 __node__ActivityFinalNode__111__0 __end __loop
.state graph
s1 __start s2
s2 __node__InitialNode__109__0 s4
s2 flip2 s5
s4 __node__CallBehaviorAction__113 s7
s4 flip2 s6
s5 __node__InitialNode__109__0 s6
s6 __node__CallBehaviorAction__113 s12
s7 __node__CallBehaviorAction__113__na_result_1 s10
s7 flip2 s12
s8 __node__ActivityFinalNode__111__0 s9
s9 __end s3
s10 __node__ActivityFinalNode__111__0 s11
s10 flip2 s8
s11 flip2 s9
s12 __node__CallBehaviorAction__113__na_result_1 s8
s3 __loop s3
.marking {s1}
.end
