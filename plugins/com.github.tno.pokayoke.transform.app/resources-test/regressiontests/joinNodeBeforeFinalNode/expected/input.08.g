.model minimal
.dummy action1 action2 __start __end __reset
.state graph
s1 __start s2
s2 action2 s4
s2 action1 s5
s4 action1 s6
s5 action2 s6
s5 action1 s5
s6 __end s3
s6 action1 s6
s3 __reset s1
.marking {s1}
.end
