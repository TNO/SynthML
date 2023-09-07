# Setting up a Poka Yoke development environment

## Prerequisites

Before setting up the development environment:

* GitHub should correctly be set up regarding SSH keys (in order to check out the repository using SSH).
  * Instructions for generating new SSH keys and adding them to your GitHub account can be found [here](https://docs.github.com/en/authentication/connecting-to-github-with-ssh).

## Setup

Use the following steps to set up an Eclipse development environment for Poka Yoke development:

* Download the Eclipse Installer, from https://eclipse.org/downloads.
* Run the Eclipse Installer.
* Switch to Advanced mode, using the hamburger menu.
* Select `Eclipse Platform`, `2023-06` and `JRE 17.* - https://download.eclipse.org/justj/jres/17/updates/release/latest`.
* Click `Next`.
* Use the green plus button to add `file:/X:/PokaYoke/Oomph/com.github.tno.pokayoke.setup`.
  Choose `Catalog: Eclipse Projects` in the dropdown box.
* Select `PokaYoke` and press `Next`.
* Enable `Show all variables` and configure `Root install folder`, `Installation folder name`, `GitHub account full name` and `GitHub account email address`.
* Click `Next` and then click `Finish`.
* When the installer asks trusting licenses and content, accept all licenses and trust all content from all authorities.
  (Multiple such popups may appear.)
* Once the installer is done, and a new development environment is launched, click `Finish` in the installer to close it.
