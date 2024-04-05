.model minimal
.dummy start end Prepare_robot Robot_move_to_location1 Robot_move_to_location2 Robot_adjust_product Robot_release_product Prepare_location1 Measure_product_position c_satisfied
.state graph
loc0 start s1
s1 Prepare_location1 s2
s1 Prepare_robot s3
s2 Prepare_robot s5
s3 Prepare_location1 s5
s4 end loc0
s5 Robot_move_to_location1 s6
s6 Robot_release_product s8
s7 Robot_adjust_product s8
s7 Robot_move_to_location2 s9
s8 Measure_product_position s7
s9 c_satisfied s4
.marking {loc0}
.end
