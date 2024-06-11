.model minimal
.dummy start end do_a do_b do_c
.state graph
loc0 start s1
s1 do_c s2
s1 do_b s3
s1 do_a s4
s2 do_c s2
s2 do_b s6
s2 do_a s7
s3 do_c s6
s3 do_b s3
s3 do_a s8
s4 do_c s7
s4 do_b s8
s4 do_a s4
s5 end loc0
s6 do_c s6
s6 do_b s6
s6 do_a s5
s7 do_c s7
s7 do_b s5
s7 do_a s7
s8 do_c s5
s8 do_b s8
s8 do_a s8
.marking {loc0}
.end
