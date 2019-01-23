/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mon.lattice.control.deployment;

/**
 *
 * @author uceeftu
 */
public interface DeploymentInterface<ReturnType> {
    ReturnType startDataSource(String endPoint, String port, String userName, String args) throws Exception;
    
    ReturnType stopDataSource(String dsID) throws Exception;
    
    ReturnType startDataConsumer(String endPoint, String port, String userName, String args) throws Exception;
    
    ReturnType stopDataConsumer(String dcID) throws Exception;
    
    public void initDeployment();
}
