/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mon.lattice.im.delegate;

import mon.lattice.core.ID;
import mon.lattice.core.plane.AbstractAnnounceMessage;
import mon.lattice.core.EntityType;
import mon.lattice.core.plane.InfoPlane;
import mon.lattice.core.plane.MessageType;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;

/**
 *
 * @author uceeftu
 */
public class ControlInformationManager implements InfoPlaneDelegate {
    private final InfoPlane info;
    private final List<ID> dataSources;
    private final List<ID> dataConsumers;
    private final Map<ID, Object> pendingDataSources;
    private final Map<ID, Object> pendingDataConsumers;
    private static Logger LOGGER = LoggerFactory.getLogger(ControlInformationManager.class);
    
    
    public ControlInformationManager(InfoPlane info){
        this.info=info;
        dataSources = Collections.synchronizedList(new ArrayList());
        dataConsumers = Collections.synchronizedList(new ArrayList());
        pendingDataSources = new ConcurrentHashMap<>();
        pendingDataConsumers = new ConcurrentHashMap<>();
    }
    
    
    @Override
    public void receivedAnnounceEvent(AbstractAnnounceMessage m) {
        if (m.getMessageType() == MessageType.ANNOUNCE)
            addAnnouncedEntity(m.getEntityID(), m.getEntity());
        else if ((m.getMessageType() == MessageType.DEANNOUNCE))
                removeDeannouncedEntity(m.getEntityID(), m.getEntity());  
    }
    
    
    @Override
    public void addDataSource(ID id, int timeout) throws InterruptedException, DSNotFoundException {
        Object monitor = new Object(); 
        synchronized(monitor) {
            LOGGER.debug("Adding pending Data Source: " + id);
            pendingDataSources.put(id, monitor);
            monitor.wait(timeout);
        }
        if (pendingDataSources.containsKey(id)) //cleaning up
            pendingDataSources.remove(id);
        
        if (!containsDataSource(id)) {  
            if (!info.containsDataSource(id, 0)) //wait some more time
                throw new DSNotFoundException("Announce Message was not received by the ControlInformationManager");
            else
                addDataSource(id); //we may have lost the message but the DS might be up and running
            
        }
    }
    
    
    @Override
    public void addDataConsumer(ID id, int timeout) throws InterruptedException, DCNotFoundException {
        Object monitor = new Object(); 
        synchronized(monitor) {
            LOGGER.debug("Adding pending Data Consumer: " + id);
            pendingDataConsumers.put(id, monitor);
            monitor.wait(timeout);
        }
        if (pendingDataConsumers.containsKey(id)) //cleaning up
            pendingDataConsumers.remove(id);
        
        if (!containsDataConsumer(id)) {  
            if (!info.containsDataConsumer(id, 0))
                throw new DCNotFoundException("Announce Message was not received by the ControlInformationManager");
            else
                addDataConsumer(id); //we may have lost the message but the DC is up and running
            
        }
    }
    
    
    
    @Override
    public JSONArray getDataSources() throws JSONException {
        JSONArray obj = new JSONArray();
        for (ID id: getDataSourcesList()) {
            JSONObject dsAddr = new JSONObject();
            JSONObject dataSourceInfo = new JSONObject();
            try {
                ControlEndPointMetaData dsInfo = getDSAddressFromID(id);
                if (dsInfo instanceof ZMQControlEndPointMetaData)
                    dsAddr.put("type", ((ZMQControlEndPointMetaData)dsInfo).getType());
                else if (dsInfo instanceof SocketControlEndPointMetaData) {
                    dsAddr.put("host", ((SocketControlEndPointMetaData)dsInfo).getHost().getHostAddress());
                    dsAddr.put("port", ((SocketControlEndPointMetaData)dsInfo).getPort());
                }
                dataSourceInfo.put("id", id.toString());
                dataSourceInfo.put("info", dsAddr);
            } catch (DSNotFoundException ex) {
                LOGGER.error(ex.getMessage());
                deleteDataSource(id);
              }
            obj.put(dataSourceInfo);
        }
        return obj;
    }
    
    @Override
    public JSONArray getDataConsumers() throws JSONException {
        JSONArray obj = new JSONArray();
        for (ID id: getDataConsumersList()) {
            JSONObject dcAddr = new JSONObject();
            JSONObject dataConsumerInfo = new JSONObject();
            try {
                ControlEndPointMetaData dcInfo = getDCAddressFromID(id);
                if (dcInfo instanceof ZMQControlEndPointMetaData)
                    dcAddr.put("type", ((ZMQControlEndPointMetaData)dcInfo).getType());
                else if (dcInfo instanceof SocketControlEndPointMetaData) {
                    dcAddr.put("host", ((SocketControlEndPointMetaData)dcInfo).getHost().getHostAddress());
                    dcAddr.put("port", ((SocketControlEndPointMetaData)dcInfo).getPort());
                }
                dataConsumerInfo.put("id", id.toString());
                dataConsumerInfo.put("info", dcAddr);
            } catch (DCNotFoundException ex) {
                LOGGER.error(ex.getMessage());
                deleteDataConsumer(id);
              }
            obj.put(dataConsumerInfo);
            }
        return obj;
    }
    
    
    
