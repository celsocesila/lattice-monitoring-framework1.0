// DHTDataSourceInfoPlane.java
// Author: Stuart Clayman
// Email: sclayman@ee.ucl.ac.uk
// Date: Sept 2009

package mon.lattice.im.dht;

import mon.lattice.im.dht.tomp2p.IMNode;
import mon.lattice.core.ControllableReporter;
import mon.lattice.core.DataConsumerInteracter;
import mon.lattice.core.DataSource;
import mon.lattice.core.ID;
import mon.lattice.core.Probe;
import mon.lattice.core.ProbeAttribute;
import mon.lattice.core.Reporter;
import mon.lattice.core.plane.InfoPlane;

import java.io.IOException;
import mon.lattice.core.ControllableDataConsumer;
import mon.lattice.core.EntityType;
import mon.lattice.core.plane.AbstractAnnounceMessage;
import mon.lattice.core.plane.AnnounceMessage;
import mon.lattice.core.plane.DeannounceMessage;

/**
 * A DHTDataConsumerInfoPlane is an InfoPlane implementation
 that sends the Information Model data.
 */

public class DHTDataConsumerInfoPlane extends AbstractDHTInfoPlane implements InfoPlane, DataConsumerInteracter {
    ControllableDataConsumer dataConsumer;
    // The hostname of the DHT root.
    String rootHost;

    // The port to connect to
    int rootPort;

    // The local port
    int port;

    /**
     * Construct a DHTInfoPlane.
     * Connect to the DHT root at hostname on port,
     * and start here on localPort.
     */
    public DHTDataConsumerInfoPlane(String remoteHostname, int remotePort, int localPort) {
	rootHost = remoteHostname;
	rootPort = remotePort;
	port = localPort;

	imNode = new IMNode(localPort, remoteHostname, remotePort);
    }
    
    public DHTDataConsumerInfoPlane(int remotePort, int localPort) {
	rootPort = remotePort;
	port = localPort;

	imNode = new IMNode(localPort, remotePort);
    }
    

    /**
     * Connect to a delivery mechansim.
     * In a DHTDataSourceInfoPlane we call announce.
     */
    public boolean connect() {
        /*
	if (super.connect()) {
	    return announce();
	} else {
	    return false;
	}*/
        return super.connect();
    }

    /**
     * Disconnect from a delivery mechanism.
     * In a DHTDataSourceInfoPlane we call deannounce.
     */
    public boolean disconnect() {
	/*if (super.disconnect()) {
	    return dennounce();
	} else {
	    return false;
	}*/
        return super.disconnect();
    }


    /**
     * Announce that the plane is up and running
     */
    public boolean announce() {
	addDataConsumerInfo(dataConsumer);
        return true;
    }

    /**
     * Un-announce that the plane is up and running
     */
    public boolean dennounce() {
        try {
	    imNode.removeDataConsumer(dataConsumer);
            
            imNode.announce(new DeannounceMessage(dataConsumer.getID(), EntityType.DATACONSUMER));
	    LOGGER.info("just deannounced this Data Consumer " + dataConsumer.getID());
	    return true;
	} catch (IOException ioe) {
	    return false;
	}        
    }

    @Override
    public boolean addDataConsumerInfo(ControllableDataConsumer dc) {
        try {
	    imNode.addDataConsumer(dc);
            imNode.addDataConsumerInfo(dc);
            
            imNode.announce(new AnnounceMessage(dataConsumer.getID(), EntityType.DATACONSUMER));
	    LOGGER.info("just announced this Data Consumer " + dc.getID());
	    return true;
	} catch (IOException ioe) {
	    return false;
	}
    }

    @Override
    public boolean addReporterInfo(Reporter r) {
        try {
	    imNode.addReporter((ControllableReporter)r);
	    LOGGER.info("just added reporter " + ((ControllableReporter)r).getName());
	    return true;
	} catch (IOException ioe) {
	    return false;
	}
    }

    @Override
    public boolean removeDataConsumerInfo(ControllableDataConsumer dc) {
        try {
	    imNode.removeDataConsumer(dc);
	    LOGGER.info("just removed Data Consumer " + dc);
	    return true;
	} catch (IOException ioe) {
	    return false;
	}
    }

    @Override
    public boolean removeReporterInfo(Reporter r) {
        try {
	    imNode.addReporter((ControllableReporter)r);
	    LOGGER.info("just removed reporter " + r);
	    return true;
	} catch (IOException ioe) {
	    return false;
	}
    }
    
    /* maybe we should consider refactoring the interface InfoService
       the below method cannot be executed by a Data Consumer
    */

    @Override
    public boolean addDataSourceInfo(DataSource ds) {
        return false;
    }

    @Override
    public boolean addProbeInfo(Probe p) {
        return false;
    }

    @Override
    public boolean addProbeAttributeInfo(Probe p, ProbeAttribute pa) {
        return false;
    }

    @Override
    public boolean modifyDataSourceInfo(DataSource ds) {
        return false;
    }

    @Override
    public boolean modifyProbeInfo(Probe p) {
        return false;
    }

    @Override
    public boolean modifyProbeAttributeInfo(Probe p, ProbeAttribute pa) {
        return false;
    }

    @Override
    public boolean removeDataSourceInfo(DataSource ds) {
        return false;
    }

    @Override
    public boolean removeProbeInfo(Probe p) {
        return false;
    }

    @Override
    public boolean removeProbeAttributeInfo(Probe p, ProbeAttribute pa) {
        return false;
    }

    @Override
    public boolean containsDataSource(ID dataSourceID, int timeOut) {
        throw new UnsupportedOperationException("Not supported on a Data Consumer"); 
    }

    @Override
    public boolean containsDataConsumer(ID dataConsumerID, int timeOut) {
        throw new UnsupportedOperationException("Not supported on a Data Consumer");
    } 

    @Override
    public ControllableDataConsumer getDataConsumer() {
        return this.dataConsumer;
    }

    @Override
    public ControllableDataConsumer setDataConsumer(ControllableDataConsumer dc) {
        this.dataConsumer = dc;
        return dataConsumer;
    }

    @Override
    public Object lookupProbesOnDS(ID dataSourceID) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }


 


}