.model minimal
.dummy __start __node__InitialNode__44__0 __node__MergeNode__50__0 __node__OpaqueAction__46 __node__DecisionNode__52__1 __node__DecisionNode__52__0 __node__ActivityFinalNode__48__0 __end __node__MergeNode__50__1 __loop
.state graph
s1 __start s2
s2 __node__InitialNode__44__0 s4
s4 __node__MergeNode__50__0 s5
s5 __node__OpaqueAction__46 s6
s6 __node__DecisionNode__52__1 s9
s6 __node__DecisionNode__52__0 s7
s7 __node__ActivityFinalNode__48__0 s8
s8 __end s3
s9 __node__MergeNode__50__1 s5
s3 __loop s3
.marking {s1}
.end
