# Setting up a Poka Yoke development environment

## Prerequisites

Before setting up the development environment:

* GitHub should correctly be set up regarding SSH keys (in order to check out the repository using SSH).

## Setup

Use the following steps to set up an Eclipse development environment for Poka Yoke development:

* Download the Eclipse Installer, from https://eclipse.org/downloads.
* Run the Eclipse Installer.
* Switch to Advanced mode, using the hamburger menu.
* Select `Eclipse Platform`, `2022-06` and `JRE 11.* - https://download.eclipse.org/justj/jres/11/updates/release/latest`.
* Click `Next`.
* Use the green plus button to add `https://github.com/TNO/PokaYoke/raw/main/com.github.tno.pokayoke.setup?oomph_form=b%27login%27`. Login is required, via a login form, which should appear. Furthermore, choose `Catalog: Eclipse Projects`.
* Select `PokaYoke` and press `Next`.
* Enable `Show all variables` and configure `Root install folder`, `Installation folder name`, `GitHub account full name` and `GitHub account email address`.
* Click `Next` and then click `Finish`.
* Once the installer is done, and a new development environment is launched, click `Finish` in the installer to close it.
* The first launch of the new development environment requires another login to GitHub, via a login form that should appear at startup.