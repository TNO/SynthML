import os
import shutil

for file in os.listdir("./"):
    basePath = "./" + file
    expectedPath = basePath + "/expected"
    actualPath = basePath + "/actual"

    if os.path.exists(expectedPath) and os.path.exists(actualPath):
        shutil.rmtree(expectedPath)
        os.rename(actualPath, expectedPath)
        print("Copied " + file)
