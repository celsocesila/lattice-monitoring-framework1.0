/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.fivegex.monitoring.control.probescatalogue;

/**
 *
 * @author uceeftu
 */
public interface ProbesCatalogue <ReturnType, ExceptionType extends Throwable> {
    ReturnType getProbesCatalogue() throws ExceptionType; 
    
    void initCatalogue();
}
