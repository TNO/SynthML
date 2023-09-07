# Setting up a Poka Yoke development environment

## Prerequisites

Before setting up the development environment:

* GitHub should correctly be set up regarding SSH keys (in order to check out the repository using SSH).
  * Instructions for generating new SSH keys and adding them to your GitHub account can be found [here](https://docs.github.com/en/authentication/connecting-to-github-with-ssh).
  * Alternatively, you can generate an SSH key using the Eclipse installer by following the instructions in the subsection below.
       
#### Generating SSH keys within the Eclipse installer

The following steps can be used to generate an SSH key using the Eclipse installer, instead of, e.g., using Git Bash: 

1. After step 3 of the setup instructions below, click on the small round key-shaped icon on the bottom-left of the Eclipse installer.
2. Go to the 'Key Management' tab.
3. Click 'Generate RSA Key...'.
4. Add the public key that is displayed to your GitHub account using [these](https://docs.github.com/en/authentication/connecting-to-github-with-ssh/adding-a-new-ssh-key-to-your-github-account) instructions.
   Select 'Authentication Key' as the key type.
5. Enter a passphrase, which is optional, but recommended.
6. Click 'Save Private Key...' and save the SSH key.
   * It would be easiest to save the key as 'id_rsa' in the '.ssh' directory, potentially overwriting any existing such key, as is suggested by the 'Save As' dialog.
   * If you don't do that and choose a different name instead, then git might not automatically find the generated key and some extra configuration may be required to fix that.
     Open or create the file `~/.ssh/config`, and add the following two lines to it, thereby replacing `XXX` with the name you've chosen.
     ```
     Host github.com
     IdentityFile ~/.ssh/XXX
     ```
       
## Setup

Use the following steps to set up an Eclipse development environment for Poka Yoke development:

1. Download the Eclipse Installer, from https://eclipse.org/downloads.
2. Run the Eclipse Installer.
3. Switch to Advanced mode, using the hamburger menu.
   * In case you haven't yet configured SSH keys, consider following the steps described above in the rerequisites, for generating SSH keys within the Eclipse installer.
5. Select `Eclipse Platform`, `2023-06` and `JRE 17.* - https://download.eclipse.org/justj/jres/17/updates/release/latest`.
6. Click `Next`.
7. Use the green plus button to add `file:/X:/PokaYoke/Oomph/com.github.tno.pokayoke.setup`.
  Choose `Catalog: Eclipse Projects` in the dropdown box.
8. Select `PokaYoke` and press `Next`.
9. Enable `Show all variables` and configure `Root install folder`, `Installation folder name`, `GitHub account full name` and `GitHub account email address`.
10. Click `Next` and then click `Finish`.
11. When the installer asks trusting licenses and content, accept all licenses and trust all content from all authorities.
  (Multiple such popups may appear.)
12. Once the installer is done, and a new development environment is launched, click `Finish` in the installer to close it.
