.model minimal
.dummy change __start __end __reset
.state graph
s1 __start s2
s2 change s4
s4 __end s3
s3 __reset s1
.marking {s1}
.end
