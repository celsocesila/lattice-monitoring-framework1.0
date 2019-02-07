// TomP2PDHTInfoPlaneConsumer.java
// Author: Stuart Clayman
// Email: sclayman@ee.ucl.ac.uk
// Date: Sept 2009

package mon.lattice.im.dht;

import mon.lattice.im.dht.tomp2p.*;
import mon.lattice.im.delegate.ControlInformationManager;
import mon.lattice.im.delegate.InfoPlaneDelegate;
import mon.lattice.core.DataSource;
import mon.lattice.core.ID;
import mon.lattice.core.Probe;
import mon.lattice.core.ProbeAttribute;
import mon.lattice.core.Reporter;
import mon.lattice.core.ControllableDataConsumer;
import mon.lattice.core.plane.AbstractAnnounceMessage;
import mon.lattice.core.plane.AnnounceEventListener;
import mon.lattice.im.delegate.InfoPlaneDelegateInteracter;

/**
 * A TomP2PDHTInfoPlaneConsumer is an InfoPlane implementation
 that collects data from the Information Model data.
 */
public abstract class AbstractDHTRootInfoPlane extends AbstractDHTInfoPlane implements InfoPlaneDelegateInteracter, AnnounceEventListener  {
    private InfoPlaneDelegate infoPlaneDelegate;
    
    AnnounceEventListener listener;
    
    // The hostname of the DHT root.
    protected String rootHost;

    // The port to connect to
    protected int rootPort;

    // The local port
    protected int port;

    /**
     * Constructor for subclasses.
     */
    public AbstractDHTRootInfoPlane() {
        setInfoPlaneDelegate(new ControlInformationManager(this));
    }



   /**
     * Announce that the plane is up and running
     */
    public boolean announce() {
	return true;
    }

    /**
     * Un-sendMessage that the plane is up and running
     */
    public boolean dennounce() {
	return true;
    }

    /**
     * Consumer can never add a DataSource.
     * Return false
     */
    public boolean addDataSourceInfo(DataSource ds) {
	return false;
    }

    /**
     * Consumer can never add a Probe.
     * Return false
     */
    public boolean addProbeInfo(Probe p) {
	return false;
    }

    /**
     * Consumer can never add a ProbeAttribute to a ProbeAttribute
     */
    public boolean addProbeAttributeInfo(Probe p, ProbeAttribute pa) {
	return false;
    }

    /**
     * Consumer can never remove a DataSource
     */
    public boolean modifyDataSourceInfo(DataSource ds) {
	return false;
    }

    /**
     * Consumer can never remove a Probe
     */
    public boolean modifyProbeInfo(Probe p) {
	return false;
    }

    /**
     * Consumer can never remove a ProbeAttribute from a Probe
     */
    public boolean modifyProbeAttributeInfo(Probe p, ProbeAttribute pa) {
	    return false;
    }

    /**
     * Consumer can never remove a DataSource
     */
    public boolean removeDataSourceInfo(DataSource ds) {
	return false;
    }

    /**
     * Consumer can never remove a Probe
     */
    public boolean removeProbeInfo(Probe p) {
	return false;
    }

    /**
     * Consumer can never remove a ProbeAttribute from a Probe
     */
    public boolean removeProbeAttributeInfo(Probe p, ProbeAttribute pa) {
	    return false;
    }

    @Override
    public boolean addDataConsumerInfo(ControllableDataConsumer dc) {
        return false;
    }

    @Override
    public boolean addReporterInfo(Reporter r) {
        return false;
    }

    @Override
    public boolean removeDataConsumerInfo(ControllableDataConsumer dc) {
        return false;
    }

    @Override
    public boolean removeReporterInfo(Reporter r) {
        return false;
    }
    
    @Override
    public boolean containsDataSource(ID dataSourceID, int timeout) {
        return imNode.containsDataSource(dataSourceID, timeout); 
    }
    
    @Override
    public boolean containsDataConsumer(ID dataConsumerID, int timeout) {
        return imNode.containsDataConsumer(dataConsumerID, timeout);
    }
    
    
    @Override
    public void receivedAnnounceEvent(AbstractAnnounceMessage m) {
        infoPlaneDelegate.receivedAnnounceEvent(m);
    }
    
    @Override
    public void setInfoPlaneDelegate(InfoPlaneDelegate im) {
        this.infoPlaneDelegate = im;
    }

    @Override
    public InfoPlaneDelegate getInfoPlaneDelegate() {
        return this.infoPlaneDelegate;
    }
    
    public void addAnnounceEventListener(AnnounceEventListener l) {
        this.listener=l;
    }
    
    protected void fireEvent(AbstractAnnounceMessage m) {
        listener.receivedAnnounceEvent(m);
    }

    @Override
    public Object lookupProbesOnDS(ID dataSourceID) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}