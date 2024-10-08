.model minimal
.dummy __start __end __loop
.state graph
s1 __start s2
s2 __end s3
s3 __loop s3
.marking {s1}
.end
