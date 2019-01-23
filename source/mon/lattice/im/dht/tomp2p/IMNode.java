package mon.lattice.im.dht.tomp2p;

import mon.lattice.appl.dataconsumers.DefaultControllableDataConsumer;
import mon.lattice.core.ControllableDataSource;
import mon.lattice.core.DataSource;
import mon.lattice.core.Probe;
import mon.lattice.core.ProbeAttribute;
import mon.lattice.core.ID;
import mon.lattice.core.ControllableReporter;
import java.io.Serializable;
import java.io.IOException;
import java.util.Collection;
import mon.lattice.core.ControllableDataConsumer;
import mon.lattice.appl.datasources.DockerDataSource;
import mon.lattice.core.plane.AbstractAnnounceMessage;
import mon.lattice.core.plane.AnnounceEventListener;
import java.net.InetAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;

/**
 * An IMNode is responsible for converting  DataSource, ControllableDataConsumer and Probe
 attributes into Hashtable keys and values for the DistributedHashTable.
 * <p>
 * For example, with a given DataSource you get:
 * <ul>
 * <li> /datasource/datasource-id/attribute = value
 * </ul>
 * and with a given Probe you get:
 * <ul>
 * <li> /probe/probe-id/attribute = value
 * </ul>
 */
public class IMNode implements AnnounceEventListener {
    // The actual DHT
    DistributedHashTable dht = null;
    
    AnnounceEventListener listener;

    // the local port
    int localPort = 0;
    
    int localUDPPort = 0;
    int localTCPPort = 0;

    // the remote host
    String remoteHost;
    
    // the remote port
    int remotePort = 0;
    
    static Logger LOGGER = LoggerFactory.getLogger(IMNode.class);

    /**
     * Construct an IMNode, given a local port and a remote host
     * and a remote port.
     */
    public IMNode(int myPort, String remHost, int remPort) {
	localPort = myPort;
	remoteHost = remHost;
	remotePort = remPort;
    }
    
    /**
     * Construct an IMNode, given local TCP and UDP ports and a remote port.
     */
    
    public IMNode(int myUDPPort, int myTCPPort, int remPort) {
	localUDPPort = myUDPPort;
        localTCPPort = myTCPPort;
        
	remotePort = remPort;
        remoteHost = null; // will be initialized after connection
    }
    
    
    /**
     * Construct an IMNode, given local TCP and UDP ports and a remote port.
     */
    
    public IMNode(int myUDPPort, int myTCPPort, String remHost, int remPort) {
	localUDPPort = myUDPPort;
        localTCPPort = myTCPPort;
        remoteHost = remHost;
        
	remotePort = remPort;
    }
    
    /**
     * Construct an IMNode, given a local port and a remote port.
     */
    
    public IMNode(int myPort, int remPort) {
	localPort = myPort;
	remotePort = remPort;
        remoteHost = null; // will be initialized after connection
    }
    
    public IMNode(int myPort) {
	localPort = myPort;
	remotePort = localPort;
        remoteHost = null; // will be initialized after connection
    }

    /**
     * Connect to the DHT peers.
     */
    public boolean connect() {
        String remoteConnectedHost = null;
        
	try {
	    // only connect if we don't already have a DHT
	    if (dht == null) {
                if (localPort == remotePort) {
                    dht = new DistributedHashTable(localPort);
                    remoteHost = dht.connect();
                }
                else {
                    if (localPort != 0)
                        dht = new DistributedHashTable(localPort, InetAddress.getLocalHost());
                    else
                        dht = new DistributedHashTable(localUDPPort, localTCPPort, InetAddress.getLocalHost());
                    if (remoteHost == null)
                       remoteConnectedHost = dht.connect(remotePort);
                    else
                       remoteConnectedHost = dht.connect(remoteHost, remotePort);
                }

                //setting this IMNode as a AnnounceEventListener in the DHT
                dht.addAnnounceEventListener(this);
                
		return remoteConnectedHost != null;
	    } else {
		return true;
	    }
	} catch (IOException ioe) {
	    LOGGER.error("Connect failed: " + ioe);
	    if (dht != null) {
		try {
		    dht.close();
		} catch (IOException e) {
		}
		dht = null;
	    }
	    return false;
	}
    }

