# Poka Yoke UML Profile

This plugin defines the Poka Yoke UML profile using [UML Designer](https://github.com/TNO/UML-Designer).

## Generating Ecore

From this profile, the [com.github.tno.pokayoke.uml.profile.plugin](../com.github.tno.pokayoke.uml.profile.plugin) ecore plugin is generated using [UML Designer](https://github.com/TNO/UML-Designer).
Perform the following steps to re-generate this plugin when the profile has changed:

1. Remove the `com.github.tno.pokayoke.uml.profile.plugin` project from your workspace, **but don't remove its project contents on disk!**
2. Open the Poka Yoke UML profile diagram and drag the `Export Profile` from the `Tools` palette to the diagram canvas.
3. Follow the steps, using `com.github.tno.pokayoke.uml.profile.plugin` as `Profile plug-in name`.
4. When asked to export deployable plug-ins and fragments, you can cancel the wizard.
5. The workspace now contains a new generated `com.github.tno.pokayoke.uml.profile.plugin` project which is closed.
   This project is now located in your Eclipse workspace, use the properties view to see its location on disk.
6. Replace the `src` and `model` folders from the `com.github.tno.pokayoke.uml.profile.plugin` in Git with the ones from the generated project.
7. Remove the generated project from your workspace, **including its project contents on disk!**
8. Re-import projects from Git by means of an Oomph refresh.
