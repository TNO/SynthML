.model minimal
.dummy __start Action __end __loop
.state graph
s1 __start s2
s2 Action s4
s4 __end s3
s3 __loop s3
.marking {s1}
.end
