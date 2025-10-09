The model contains
- one Boolean property 'init'
- one abstract activity 'AbsActivity' (see later)
- one concrete activity 'ConcrActivity' (see picture), that transforms 'init' from 'false' to 'true' and calls 'CalledActivity'
- one concrete activity 'CalledActivity' (see picture), that does *not* transform 'init', calls 'UselessOpaqueBehavior'
- one opaque behavior 'UselessOpaqueBehavior', with 'true' guard and empty effects

The abstract activity 'AbsActivity' has 
- precondition: not init
- postcondition: init 
- occurrence constraint: at most one call to 'CalledActivity' and one call to 'UselessOpaqueBehavior'

In short, the abstract activity should call 'ConcrActivity' once, and can also call 'UselessOpaqueBehavior' once. 
The occurrence constraint should *not* count the (internal) call to 'UselessOpaqueBehavior' located in 'CalledActivity'.