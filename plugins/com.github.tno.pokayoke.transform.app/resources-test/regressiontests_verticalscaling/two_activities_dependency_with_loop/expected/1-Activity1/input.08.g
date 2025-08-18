.model minimal
.dummy __start flipExtra flipProp1 __end __loop
.state graph
s1 __start s2
s2 flipExtra s4
s2 flipProp1 s5
s4 flipProp1 s6
s5 flipExtra s6
s5 flipProp1 s2
s6 __end s3
s3 __loop s3
.marking {s1}
.end