    @Override
    public ControlEndPointMetaData getDSAddressFromProbeID(ID probe) throws ProbeNotFoundException, DSNotFoundException {
        String dsID = (String)info.lookupProbeInfo(probe, "datasource");
        
        if (dsID != null) {
            ID dataSourceID = ID.fromString(dsID);
            if (!containsDataSource(dataSourceID))
                throw new DSNotFoundException("Data Source with ID " + dataSourceID.toString() + " was de-announced");
            
            LOGGER.debug("Found this data source ID: " + dataSourceID);
            ControlEndPointMetaData dsAddress = getDSAddressFromID(dataSourceID);
            if (dsAddress != null)
                return dsAddress;
            else
                throw new DSNotFoundException("Data Source with ID " + dataSourceID.toString() + " not found in the infoplane");
        }
        else {
            LOGGER.error("Probe ID error");
            throw new ProbeNotFoundException("Probe with ID " + probe.toString() + " not found in the infoplane");
        }
    }
    
    @Override
    public ControlEndPointMetaData getDSAddressFromID(ID dataSource) throws DSNotFoundException {
        if (!containsDataSource(dataSource))
            throw new DSNotFoundException("Data Source with ID " + dataSource.toString() + " was not found in the infoplane");
        
        JSONObject controlEndPoint;
        Object lookupDataSourceInfo = info.lookupDataSourceInfo(dataSource, "controlendpoint");
        
        try {
            if (lookupDataSourceInfo instanceof String) 
                controlEndPoint = new JSONObject((String) lookupDataSourceInfo);
            
            else
                controlEndPoint = (JSONObject)info.lookupDataSourceInfo(dataSource, "controlendpoint");
       
            LOGGER.debug(controlEndPoint.toString());
            
            ControlEndPointMetaData dsAddress = null;
            if (controlEndPoint.getString("type").equals("socket")) {
                dsAddress = new SocketControlEndPointMetaData(controlEndPoint.getString("type"),
                                                        InetAddress.getByName(controlEndPoint.getString("address")),
                                                        controlEndPoint.getInt("port")
                                                       );
            }
            
            else if (controlEndPoint.getString("type").equals("zmq")) {
                dsAddress = new ZMQControlEndPointMetaData(controlEndPoint.getString("type"), dataSource);
            }
            return dsAddress;
        } 
        catch(Exception e) {
            throw new DSNotFoundException("error while retrieving controlEndPoint for Data Source with ID " + dataSource.toString() + e.getMessage());
        }
        
    }
        
    @Override
    public String getDSIDFromName(String dsName) throws DSNotFoundException {
        //using generic getInfo method for getting DS ID from DS name
        String dsID = (String)info.getInfo("/datasource/name/" + dsName);
        if (dsID != null)
            if (!containsDataSource(ID.fromString(dsID)))
                throw new DSNotFoundException("Data Source with ID " + dsID + " was de-announced");
            else
                return dsID;
        else 
            throw new DSNotFoundException("Data Source with name " + dsName + " not found in the infoplane");
        }  
    
    @Override
    public ControlEndPointMetaData getDCAddressFromID(ID dataConsumer) throws DCNotFoundException {
        if (!containsDataConsumer(dataConsumer))
            throw new DCNotFoundException("Data Consumer with ID " + dataConsumer.toString() + " was not found in the infoplane");
        
        JSONObject controlEndPoint;
        Object lookupDataConsumerInfo = info.lookupDataConsumerInfo(dataConsumer, "controlendpoint");
        
        try {
            if (lookupDataConsumerInfo instanceof String)
                controlEndPoint = new JSONObject((String)lookupDataConsumerInfo);
        
            else
                controlEndPoint = (JSONObject)info.lookupDataConsumerInfo(dataConsumer, "controlendpoint");
            
            LOGGER.debug(controlEndPoint.toString());
                        
            ControlEndPointMetaData dcAddress = null;
            if (controlEndPoint.getString("type").equals("socket")) {
                dcAddress = new SocketControlEndPointMetaData(controlEndPoint.getString("type"),
                                                        InetAddress.getByName(controlEndPoint.getString("address")),
                                                        controlEndPoint.getInt("port")
                                                       );
            }
            
            else if (controlEndPoint.getString("type").equals("zmq")) {
                dcAddress = new ZMQControlEndPointMetaData(controlEndPoint.getString("type"), dataConsumer);
            }

            return dcAddress;
        } 
        catch(Exception e) {
            throw new DCNotFoundException("error while retrieving controlEndPoint for Data Consumer with ID " + dataConsumer.toString() + e.getMessage());
        }    
    }
    
