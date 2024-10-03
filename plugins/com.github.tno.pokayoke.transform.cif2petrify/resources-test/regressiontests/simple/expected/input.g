.model statespace
.dummy c_act1 c_act2 c_act3 c_act4 c_act5 c_act6 c_act7 c_act8 c_act9 u_act10 c_act11 c_act12 c_act13 c_act14 c_satisfied __loop
.state graph
loc1 c_act1 loc2
loc1 c_act8 loc3
loc2 c_act6 loc4
loc2 c_act8 loc5
loc3 c_act1 loc5
loc3 c_act14 loc6
loc4 c_act8 loc7
loc5 c_act6 loc7
loc5 c_act14 loc8
loc6 c_act1 loc8
loc6 c_act11 loc9
loc7 c_act14 loc10
loc8 c_act6 loc10
loc8 c_act11 loc11
loc9 c_act1 loc11
loc10 c_act11 loc12
loc11 c_act6 loc12
loc12 c_act2 loc13
loc13 c_act7 loc14
loc14 u_act10 loc15
loc14 u_act10 loc16
loc15 c_act6 loc17
loc16 c_act3 loc14
loc17 c_act5 loc18
loc17 c_act9 loc19
loc18 c_act4 loc20
loc18 c_act9 loc21
loc19 c_act5 loc21
loc19 c_act12 loc22
loc20 c_act9 loc23
loc21 c_act4 loc23
loc21 c_act12 loc24
loc22 c_act5 loc24
loc22 c_act13 loc25
loc23 c_act12 loc26
loc24 c_act4 loc26
loc24 c_act13 loc27
loc25 c_act5 loc27
loc26 c_act13 loc28
loc27 c_act4 loc28
loc28 c_satisfied loc29
loc29 __loop loc29
.marking {loc1}
.end
