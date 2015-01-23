/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package javaapplication2;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class JavaApplication2 {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        long oldT = System.currentTimeMillis();
        long difT = 25l * 1000;
        while(true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(JavaApplication2.class.getName()).log(Level.SEVERE, null, ex);
            }
            long newT = System.currentTimeMillis();
            if(oldT + difT < newT) {
                System.out.println("old:" + oldT);
                System.out.println("new:" + newT);
                System.out.println("dif:" + difT);
                oldT = newT;
            }
        }
    }
    
}