    /**
     * Disconnect from the DHT peers.
     */
    public boolean disconnect() {
        if (dht != null) {
            try {
                dht.close();
                dht = null;
                return true;
            } catch (IOException e) {
                dht = null;
                return false;
            }
        }
        // was already disconnected so returning true anyway
        return true;
    }

    public String getRootHostname() {
        return this.remoteHost;
    }
    
    public IMNode addDataConsumer(ControllableDataConsumer dc) throws IOException {
        putDHT("/dataconsumer/" + dc.getID() + "/name", dc.getName());  
        
        JSONObject controlEndPoint = new JSONObject(dc.getControlPlane().getControlEndPoint());
        putDHT("/dataconsumer/" + dc.getID() + "/controlEndPoint", controlEndPoint.toString());
        
        for (ControllableReporter r: dc.getReportersCollection()) {
            if (r instanceof ControllableReporter)
                addReporter((ControllableReporter)r);
        }
        
        return this;
    }
    
    
    public IMNode addDataConsumerInfo(ControllableDataConsumer dc) throws IOException {
        // this maps the name to the ID
	putDHT("/dataconsumer/name/" + dc.getName(), dc.getID().toString()); 
        
        if (dc instanceof DefaultControllableDataConsumer)
            putDHT("/dataconsumer/" + dc.getID() + "/pid", ((DefaultControllableDataConsumer) dc).getMyPID());       
	return this;
    }
    
    public IMNode addReporter(ControllableReporter r) throws IOException {
        putDHT("/reporter/" + r.getId() + "/name", r.getName());
        putDHT("/reporter/" + r.getId() + "/dataconsumer", r.getDcId().toString());
        return this;
    }
    
    
    /**
     * Add data for a DataSource
     */
    public IMNode addDataSource(DataSource ds) throws IOException {
	putDHT("/datasource/" + ds.getID() + "/name", ds.getName());     
        
        JSONObject controlEndPoint = new JSONObject();
        if (ds instanceof DockerDataSource && ((DockerDataSource) ds).getDataSourceConfigurator() != null) {
            String externalHost = ((DockerDataSource) ds).getDataSourceConfigurator().getDockerHost();
            int controlPort = ((DockerDataSource) ds).getDataSourceConfigurator().getControlForwardedPort();
            
            try {
            controlEndPoint.put("address", externalHost);
            controlEndPoint.put("port", controlPort);
            controlEndPoint.put("type", "socket/NAT");
            } catch (JSONException e) {
                return null;
            }
            
            putDHT("/datasource/" + ds.getID() + "/controlendpoint", controlEndPoint.toString());
        }
            
        else {
              controlEndPoint = new JSONObject(ds.getControlPlane().getControlEndPoint());
              putDHT("/datasource/" + ds.getID() + "/controlEndPoint", controlEndPoint.toString());
        }
        
	Collection<Probe> probes = ds.getProbes();

	// skip through all probes
	for (Probe aProbe : probes) {
	    addProbe(aProbe);
	}
	    
	return this;
    }
    
