.model minimal
.dummy __start __node__InitialNode__58__0 __node__DecisionNode__66__0 __node__ActivityFinalNode__60__0 __end __node__OpaqueAction__62 __node__MergeNode__68__1 __loop
.state graph
s1 __start s2
s2 __node__InitialNode__58__0 s4
s4 __node__DecisionNode__66__0 s7
s5 __node__ActivityFinalNode__60__0 s6
s6 __end s3
s7 __node__OpaqueAction__62 s8
s8 __node__MergeNode__68__1 s5
s3 __loop s3
.marking {s1}
.end
