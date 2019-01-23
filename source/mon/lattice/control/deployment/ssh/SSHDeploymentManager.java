/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mon.lattice.control.deployment.ssh;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import mon.lattice.control.deployment.DeploymentException;
import mon.lattice.control.deployment.EntityDeploymentDelegate;
import mon.lattice.control.deployment.DataConsumerInfo;
import mon.lattice.control.deployment.DataSourceInfo;
import mon.lattice.control.deployment.MonitorableEntityInfo;
import mon.lattice.im.delegate.DCNotFoundException;
import mon.lattice.im.delegate.DSNotFoundException;
import mon.lattice.im.delegate.InfoPlaneDelegate;
import mon.lattice.core.ID;
import mon.lattice.core.EntityType;
import java.io.File;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author uceeftu
 */
public class SSHDeploymentManager implements EntityDeploymentDelegate {
    final String localJarFilePath;
    final String remoteJarFilePath;
    final String jarFileName;
    final JSch jsch;
    
    final Map<InetSocketAddress, DataSourceInfo> resourcesDataSources;
    final Map<InetSocketAddress, DataConsumerInfo> resourcesDataConsumers;
    final Map<InetSocketAddress, MonitorableEntityInfo> resources;
    
    final Map<ID, InetSocketAddress> dsIDsAddresses;
    final Map<ID, InetSocketAddress> dcIDsAddresses;
    
    
    final InfoPlaneDelegate infoPlaneDelegate;
    
    String identityFile = System.getProperty("user.home") + "/.ssh/id_rsa";
    
    Logger LOGGER = LoggerFactory.getLogger(SSHDeploymentManager.class);
    
    
    public SSHDeploymentManager(String localJarFilePath, String jarFileName, String remoteJarFilePath, InfoPlaneDelegate info) {
        this.localJarFilePath = localJarFilePath;
        this.remoteJarFilePath = remoteJarFilePath;
        this.jarFileName = jarFileName;
        this.jsch = new JSch();
        
        this.resourcesDataSources = new ConcurrentHashMap<>(); 
        this.resourcesDataConsumers = new ConcurrentHashMap<>();
        this.resources = new ConcurrentHashMap();
        
        this.dsIDsAddresses = new ConcurrentHashMap();
        this.dcIDsAddresses = new ConcurrentHashMap();
        
        this.infoPlaneDelegate = info;
    }
    
    public SSHDeploymentManager(String identityFile, String localJarFilePath, String jarFileName, String remoteJarFilePath, String entityFileName, EntityType entityType, InfoPlaneDelegate info) {
        this(localJarFilePath, jarFileName, remoteJarFilePath, info);
        this.identityFile = identityFile;
    }
    
   
    
    private String parseDeps(List<String> depJars) {
        StringBuilder s = new StringBuilder(); 
        for (String path : depJars) {
            s.append(':');
            s.append(path);
        }
        
        return s.toString();
    }
    

    
    @Override
    public ID startDataSourceIfDoesNotExist(MonitorableEntityInfo resource, DataSourceInfo dataSource) throws DeploymentException {
        DataSourceInfo existingDataSource;
        Session session = null;
        Channel channel = null;
         
        // adding the current resource to the map if not already present
        resources.putIfAbsent(resource.getAddress(), resource);
        
        // checking if a Data Source is already running/being started on that Resource address/port
        existingDataSource = this.resourcesDataSources.putIfAbsent(resource.getAddress(), dataSource);

        if (existingDataSource != null && infoPlaneDelegate.containsDataSource(existingDataSource.getId()))
           dataSource = existingDataSource; // there is a DS on that host/resource - re-using that one
        
        synchronized(dataSource)
            {
             try {
                if (dataSource.isRunning())
                    return dataSource.getId(); // a DS is already up and running - exit immediately

                session = this.connectWithKey(resource);
                
                this.deployJarOnResource(resource, session);

                LOGGER.debug("Future " + dataSource.getEntityType() + " ID: " + dataSource.getId());

                String jvm = "java"; //we assume the executable is in the PATH
                String command = jvm + 
                                 " -cp " + this.remoteJarFilePath + "/" + this.jarFileName + " " + 
                                 dataSource.getEntityClassName() + " " +   
                                 dataSource.getId() + " " +
                                 dataSource.getArguments();

                channel = session.openChannel("exec");
                ((ChannelExec) channel).setCommand(command);
                channel.connect(3000);

                // we are supposed to wait here until either the announce message sent by the DS 
                // is received from the Announcelistener thread or the timeout is reached (5 secs)
                infoPlaneDelegate.addDataSource(dataSource.getId(), 20000);

                // if there is no Exception before we can now try to get the Data Source PID
                dataSource.setpID(infoPlaneDelegate.getDSPIDFromID(dataSource.getId()));
                dataSource.setRunning();
                
                dsIDsAddresses.put(dataSource.getId(), resource.getAddress());

                // has to catch DeploymentException    
                } catch (JSchException | DSNotFoundException e) {
                    // we are here if there was an error while starting the remote Data Source
                    String errorMessage = "Error while starting " + dataSource.getEntityType() + " on " + resource.getAddress() + " " + e.getMessage();
                    if (channel != null) {
                        if (!channel.isClosed())
                            errorMessage += ". The SSH remote channel is still open - the DS may be up and running. ";
                        else
                            errorMessage += "Remote process exit-status " + channel.getExitStatus();
                    }

                    // TODO we may now collect the error log file to report back the issue 
                    throw new DeploymentException(errorMessage);

                } catch (InterruptedException ie) {
                    LOGGER.info("Interrupted " + ie.getMessage());
                }
                  catch (DeploymentException de) {
                    throw de;
                  }
             
                  finally {
                    // as the command was started without a pty when we close the channel 
                    // and session the remote command will continue to run
                    if (channel != null && session != null) {
                        channel.disconnect();
                        session.disconnect();
                    }
                  }
            return dataSource.getId();
            }
    }

    
    
