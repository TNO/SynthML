.model minimal
.dummy start end change c_satisfied
.state graph
loc0 start s1
s1 change s2
s2 c_satisfied s3
s3 end loc0
.marking {loc0}
.end