    @Override
    public ControlEndPointMetaData getDCAddressFromReporterID(ID reporter) throws ReporterNotFoundException, DCNotFoundException {
        String dcID = (String)info.lookupReporterInfo(reporter, "dataconsumer");
        
        if (dcID != null) {
            ID dataConsumerID = ID.fromString(dcID);
            if (!containsDataConsumer(dataConsumerID))
                throw new DCNotFoundException("Data Consumer with ID " + dataConsumerID.toString() + " was de-announced");
                
            LOGGER.debug("Found this data consumer ID: " + dataConsumerID);
            ControlEndPointMetaData dcAddress = getDCAddressFromID(dataConsumerID);
            if (dcAddress != null)
                return dcAddress;
            else
                throw new DCNotFoundException("Data Consumer with ID " + dataConsumerID.toString() + " not found in the infoplane");
        }
        else
            throw new ReporterNotFoundException("Probe with ID " + reporter.toString() + " not found in the infoplane");
    }
    
    @Override
    public Integer getDSPIDFromID(ID dataSource) throws DSNotFoundException {
        if (!containsDataSource(dataSource))
            throw new DSNotFoundException("Data Source with ID " + dataSource.toString() + " was de-announced");
        
        Integer pID = (Integer)info.lookupDataSourceInfo(dataSource, "pid");
        if (pID != null)
            return pID;
        else 
            throw new DSNotFoundException("Data Source with ID " + dataSource.toString() + " not found in the infoplane or missing pid entry"); 
    }
    
    @Override
    public int getDCPIDFromID(ID dataConsumer) throws DCNotFoundException {
        if (!containsDataConsumer(dataConsumer))
            throw new DCNotFoundException("Data Consumer with ID " + dataConsumer.toString() + " was de-announced");
        
        Integer pID = (Integer)info.lookupDataConsumerInfo(dataConsumer, "pid");
        if (pID != null)
            return pID;
        else
            throw new DCNotFoundException("Data Consumer with ID " + dataConsumer.toString() + " not found in the infoplane or missing pid entry");
    }
    

    @Override
    public JSONArray getProbesOnDS(ID dataSource) throws DSNotFoundException {
        if (!containsDataSource(dataSource))
            throw new DSNotFoundException("Data Source with ID " + dataSource.toString() + " was de-announced");
        
        JSONArray probes = (JSONArray) info.lookupProbesOnDS(dataSource);
        return probes;
    }
    
    
    @Override
    public boolean containsDataSource(ID id) {
        return dataSources.contains(id);
    }
    
    @Override
    public boolean containsDataConsumer(ID id) {
        return dataConsumers.contains(id);
    }
    
    void addDataSource(ID id) {
        dataSources.add(id);
    }
    
    void addDataConsumer(ID id) {
        dataConsumers.add(id);
    }
    
    void deleteDataSource(ID id) {    
        dataSources.remove(id);
    }
    
    void deleteDataConsumer(ID id) {
        dataConsumers.remove(id);
    }
    
    List<ID> getDataSourcesList() {
        synchronized(dataSources) {
            return dataSources;
        }
    }
    
    List<ID> getDataConsumersList() {
        synchronized(dataConsumers) {
            return dataConsumers;
        }
    }
    
    
    void addAnnouncedEntity(ID id, EntityType type) {
        if (type == EntityType.DATASOURCE && !containsDataSource(id)) {
            LOGGER.info("Adding Data Source " + id.toString());
            addDataSource(id);
            notifyDataSource(id); // notify any pending deployment threads
        }
        else if (type == EntityType.DATACONSUMER && !containsDataConsumer(id)) {
                LOGGER.info("Adding Data Consumer " + id.toString());
                addDataConsumer(id);
                notifyDataConsumer(id);
        }
    }
    
    void removeDeannouncedEntity(ID id, EntityType type) {
        if (type == EntityType.DATASOURCE && containsDataSource(id)) {
            LOGGER.info("Removing Data Source " + id.toString());
            deleteDataSource(id);
        }
        else if (type == EntityType.DATACONSUMER && containsDataConsumer(id)) {
            LOGGER.info("Removing Data Consumer " + id.toString());
            deleteDataConsumer(id);
        }
    }
    
    void notifyDataSource(ID id) {
        // checking if there is a pending deployment for that Data Source ID
        if (pendingDataSources.containsKey(id)) {
            LOGGER.debug("Notifying pending Data Source: " + id);
            Object monitor = pendingDataSources.remove(id);
            synchronized (monitor) {
                monitor.notify();
            }
        }
        // else do nothing
    }
    
    
    void notifyDataConsumer(ID id) {
        // checking if there is a pending deployment for that Data Consumer ID
        if (pendingDataConsumers.containsKey(id)) {
            LOGGER.debug("Notifying pending Data Consumer: " + id);
            Object monitor = pendingDataConsumers.remove(id);
            synchronized (monitor) {
                monitor.notify();
            }
        }
        // else do nothing
    }
}
