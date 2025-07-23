# Development guidelines

Below you find guidelines for contributing to SynthML.

## Working with Git issues
- Before opening a Git issue, first check the four overview issues to see whether the same issue, or a similar one, is already open.
	- We have overview issues for: [tasks related to formalisms](https://github.com/TNO/PokaYoke/issues/265), [tasks related to analysis (i.e., V&V)](https://github.com/TNO/PokaYoke/issues/574), [tasks related to the activity synthesis workflow](https://github.com/TNO/PokaYoke/issues/298), and [industrialization-related tasks](https://github.com/TNO/PokaYoke/issues/555).
	- If the Git issue you want to open can be described in just one or two sentences, then add a task to the relevant overview issue instead of opening a new issue. This prevents us from having a large number of small Git issues, which helps us to manage all open issues and keep the overview.
	- When updating the task list of an overview issue, also write a comment explaining what you changed to the issue list. (We don't get automatic notifications of changes to issue descriptions, so leaving a comment helps us to keep track of what changed.)
	- When adding a task to an overview issue, please add it to the appropriate category within the overview. If the task happens to have particularly high or low priority, also indicate this explicitly.
- If you want to write any new code, or change existing code, however large or small, first make a Git issue that clearly describes what you want to add or change.
	- If you want to pick up a task from an overview issue that doesn't have it's own issue yet, then first make an issue for it. Also update the overview issue by replacing the task description by the new issue number.
	- Use a descriptive issue title (for example, 'Resolve non-determinism in UML transformer' rather than 'Problem in code').
	- Keep the scope of Git issues small and clear. It's fine to make Git issues for single-line code changes (e.g., adding something to the documentation).
	- In case of big changes, consider whether it's possible to split the work up into multiple issues, or multiple PRs under the same issue, that each have a small and clear scope.
	- In case it's not possible or obvious how to cleanly split-up a big change in multiple small issues, then first discuss the idea and plan with the team.
- When opening a new Git issue, assign the appropriate labels to it.
	- In principle, every new Git issue should have (at least, but preferably exactly) one of the following labels: `formalisms`, `analysis`, `synthesis`, or `industrialization`.
	- If the Git issue addresses a bug, then assign the label `bug` to it.
	- Some Git issues use other labels as well, like `documentation` or `good first issue`. These are optional. They could be added if you find them appropriate for the new issue.
- After having opened a new Git issue, update the relevant overview issue.
	- Every non-overview issue must be included in (at least, but preferably exactly) one overview issue.
	- Ensure that the issue label is consistent with the overview issue to which the new issue is added. For example, if the new issue has label `synthesis`, then add it to the overview issue of activity synthesis tasks.
- When working on a Git issue, assign yourself to that Git issue so that we have an overview of who is working on what, and which Git issues are not yet worked on.
	- Being assigned to a Git issue means that you are in the lead of that issue.
	- For every Git issue, there should be at most one person in the lead.
- If you want to work on resolving a Git issue, make a branch for that issue, which you can do by clicking on "Create a branch". That way we ensure that branches are linked to issues.
	- By being assigned to a Git issue, you are also in the lead of any branches for that issue.
	- Only commit to a branch when you are in the lead of its corresponding issue, or otherwise in consultation with the person in lead of it.
- When closing a Git issue, update the relevant overview issue(s) accordingly.

## Working with Pull Requests (PRs)
- All contributions to the `main` branch should be made via PRs.
- PRs should be approved by at least one reviewer before they can be merged.
- When opening a PR, put the Git issue number in the title at the front (e.g., `#21 Support for hierarchical models` for a PR for Git issue 21).
- If a PR addresses or closes a Git issue, indicate this in the PR description (e.g., `Addresses #21` or `Closes #21`, in case of Git issue 21).
- Avoid scope creep in PRs. Make sure that the PR closely adheres to the scope of its Git issue.
	- In case you have a branch with code that's not fully in scope of the Git issue but is nevertheless needed, consider making separate PRs or Git issues for that code, in order to have clear scopes again and make reviewing easier.
- Try to avoid massive PRs.
	- In case of large changes to the code base affecting many files, consider making multiple PRs, possibly for the same Git issue, to incrementally contribute all changes to the main branch, which makes reviewing easier.
	- If, during reviewing, new ideas or suggestions pop up for further improvements, consider whether that should be done in the same PR, or in a follow-up PR. 
- Avoid opening a PR that conflicts with an existing one. Before opening a PR, have a look at other PRs that are open and check whether any of them would conflict with yours. If so, align with the owner of that PR on these conflicts, and on which of the conflicting PRs should go first.
- Code in an open PR must not have leftover TODOs.
	- These should either be handled directly in the PR (if the scope of the Git issue allows it), or a new Git issue should be made for them to resolve them later.
- Before opening a PR, make sure that all Java files are formatted (Ctrl+Shift+F) and their imports are organized (Ctrl+Shift+O).
- Try to avoid stale PRs that remain open for a long time (i.e., weeks).
- After having merged a PR, also make sure the corresponding branch is deleted.
- After having merged a PR, update the status of the corresponding Git issue (if not done already automatically).
	- In case a PR only addresses the issue, it is still closed when the PR is merged. If so, re-open the issue.
- Prevent wild growth of branches. If there is a branch that should be preserved (e.g., some code that you want to archive), instead make a patch and attach it to the Git issue.

## Working with code
- When picking up an issue, especially a non-trivial one, first discuss and agree upon the design with the team, before making a code implementation and PR.
	- If resolving the issue requires much implementation work, then make small steps and PRs if possible. Keeping code contributions small makes reviewing significantly easier.
- Strive to keep the code in a consistent state, to have it predictable and easy to read. Avoid making changes at one place, but not at other places that are similar.
- Use section and explanation comments in Java methods, to document at a high-level what the code is doing conceptually. Trying to reverse-engineer the intention of what the code should do, and understanding the rationale of why things are implemented in a particular way, is often difficult to do without comments that explain these. This also significantly eases reviewing.
	- For the section comments, it could help to make extra functions for sections instead, but don't make then too fine-grained. Try to find a balance. (See also [here](https://bpoplauschi.github.io/2021/01/20/Clean-Code-Comments-by-Uncle-Bob-part-2.html).)
	- Java comments should always start with a capital letter and end with a dot.
	- Refer to concepts (e.g., `// Validate the input model.`) rather than concrete variables (e.g., `// Validate 'model'.`) or classes (e.g., // `Validate Model.`).
- Try to declare class fields and methods in the order in which they are used/invoked. In a class, first declare fields, then constructors, then public methods, then protected methods, then private methods (all in invocation order).
- Avoid empty JavaDoc blocks, e.g., `/** */`, with nothing in them.
- For packages, classes and fields, prefer single-line JavaDoc instead of multi-line, in case the JavaDoc is sufficiently small to fit on a single line.
- We should aim to have our Checkstyle check as many conventions that we use as possible. If the Checkstyle can be improved with respect to that, make a Git issue to have the discussion on it as a team, and if we decide to change it, make a branch to resolve it.
- Try to add regression tests wherever possible.
	- Especially when there is not a clear input-output relation (for example, of a transformation), test cases may help to get understanding of how the code should be used and how it works.
- Try to add many assertions and preconditions, to check expectations values and input.
	- Focus in particular on expectations on the interfaces. For example, it would be good to start a method implementation by checking whether the arguments are as expected (i.e., defensive programming). There is usually no need however to check whether inputs are not `null`, since, if they are `null`, then a null pointer exception will probably be thrown anyway at some point.
	- Try to make the program fail as early as possible in case the variables/inputs don't have the expected values.
	- Upon an error, write string descriptions that clearly indicate what the error is. Try to make the error messages informative.

## Working with commits
- When committing, write a short commit message that indicates the code change/addition.
	- Start commit messages with the issue number, e.g., `#24 Commit message` for Git issue 24.
- Try to avoid large commits with many changes if possible, especially when processing reviewer feedback. Instead, try to make small commits with a clear purpose, e.g., resolving one reviewer comment. This makes reviewing faster, since any changes can then be reviewed per commit.
- In case you rename a file, first commit the rename (as a file deletion and file addition) before committing any changes to the contents of that file. That way, reviewing becomes easier.
- In general it would be good to keep branches and PRs non-conflicting with main.
- When resolving merge conflicts, never add new changes in merge commits.
	- In case of a bigger conflict, just commit the merge commit with the conflict (including conflict markers, and code from both branches). Then in a next commit solve the conflict. This makes it easier for others to see what is going on and to review it.
