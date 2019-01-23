/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mon.lattice.control.deployment;

import mon.lattice.core.ID;

/**
 *
 * @author uceeftu
 */
public interface EntityDeploymentDelegate {
    public ID startDataSourceIfDoesNotExist(MonitorableEntityInfo resource, DataSourceInfo dataSource) throws DeploymentException;
    
    public ID startDataSource(MonitorableEntityInfo resource, DataSourceInfo dataSource) throws DeploymentException;
    
    public boolean stopDataSource(ID dataSourceID) throws DeploymentException;
    
    public ID startDataConsumer(MonitorableEntityInfo resource, DataConsumerInfo dataConsumer) throws DeploymentException; 
    
    public boolean stopDataConsumer(ID dataConsumerID) throws DeploymentException;
}
