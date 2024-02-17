.model statespace
.dummy start end a b
.state graph
loc0 start s2
s2 a s3
s3 b s5
s2 b s4
s4 a s5
s5 end loc0
.marking {loc0}
.end
