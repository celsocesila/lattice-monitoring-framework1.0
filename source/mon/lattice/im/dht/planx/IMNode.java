// IMNode.java
// Author: Stuart Clayman
// Email: sclayman@ee.ucl.ac.uk
// Date: Oct 2008

package mon.lattice.im.dht.planx;

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
import java.math.BigInteger;
import mon.lattice.core.ControllableDataConsumer;
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
 * @deprecated, use eu.fivegex.monitoring.im.dht.tomp2p.IMNode
 **/

public class IMNode {
    // The actual DHT
    DistributedHashTable dht = null;

    // the local port
    int localPort = 0;

    // the remote host
    String remoteHost;

    // the remote port
    int remotePort = 0;

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
     * Connect to the DHT peers.
     */
    public boolean connect() {
	try {
	    // only connect if we don't already have a DHT
	    if (dht == null) {
		dht = new DistributedHashTable(localPort);
		dht.connect(remoteHost, remotePort);

		System.out.println("IMNode: connect: " + localPort + " to " + remoteHost + "/" + remotePort);

		return true;
	    } else {
		return true;
	    }
	} catch (IOException ioe) {
	    System.err.println("IMNode: connect failed: " + ioe);
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

    public IMNode addDataConsumer(ControllableDataConsumer dc) throws IOException {
        putDHT("/dataconsumer/" + dc.getID() + "/name", dc.getName());   
        
        // this might be slightly different approach from other entries as we serialise a whole JSON
        // it might be modified to use separate entries
        
        JSONObject controlEndPoint = new JSONObject(dc.getControlPlane().getControlEndPoint());
        putDHT("/dataconsumer/" + dc.getID() + "/controlEndPoint", controlEndPoint.toString());
        
        //Object [] reporters = dc.getReporters();
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
        
        JSONObject controlEndPoint = new JSONObject(ds.getControlPlane().getControlEndPoint());
        putDHT("/datasource/" + ds.getID() + "/controlEndPoint", controlEndPoint.toString());
        
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
        remDHT("/datasource/" + ds.getID() + "/controlEndPoint");
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
	DataSource ds = (DataSource)aProbe.getProbeManager();
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
        remDHT("/dataconsumer/" + dc.getID() + "/controlEndPoint"); //we also need to remove the control end point
        remDHT("/dataconsumer/name/" + dc.getName()); 
        
        if (dc instanceof DefaultControllableDataConsumer)
            remDHT("/dataconsumer/" + dc.getID() + "/pid"); 
        
        
	//Object[] reporters = dc.getReporters();

	// skip through all reporters
	for (ControllableReporter r: dc.getReportersCollection()) {
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
    
    
    public boolean containsDataSource(ID dataSourceID) {
        try {
            BigInteger newKey = keyToBigInteger("/datasource/" + dataSourceID + "/name");
            return dht.contains(newKey);
        } 
        catch (IOException ioe) {
            System.err.println("IMNode: containsDataSource failed for DS " + dataSourceID);
            return false;
        }
    }
    
    public boolean containsDataConsumer(ID dataConsumerID) {
        try {
            BigInteger newKey = keyToBigInteger("/dataconsumer/" + dataConsumerID + "/name");
            return dht.contains(newKey);
        } 
        catch (IOException ioe) {
            System.err.println("IMNode: containsDataConsumer failed for DS " + dataConsumerID);
            return false;
        }
    }
    


    /**
     * Put stuff into DHT.
     */
    public boolean putDHT(String aKey, Serializable aValue) {
	try {
	    BigInteger newKey = keyToBigInteger(aKey);
	    //System.out.println("IMNode: put " + aKey + " K(" + newKey + ") => " + aValue);
	    dht.put(newKey, aValue);
	    return true;
	} catch (IOException ioe) {
	    System.err.println("IMNode: putDHT failed for key: '" + aKey + "' value: '" + aValue + "'");
	    return false;
	}
    }

    /**
     * Lookup info directly from the DHT.
     * @return the value if found, null otherwise
     */
    public Object getDHT(String aKey) {
	try {
	    BigInteger newKey = keyToBigInteger(aKey);
	    Object aValue = dht.get(newKey);
	    //System.out.println("IMNode: get " + aKey + " = " + newKey + " => " + aValue);
	    return aValue;
	} catch (IOException ioe) {
	    System.err.println("IMNode: getDHT failed for key: '" + aKey + "'");
	    ioe.printStackTrace();
	    return null;
	}
    }

    /**
     * Remove info from the DHT.
     * @return boolean
     */
    public boolean remDHT(String aKey) {
	try {
	    BigInteger newKey = keyToBigInteger(aKey);
	    dht.remove(newKey);
	    //System.out.println("IMNode: get " + aKey + " = " + newKey + " => " + aValue);
	    return true;
	} catch (IOException ioe) {
	    System.err.println("IMNode: remDHT failed for key: '" + aKey + "'");
	    return false;
	}
    }

    /**
     * Convert a key like /a/b/c/d into a fixed size big integer.
     */
    private BigInteger keyToBigInteger(String aKey) {
	// hash codes are signed ints
	int i = aKey.hashCode();
	// convert this into an unsigned long
	long l = 0xffffffffL & i;
	// create the BigInteger
	BigInteger result = BigInteger.valueOf(l);

	return result;
    }
    
    
    public String toString() {
        return dht.toString();
    }
    

}
