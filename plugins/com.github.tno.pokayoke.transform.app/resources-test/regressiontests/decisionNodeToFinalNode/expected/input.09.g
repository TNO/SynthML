.model minimal
.dummy action __start __end __reset
.state graph
s1 __start s2
s2 action s4
s4 __end s3
s4 action s4
s3 __reset s1
.marking {s1}
.end
