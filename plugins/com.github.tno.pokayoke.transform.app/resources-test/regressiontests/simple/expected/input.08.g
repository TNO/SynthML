.model minimal
.dummy start end prepare_robot robot_move_to_location1 robot_move_to_location2 robot_adjust_product robot_release_product_at_location2 prepare_location2 measure_product_position
.state graph
loc0 start s1
s1 prepare_location2 s2
s1 prepare_robot s3
s2 prepare_robot s5
s3 prepare_location2 s5
s4 end loc0
s5 robot_move_to_location2 s8
s6 robot_adjust_product s7
s6 robot_move_to_location1 s4
s7 measure_product_position s6
s8 robot_release_product_at_location2 s7
.marking {loc0}
.end