    public IMNode addDataSourceInfo(DataSource ds) throws IOException {
        // this maps the name to the ID
	putDHT("/datasource/name/" + ds.getName(), ds.getID().toString()); 
        
        if (ds instanceof ControllableDataSource)
            putDHT("/datasource/" + ds.getID() + "/pid", ((ControllableDataSource) ds).getMyPID());       
	return this;
    }
    
    
    /**
     * Add data for a Probe.
     */
    public IMNode addProbe(Probe aProbe) throws IOException {
	// add probe's ref to its data source
	// found through the ProbeManager
	DataSource ds = (DataSource)aProbe.getProbeManager();
	putDHT("/probe/" + aProbe.getID() + "/datasource", ds.getID().toString());

	// add probe name to DHT
	putDHT("/probe/" + aProbe.getID() + "/name", aProbe.getName());
	putDHT("/probe/" + aProbe.getID() + "/datarate", aProbe.getDataRate().toString());
	putDHT("/probe/" + aProbe.getID() + "/on", aProbe.isOn());
	putDHT("/probe/" + aProbe.getID() + "/active", aProbe.isActive());

	// now probe attributes
	Collection<ProbeAttribute> attrs = aProbe.getAttributes();

	putDHT("/probeattribute/" + aProbe.getID() + "/size", attrs.size());
	// skip through all ProbeAttributes
	for (ProbeAttribute attr : attrs) {
	    addProbeAttribute(aProbe, attr);
	}

	return this;
    }

    /**
     * Add data for a ProbeAttribute.
     */
    public IMNode addProbeAttribute(Probe aProbe, ProbeAttribute attr)  throws IOException {
	String attrRoot = "/probeattribute/" + aProbe.getID() + "/" +
	    attr.getField() + "/";

	putDHT(attrRoot + "name", attr.getName());
	putDHT(attrRoot + "type", attr.getType().getCode());
	putDHT(attrRoot + "units", attr.getUnits());

	return this;

    }

    /*
     * Modify stuff
     */
    public IMNode modifyDataSource(DataSource ds) throws IOException {
	// remove then add
	throw new IOException("Not implemented yet!!");
    }

    public IMNode modifyProbe(Probe p) throws IOException {
	throw new IOException("Not implemented yet!!");
    }

    public IMNode modifyProbeAttribute(Probe p, ProbeAttribute pa)  throws IOException {
	throw new IOException("Not implemented yet!!");
    }


    /*
     * Remove stuff
     */
    public IMNode removeDataSource(DataSource ds) throws IOException {
	remDHT("/datasource/" + ds.getID() + "/name");
        remDHT("/datasource/" + ds.getID() + "/controlendpoint");
        remDHT("/datasource/name/" + ds.getName()); 
        
        if (ds instanceof ControllableDataSource)
            remDHT("/datasource/" + ds.getID() + "/pid");
        
	Collection<Probe> probes = ds.getProbes();

	// skip through all probes
	for (Probe aProbe : probes) {
	    removeProbe(aProbe);
	}
	    
	return this;
    }

    public IMNode removeProbe(Probe aProbe) throws IOException {
	// add probe's ref to its data source
	// found through the ProbeManager
	remDHT("/probe/" + aProbe.getID() + "/datasource");

	// add probe name to DHT
	remDHT("/probe/" + aProbe.getID() + "/name");
	remDHT("/probe/" + aProbe.getID() + "/datarate");
	remDHT("/probe/" + aProbe.getID() + "/on");
	remDHT("/probe/" + aProbe.getID() + "/active");

	// now probe attributes
	Collection<ProbeAttribute> attrs = aProbe.getAttributes();

	remDHT("/probeattribute/" + aProbe.getID() + "/size");
	// skip through all ProbeAttributes
	for (ProbeAttribute attr : attrs) {
	    removeProbeAttribute(aProbe, attr);
	}

	return this;
    }

    public IMNode removeProbeAttribute(Probe aProbe, ProbeAttribute attr)  throws IOException {
	String attrRoot = "/probeattribute/" + aProbe.getID() + "/" +
	    attr.getField() + "/";

	remDHT(attrRoot + "name");
	remDHT(attrRoot + "type");
	remDHT(attrRoot + "units");

	return this;
    }

    
    public IMNode removeDataConsumer(ControllableDataConsumer dc) throws IOException {
	remDHT("/dataconsumer/" + dc.getID() + "/name");
        remDHT("/dataconsumer/" + dc.getID() + "/controlendpoint"); //we also need to remove the control end point
        remDHT("/dataconsumer/name/" + dc.getName()); 
        
        if (dc instanceof DefaultControllableDataConsumer)
            remDHT("/dataconsumer/" + dc.getID() + "/pid");

	// skip through all reporters
	for (ControllableReporter r : dc.getReportersCollection()) {
	    removeReporter((ControllableReporter)r);
	}        
	return this;
    }
    
    
    public IMNode removeReporter(ControllableReporter r) throws IOException {
        remDHT("/reporter/" + r.getId() + "/name");
        remDHT("/reporter/" + r.getId() + "/dataconsumer");
        return this;
    }
    

