.model minimal
.dummy prepare_robot robot_move_to_location1 robot_move_to_location2 robot_adjust_product robot_release_product_at_location2 prepare_location2 measure_product_position __start __end __reset
.state graph
s1 __start s2
s2 prepare_location2 s4
s2 prepare_robot s5
s4 prepare_robot s9
s5 prepare_location2 s9
s6 robot_adjust_product s7
s6 robot_move_to_location1 s8
s7 measure_product_position s6
s8 __end s3
s9 robot_move_to_location2 s10
s10 robot_release_product_at_location2 s7
s3 __reset s1
.marking {s1}
.end
