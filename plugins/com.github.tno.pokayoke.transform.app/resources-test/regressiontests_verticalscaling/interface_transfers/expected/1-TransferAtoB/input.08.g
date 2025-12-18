.model minimal
.dummy __start Robot1MoveToLocA ReleaseProductAtLocB HomeRobot1 PickProductAtLocA __end Robot1MoveToLocB __loop
.state graph
s1 __start s2
s2 Robot1MoveToLocA s6
s4 ReleaseProductAtLocB s5
s5 HomeRobot1 s7
s6 PickProductAtLocA s8
s7 __end s3
s8 Robot1MoveToLocB s4
s3 __loop s3
.marking {s1}
.end
