.model statespace
.dummy start end c_act1 c_act2 c_act3 c_act4 c_act5 c_act6 c_act7 c_act8 c_act9 u_act10 c_act11 c_act12 c_act13 c_act14 c_satisfied
.state graph
loc0 start statespace.loc1
statespace.loc1 Spec.c_act1 statespace.loc2
statespace.loc1 Spec.c_act8 statespace.loc3
statespace.loc2 Spec.c_act6 statespace.loc4
statespace.loc2 Spec.c_act8 statespace.loc5
statespace.loc3 Spec.c_act1 statespace.loc5
statespace.loc3 Spec.c_act14 statespace.loc6
statespace.loc4 Spec.c_act8 statespace.loc7
statespace.loc5 Spec.c_act6 statespace.loc7
statespace.loc5 Spec.c_act14 statespace.loc8
statespace.loc6 Spec.c_act1 statespace.loc8
statespace.loc6 Spec.c_act11 statespace.loc9
statespace.loc7 Spec.c_act14 statespace.loc10
statespace.loc8 Spec.c_act6 statespace.loc10
statespace.loc8 Spec.c_act11 statespace.loc11
statespace.loc9 Spec.c_act1 statespace.loc11
statespace.loc10 Spec.c_act11 statespace.loc12
statespace.loc11 Spec.c_act6 statespace.loc12
statespace.loc12 Spec.c_act2 statespace.loc13
statespace.loc13 Spec.c_act7 statespace.loc14
statespace.loc14 Spec.u_act10 statespace.loc15
statespace.loc14 Spec.u_act10 statespace.loc16
statespace.loc15 Spec.c_act6 statespace.loc17
statespace.loc16 Spec.c_act3 statespace.loc14
statespace.loc17 Spec.c_act5 statespace.loc18
statespace.loc17 Spec.c_act9 statespace.loc19
statespace.loc18 Spec.c_act4 statespace.loc20
statespace.loc18 Spec.c_act9 statespace.loc21
statespace.loc19 Spec.c_act5 statespace.loc21
statespace.loc19 Spec.c_act12 statespace.loc22
statespace.loc20 Spec.c_act9 statespace.loc23
statespace.loc21 Spec.c_act4 statespace.loc23
statespace.loc21 Spec.c_act12 statespace.loc24
statespace.loc22 Spec.c_act5 statespace.loc24
statespace.loc22 Spec.c_act13 statespace.loc25
statespace.loc23 Spec.c_act12 statespace.loc26
statespace.loc24 Spec.c_act4 statespace.loc26
statespace.loc24 Spec.c_act13 statespace.loc27
statespace.loc25 Spec.c_act5 statespace.loc27
statespace.loc26 Spec.c_act13 statespace.loc28
statespace.loc27 Spec.c_act4 statespace.loc28
statespace.loc28 Post.c_satisfied statespace.loc29
statespace.loc29 end loc0
.marking {loc0}
.end
