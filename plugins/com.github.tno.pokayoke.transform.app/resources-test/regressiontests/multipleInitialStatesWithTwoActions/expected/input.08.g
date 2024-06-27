.model minimal
.dummy left right __start __end __reset
.state graph
s1 __start s2
s2 right s4
s2 left s4
s4 __end s3
s3 __reset s1
.marking {s1}
.end
