/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mon.lattice.im.dht;

import java.io.IOException;
import mon.lattice.core.ControllableDataConsumer;
import mon.lattice.core.DataSource;
import mon.lattice.core.DataSourceDelegate;
import mon.lattice.core.DataSourceDelegateInteracter;
import mon.lattice.core.EntityType;
import mon.lattice.core.ID;
import mon.lattice.core.Probe;
import mon.lattice.core.ProbeAttribute;
import mon.lattice.core.Reporter;
import mon.lattice.core.plane.AnnounceMessage;
import mon.lattice.core.plane.DeannounceMessage;
import static mon.lattice.im.dht.AbstractDHTInfoPlane.LOGGER;

/**
 *
 * @author uceeftu
 */
public abstract class AbstractDHTDataSourceInfoPlane extends AbstractDHTInfoPlane implements DataSourceDelegateInteracter  {
    
    // DataSourceDelegate
    protected DataSourceDelegate dataSourceDelegate;

    /**
     * Announce that the plane is up and running
     */
    @Override
    public boolean announce() {
	try {
	    DataSource dataSource = dataSourceDelegate.getDataSource();
	    imNode.addDataSource(dataSource);
            
            // adding additional DS information
            addDataSourceInfo(dataSource);
            
            imNode.sendMessage(new AnnounceMessage(dataSource.getID(), EntityType.DATASOURCE));
	    LOGGER.info("just announced this Data Source " + dataSource.getID());
	    return true;
	} catch (IOException ioe) {
	    return false;
	}
    }

    /**
     * Un-sendMessage that the plane is up and running
     */
    public boolean dennounce() {
        try {
            DataSource dataSource = dataSourceDelegate.getDataSource();
            imNode.removeDataSource(dataSource);
            
            imNode.sendMessage(new DeannounceMessage(dataSource.getID(), EntityType.DATASOURCE));
            LOGGER.info("just deannounced this Data Source " + dataSource.getID());
            return true;
        } catch (IOException ioe) {
            return false;
        }
    }


    /**
     * Get the DataSourceDelegate this is a delegate for.
     */
    public DataSourceDelegate getDataSourceDelegate() {
	return dataSourceDelegate;
    }

    /**
     * Set the DataSourceDelegate this is a delegate for.
     */
    public DataSourceDelegate setDataSourceDelegate(DataSourceDelegate ds) {
	//System.out.println("DHTInfoPlane: setDataSource: " + ds);
	dataSourceDelegate = ds;
	return ds;
    }

    /**
     * Add a DataSource
     */
    public boolean addDataSourceInfo(DataSource ds) {
	try {
	    // adds further information for the DS
            imNode.addDataSourceInfo(ds);
	    return true;
	} catch (IOException ioe) {
	    return false;
	}
    }

    /**
     * Add a Probe
     */
    public boolean addProbeInfo(Probe p) {
	try {
	    imNode.addProbe(p);

	    LOGGER.info("just added Probe " + p.getClass());
            LOGGER.debug(p.toString());
	    return true;
	} catch (IOException ioe) {
	    return false;
	}
    }



    /**
     * Add a ProbeAttribute to a ProbeAttribute
     */
    public boolean addProbeAttributeInfo(Probe p, ProbeAttribute pa) {
	try {
	    imNode.addProbeAttribute(p, pa);

	    LOGGER.debug("just added ProbeAttribute " + p + "." + pa);
	    return true;
	} catch (IOException ioe) {
	    return false;
	}
    }

    /**
     * Modify a DataSource
     */
    public boolean modifyDataSourceInfo(DataSource ds) {
	try {
	    imNode.modifyDataSource(ds);

	    LOGGER.info("just modified DataSource " + ds);
	    return true;
	} catch (IOException ioe) {
	    return false;
	}
    }

    /**
     * Modify a Probe
     */
    public boolean modifyProbeInfo(Probe p) {
	try {
	    imNode.modifyProbe(p);

	    LOGGER.info("just modified Probe " + p.getClass());
	    return true;
	} catch (IOException ioe) {
	    return false;
	}
    }

    /**
     * Modify a ProbeAttribute from a Probe
     */
    public boolean modifyProbeAttributeInfo(Probe p, ProbeAttribute pa) {
	try {
	    imNode.modifyProbeAttribute(p, pa);

	    LOGGER.debug("just modified ProbeAttribute " + p + "." + pa);
	    return true;
	} catch (IOException ioe) {
	    return false;
	}
    }


    /**
     * Remove a DataSource
     */
    public boolean removeDataSourceInfo(DataSource ds) {
	try {
	    imNode.removeDataSource(ds);

	    LOGGER.info("just removed Data Source " + ds);
	    return true;
	} catch (IOException ioe) {
	    return false;
	}
    }

    /**
     * Remove a Probe
     */
    public boolean removeProbeInfo(Probe p) {
	try {
	    imNode.removeProbe(p);

	    LOGGER.info("just removed Probe " + p.getClass());
	    return true;
	} catch (IOException ioe) {
	    return false;
	}
    }

    /**
     * Remove a ProbeAttribute from a Probe
     */
    public boolean removeProbeAttributeInfo(Probe p, ProbeAttribute pa) {
	try {
	    imNode.removeProbeAttribute(p, pa);

	    LOGGER.debug("just removed ProbeAttribute " + p + "." + pa);
	    return true;
	} catch (IOException ioe) {
	    return false;
	}
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
    public boolean containsDataSource(ID dataSourceID, int timeOut) {
        throw new UnsupportedOperationException("Not supported on a Data Source");
    }

    @Override
    public boolean containsDataConsumer(ID dataConsumerID, int timeOut) {
        throw new UnsupportedOperationException("Not supported on a Data Source");
    }

    @Override
    public Object lookupProbesOnDS(ID dataSourceID) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    
    
}