    @Override
    public ID startDataSource(MonitorableEntityInfo resource, DataSourceInfo dataSource) throws DeploymentException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    

    @Override
    public boolean stopDataSource(ID dataSourceID) throws DeploymentException {
        Session session = null;
        Channel channel = null;
       
        InetSocketAddress resourceAddressFromDSID;
        DataSourceInfo dataSource = null;
        
        resourceAddressFromDSID = this.dsIDsAddresses.get(dataSourceID);
        LOGGER.debug(resourceAddressFromDSID.toString());
        
        try {
            if (resourceAddressFromDSID == null)
                throw new DSNotFoundException("Data Source with ID " + dataSourceID + " not found");
        
            dataSource = this.resourcesDataSources.get(resourceAddressFromDSID);
            synchronized (dataSource) {
                if (dataSource == null) {
                    return false;
                }

                session = this.connectWithKey(resources.get(resourceAddressFromDSID));
                LOGGER.debug("Stopping " + dataSource.getEntityType());
                String command = "kill " + dataSource.getpID();
                LOGGER.debug(command);
                channel = session.openChannel("exec");
                ((ChannelExec) channel).setCommand(command);
                channel.connect(3000);
                while (true) {
                    if (channel.isClosed()) {
                        if (channel.getExitStatus() == 0) {
                            this.resourcesDataSources.remove(resourceAddressFromDSID);
                            break;
                        } else {
                            // the process is likely to be already stopped: removing from the map
                            this.resourcesDataSources.remove(resourceAddressFromDSID);
                            throw new DeploymentException("exit-status: " + channel.getExitStatus());
                        }
                    }
                    Thread.sleep(500);
                }
            }
        } catch (JSchException | DSNotFoundException e) {
                throw new DeploymentException("Error while stopping DataSource, " + e.getMessage());
        } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
        } finally {
                if (session != null && channel != null) {
                    channel.disconnect();
                    session.disconnect();
                }
            }
        return true;
    }
    
    
    @Override
    public ID startDataConsumer(MonitorableEntityInfo resource, DataConsumerInfo dataConsumer) throws DeploymentException {
        DataConsumerInfo existingDataConsumer;
        Session session = null;
        Channel channel = null;
         
        // adding the current resource to the map if not already present
        resources.putIfAbsent(resource.getAddress(), resource);
        
        // checking if a Data Consumer is already running/being started on that Resource address/port
        existingDataConsumer = this.resourcesDataConsumers.putIfAbsent(resource.getAddress(), dataConsumer);

        if (existingDataConsumer != null && infoPlaneDelegate.containsDataConsumer(existingDataConsumer.getId()))
           dataConsumer = existingDataConsumer; // there is a DC on that resource - using that one
        
        synchronized(dataConsumer)
            {
             try {
                if (dataConsumer.isRunning())
                    return dataConsumer.getId(); // a DS is already up and running - exit immediately

                session = this.connectWithKey(resource);
                
                this.deployJarOnResource(resource, session);

                LOGGER.debug("Future " + dataConsumer.getEntityType() + " ID: " + dataConsumer.getId());

                String jvm = "java"; //we assume the executable is in the PATH
                String command = jvm + 
                                 " -cp " + this.remoteJarFilePath + "/" + this.jarFileName + " " + 
                                 dataConsumer.getEntityClassName() + " " +   
                                 dataConsumer.getId() + " " +
                                 dataConsumer.getArguments();

                channel = session.openChannel("exec");
                ((ChannelExec) channel).setCommand(command);
                channel.connect(3000);

                // we are supposed to wait here until either the announce message sent by the DS 
                // is received from the Announcelistener thread or the timeout is reached (5 secs)
                infoPlaneDelegate.addDataConsumer(dataConsumer.getId(), 20000);

                // if there is no Exception before we can now try to get the Data Source PID
                dataConsumer.setpID(infoPlaneDelegate.getDCPIDFromID(dataConsumer.getId()));
                dataConsumer.setRunning();
                
                dcIDsAddresses.put(dataConsumer.getId(), resource.getAddress());

                // has to catch DeploymentException    
                } catch (JSchException | DCNotFoundException e) {
                    // we are here if there was an error while starting the remote Data Source
                    String errorMessage = "Error while starting " + dataConsumer.getEntityType() + " on " + resource.getAddress() + " " + e.getMessage();
                    if (channel != null) {
                        if (!channel.isClosed())
                            errorMessage += ". The SSH remote channel is still open - the DS may be up and running. ";
                        else
                            errorMessage += "Remote process exit-status " + channel.getExitStatus();
                    }

                    // TODO we may now collect the error log file to report back the issue 
                    throw new DeploymentException(errorMessage);

                } catch (InterruptedException ie) {
                    LOGGER.info("Interrupted " + ie.getMessage());
                }
                  catch (DeploymentException de) {
                    throw de;
                  }
             
                  finally {
                    // as the command was started without a pty when we close the channel 
                    // and session the remote command will continue to run
                    if (channel != null && session != null) {
                        channel.disconnect();
                        session.disconnect();
                    }
                  }
            return dataConsumer.getId();
            }
    }
    

    @Override
    public boolean stopDataConsumer(ID dataConsumerID) throws DeploymentException {
        Session session = null;
        Channel channel = null;
       
        InetSocketAddress resourceAddressFromDCID;
        DataConsumerInfo dataConsumer = null;    
        
        resourceAddressFromDCID = this.dcIDsAddresses.get(dataConsumerID); 
        
        try {
            if (resourceAddressFromDCID == null)
                throw new DCNotFoundException("Data Consumer with ID " + dataConsumerID + " not found");
            
            dataConsumer = this.resourcesDataConsumers.get(resourceAddressFromDCID);
            synchronized (dataConsumer) {
                if (dataConsumer == null) {
                    return false;
                }

                session = this.connectWithKey(resources.get(resourceAddressFromDCID));
                LOGGER.debug("Stopping " + dataConsumer.getEntityType());
                String command = "kill " + dataConsumer.getpID();
                channel = session.openChannel("exec");
                ((ChannelExec) channel).setCommand(command);
                channel.connect(3000);
                while (true) {
                    if (channel.isClosed()) {
                        if (channel.getExitStatus() == 0) {
                            this.resourcesDataConsumers.remove(resourceAddressFromDCID);
                            break;
                        } else {
                            // the process is likely to be already stopped: removing from the map
                            this.resourcesDataConsumers.remove(resourceAddressFromDCID);
                            throw new DeploymentException("exit-status: " + channel.getExitStatus());
                        }
                    }
                    Thread.sleep(500);
                }
            }
        } catch (JSchException | DCNotFoundException  e) {
                throw new DeploymentException("Error while stopping DataConsumer, " + e.getMessage());
        } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
        } finally {
                if (session != null && channel != null) {
                    channel.disconnect();
                    session.disconnect();
                }
            }
        return true;
    }

    
    boolean deployJarOnResource(MonitorableEntityInfo resource, Session session) throws DeploymentException {
        synchronized (resource) 
            {
            File jarFile = new File(this.localJarFilePath + "/" + this.jarFileName);
                if (!jarFile.exists()) {
                    throw new DeploymentException("Error: file " + this.localJarFilePath + "/" + this.jarFileName + " does not exist");
                }

            if (resource.isJarDeployed() && jarFile.lastModified() <= resource.getJarDeploymentDate()) {
                return false;
            }
            
            LOGGER.debug("Deploying " + jarFile.getName() + " on" + resource.getAddress());
            
            ChannelSftp channelSftp = null;
            
            try {
                Channel channel = session.openChannel("sftp");
                channel.connect(3000);
                channelSftp = (ChannelSftp) channel;
                channelSftp.put(this.localJarFilePath + "/" + this.jarFileName, this.remoteJarFilePath + "/" + this.jarFileName, ChannelSftp.OVERWRITE);
                
                LOGGER.debug("Copying: " + this.localJarFilePath + "/" + this.jarFileName 
                                         + "to: " + this.remoteJarFilePath + "/" + this.jarFileName);
                
                resource.setJarDeploymentDate(jarFile.lastModified());
                resource.setJarDeployed();
                
            } catch (JSchException | SftpException e) {
                throw new DeploymentException("Error while deploying " + jarFile.getName() + " on " + resource.getAddress() + ", " + e.getMessage());
            } finally {
                if (channelSftp != null)
                    channelSftp.disconnect();
            }
        }
        return true;
    }
    
    
    
    Session connectWithKey(MonitorableEntityInfo resource) throws JSchException {
        LOGGER.debug("Using identity from file: " + identityFile);
        jsch.addIdentity(identityFile);
        Session session = jsch.getSession(resource.getCredentials(), resource.getAddress().getHostName(), resource.getAddress().getPort());
        session.setConfig("PreferredAuthentications", "publickey");
        session.setConfig("StrictHostKeyChecking", "no"); //ignore unknown hosts
        session.connect(3000);
        return session;
    }
    
    
    
}
