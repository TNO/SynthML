# SynthML

SynthML is a tool designed for formal modeling, analysis, and synthesis of UML activities.
The aim of SynthML is integrating Synthesis-Based Engineering (SBE) into UML 2.5 for the synthesis of supervisory controllers as UML activities.
Such an integration lowers the need for architects and engineers in industry to learn new formalisms and dedicated tooling for SBE.
Moreover, the synthesized supervisory controllers are presented as UML activities, which helps to make synthesis results more recognizable.
Overall, the integration of SBE into UML may help to lower the industry adoption threshold for SBE.

SynthML is an extension of [UML Designer](https://www.umldesigner.org/), adding formal annotations (like guards and effects for actions, and control flow guards), a formal execution semantics for activities, simulation capabilities (via the [Cameo Simulation Toolkit](https://www.3ds.com/products/catia/no-magic/cameo-simulation-toolkit)), and activity synthesis capabilities.
The activity synthesis algorithm uses the symbolic synthesis tool of [CIF](https://eclipse.dev/escet/cif) for synthesizing a supervisory controller, and uses the Petri net synthesis tool [Petrify](https://www.cs.upc.edu/~jordicf/petrify) to translate the controlled system in CIF to a Petri net, as a stepping stone towards translating the controlled system to a UML activity.

For further information on the vision and approach for integrating SBE into UML-based development processes, we refer to the following article:
- Wytse Oortwijn, Yuri Blankenstein, Jos Hegge, Dennis Hendriks, Pierre van de Laar, Bram van der Sanden, Laura van Veen, and Nan Yang, "Towards Synthesis-Based Engineering for Cyber-Physical Production Systems", 13th International Conference on Model-Based Software and Systems Engineering (MODELSWARD), pages 158-168, 2025, doi: [10.5220/0013103300003896](https://doi.org/10.5220/0013103300003896).

## Documentation

For end users:
- [Instructions for using SynthML](docs/synthml-instructions.pdf)

For developers:
- [Setting up a Poka Yoke development environment](docs/setup-development-environment.md)
- [Development guidelines](docs/development-guidelines.md)
- [The formal definition of the execution semantics of activities in SynthML](plugins/com.github.tno.pokayoke.activitysynthesis/docs/semantics.pdf)
- [Detailed information on how the activity synthesis algorithm works](plugins/com.github.tno.pokayoke.activitysynthesis/docs/documentation.pdf)

## License

Copyright (c) 2023-2025 TNO and Contributors to the GitHub community

This program and the accompanying materials are made available under the terms of the [Eclipse Public License v2.0](https://spdx.org/licenses/EPL-2.0.html).

SPDX-License-Identifier: EPL-2.0