    /**
     * Lookup DataSource info
     */
    public Object getDataSourceInfo(ID dsID, String info) {
	return getDHT("/datasource/" + dsID + "/" + info);
    }

    /**
     * Lookup probe details.
     */
    public Object getProbeInfo(ID probeID, String info) {
	return getDHT("/probe/" + probeID + "/" + info);
    }

    /**
     * Lookup probe attribute details.
     */
    public Object getProbeAttributeInfo(ID probeID, int field, String info) {
	return getDHT("/probeattribute/" + probeID + "/" + field + "/" + info);
    }

    /**
     * Lookup ControllableDataConsumer info
     */
    public Object getDataConsumerInfo(ID dcID, String info) {
	return getDHT("/dataconsumer/" + dcID + "/" + info);
    }
    
    
    /**
     * Lookup Reporter info
     */
    public Object getReporterInfo(ID reporterID, String info) {
	return getDHT("/reporter/" + reporterID + "/" + info);
    }
    
    
    public boolean containsDataSource(ID dataSourceID, int timeout) {
        try {
            String newKey = "/datasource/" + dataSourceID + "/name";
            return dht.contains(newKey, timeout);
        } 
        catch (IOException ioe) {
            LOGGER.error("ContainsDataSource failed for DS " + dataSourceID + ioe.getMessage());
            return false;
        }
    }
    
    public boolean containsDataConsumer(ID dataConsumerID, int timeout) {
        try {
            String newKey = "/dataconsumer/" + dataConsumerID + "/name";
            return dht.contains(newKey, timeout);
        } 
        catch (IOException ioe) {
            LOGGER.error("ContainsDataConsumer failed for DS " + dataConsumerID + ioe.getMessage());
            return false;
        }
    }
    

    /**
     * Put stuff into DHT.
     */
    public boolean putDHT(String aKey, Serializable aValue) {
	try {
	    LOGGER.info("put " + aKey + " => " + aValue);
	    dht.put(aKey, aValue);
	    return true;
	} catch (IOException ioe) {
	    LOGGER.error("putDHT failed for key: '" + aKey + "' value: '" + aValue + "' " +ioe.getMessage());
	    return false;
	}
    }

    /**
     * Lookup info directly from the DHT.
     * @return the value if found, null otherwise
     */
    public Object getDHT(String aKey) {
	try {
	    Object aValue = dht.get(aKey);
	    LOGGER.debug("get " + aKey +  " => " + aValue);
	    return aValue;
	} catch (IOException | ClassNotFoundException e) {
	    LOGGER.error("getDHT failed for key: '" + aKey + " " + e.getMessage());
	    return null;
	}
    }

    /**
     * Remove info from the DHT.
     * @return boolean
     */
    public boolean remDHT(String aKey) {
	try {
	    dht.remove(aKey);
	    LOGGER.debug("removing " + aKey);
	    return true;
	} catch (IOException ioe) {
	    LOGGER.error("remDHT failed for key: '" + aKey + "' " + ioe.getMessage());
	    return false;
	}
    }
    
    
    public void announce(AbstractAnnounceMessage m) {
        dht.announce(m);
    }

    
    @Override
    public String toString() {
        return dht.toString();
    }
    

    @Override
    public void receivedAnnounceEvent(AbstractAnnounceMessage m) {
        fireEvent(m);
        
    }
    
    
    public void addAnnounceEventListener(AnnounceEventListener l) {
        this.listener=l;
    }
    
    protected void fireEvent(AbstractAnnounceMessage m) {
        listener.receivedAnnounceEvent(m);
    }
    
}
