# ESCET CIF Parser

The classes in this plugin are generated from [Eclipse ESCET v3.0](https://gitlab.eclipse.org/eclipse/escet/escet/-/tree/v3.0/cif/org.eclipse.escet.cif.parser?ref_type=tags) by

1. [Setup Eclipse ESCET development environment](https://eclipse.dev/escet/development/development/dev-env-setup.html) and switch to tag `v3.0`
2. Adding start symbols to [cif.setext](https://gitlab.eclipse.org/eclipse/escet/escet/-/blob/v3.0/cif/org.eclipse.escet.cif.parser/src/org/eclipse/escet/cif/parser/cif.setext?ref_type=tags#L232) (see [Add-Poka-Yoke-parser-start-symbols.patch](Add-Poka-Yoke-parser-start-symbols.patch)).
3. Running [gen-cif-scanner-parser.launch](https://gitlab.eclipse.org/eclipse/escet/escet/-/blob/v3.0/cif/org.eclipse.escet.cif.parser/gen-cif-scanner-parser.launch?ref_type=tags).
4. Copying the generated parsers to [src-gen/com/github/tno/pokayoke/cif/parser](src-gen/com/github/tno/pokayoke/cif/parser)
