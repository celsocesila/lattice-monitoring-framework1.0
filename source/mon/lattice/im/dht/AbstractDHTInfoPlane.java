// AbstractDHTInfoPlane.java
// Author: Stuart Clayman
// Email: sclayman@ee.ucl.ac.uk
// Date: Sept 2009

package mon.lattice.im.dht;

import mon.lattice.core.DataSource;
import mon.lattice.core.Probe;
import mon.lattice.core.ID;
import mon.lattice.core.plane.InfoPlane;
import java.io.Serializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A DHTInfoPlane is an InfoPlane implementation
 * that sends the Information Model data.
 * It is also a DataSourceInteracter so it can, if needed,
 * talk to the DataSource object it gets bound to.
 */
public abstract class AbstractDHTInfoPlane implements InfoPlane  {
    protected AbstractDHTIMNode imNode;
    
    static protected Logger LOGGER = LoggerFactory.getLogger(AbstractDHTInfoPlane.class);
    
    public AbstractDHTInfoPlane() {
    }
    
    /**
     * Connect to a delivery mechanism.
     */
    @Override
    public boolean connect() {
	return imNode.connect();
    }

    /**
     * Disconnect from a delivery mechanism.
     */
    @Override
    public boolean disconnect() {
	return imNode.disconnect();
    }

    @Override
    public String getInfoRootHostname() {
        return imNode.getRemoteHostname();
    }

    
    
    // lookup some info in the InfoPlane
    @Override
    public Object lookupDataSourceInfo(DataSource dataSource, String info) {
	return imNode.getDataSourceInfo(dataSource.getID(), info);
    }

    // lookup some info in the InfoPlane
    @Override
    public Object lookupDataSourceInfo(ID dataSourceID, String info) {
	return imNode.getDataSourceInfo(dataSourceID, info);
    }

    // lookup some info in the InfoPlane
    @Override
    public Object lookupProbeInfo(Probe probe, String info) {
	return imNode.getProbeInfo(probe.getID(), info);
    }

    // lookup some info in the InfoPlane
    @Override
    public Object lookupProbeInfo(ID probeID, String info) {
	return imNode.getProbeInfo(probeID, info);
    }

    // lookup some info in the InfoPlane
    @Override
    public Object lookupProbeAttributeInfo(Probe probe, int field, String info) {
	return imNode.getProbeAttributeInfo(probe.getID(), field, info);
    }

    // lookup some info in the InfoPlane
    @Override
    public Object lookupProbeAttributeInfo(ID probeID, int field, String info) {
	return imNode.getProbeAttributeInfo(probeID, field, info);
    }
    
    @Override
    public Object lookupDataConsumerInfo(ID dataConsumerID, String info) {
        return imNode.getDataConsumerInfo(dataConsumerID, info);
    }
    
    @Override
    public Object lookupReporterInfo(ID reporterID, String info) {
        return imNode.getReporterInfo(reporterID, info);
    }
    

    /**
     * Put a value in the InfoPlane.
     */
    @Override
    public boolean putInfo(String key, Serializable value) {
	return imNode.putDHT(key, value);
    }

    /**
     * Get a value from the InfoPlane.
     */
    @Override
    public Object getInfo(String key) {
	return imNode.getDHT(key);
    }

    /**
     * Remove a value from the InfoPlane.
     */
    @Override
    public boolean removeInfo(String key) {
	return imNode.remDHT(key);
    }
    
    @Override
    public String toString() {
        return imNode.toString();
    }

    
}