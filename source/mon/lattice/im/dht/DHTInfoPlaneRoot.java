// DHTInfoPlaneRoot.java
// Author: Stuart Clayman
// Email: sclayman@ee.ucl.ac.uk
// Date: Sept 2009

package mon.lattice.im.dht;

import mon.lattice.im.dht.tomp2p.IMNode;
import mon.lattice.core.plane.InfoPlane;
import java.io.IOException;
import java.net.InetAddress;


/**
 * A DHTInfoPlaneRoot is an InfoPlane implementation
 * that acts as a ROOT for the Information Model data.
 * There needs to be one root for a DHT.
 * The other nodes connect to it.
 */
public class DHTInfoPlaneRoot extends DHTInfoPlaneConsumer implements InfoPlane  {
    /**
     * Construct a DHTInfoPlane.
     * Connect to the DHT root at hostname on port,
     */
    public DHTInfoPlaneRoot(String localHostname, int localPort) {
	rootHost = localHostname;
	rootPort = localPort;

        // from the super class
	imNode = new IMNode(localPort, localHostname, localPort);
        imNode.addAnnounceEventListener(this);
    } 
    
    public DHTInfoPlaneRoot(int localPort) {
	rootPort = localPort;

        // from the super class
	imNode = new IMNode(localPort);
        rootHost = imNode.getRootHostname();
        
        imNode.addAnnounceEventListener(this);
    } 
}