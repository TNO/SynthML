/**
 *
 */
package com.github.tno.pokayoke.transform.cif;

import java.io.IOException;
import java.util.LinkedHashSet;

import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.Behavior;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Model;



/**
 *
 */
public class CIFTransformer {

    private final Model model;

    public CIFTransformer(Model model) {
        this.model = model;
    }

    public static void transformFile(String sourcePath, String targetPath) throws IOException {
        Model model = FileHelper.loadModel(sourcePath);


    }
    public void transformModel() {

        //extract activities
        Class contextClass = (Class)model.getMember("Context");

        // Transform all activity behaviors of 'contextClass'.
        for (Behavior behavior: new LinkedHashSet<>(contextClass.getOwnedBehaviors())) {
            if (behavior instanceof Activity) {

            }
        }

    }

    public void transformActivity() {


    }

    public void transoformFork() {


    }






    public static void main(String args[])
    {
        Model model = FileHelper.loadModel("C:\\Users\\nanyang\\Documents\\Investigations\\2023-08-11 - Simulating the deadlock situation\\deadlock.uml");
        System.out.println(model.getName());
    }

}
