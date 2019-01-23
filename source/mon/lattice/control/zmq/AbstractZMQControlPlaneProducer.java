/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mon.lattice.control.zmq;

import mon.lattice.control.SynchronousTransmitting;
import mon.lattice.control.ControlPlaneConsumerException;
import mon.lattice.im.delegate.InfoPlaneDelegate;
import mon.lattice.im.delegate.InfoPlaneDelegateInteracter;
import mon.lattice.core.plane.ControllerControlPlane;
import mon.lattice.core.plane.ControlPlaneMessage;
import mon.lattice.distribution.MetaData;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * UDP based request-reply protocol to send control messages to Data Sources
 * connected to the control plane.
 * It also allows listeners to be added and called back when an announce message
 * is received from a Data Source on this plane (useful when the info plane 
 * implementation does not provide that functionality)
 * @author uceeftu
 */
public abstract class AbstractZMQControlPlaneProducer implements 
        ControllerControlPlane, SynchronousTransmitting, InfoPlaneDelegateInteracter  {
    
    ZMQRouter zmqRouter;
    ZMQRequesterPool controlTransmittersPool;
    int maxPoolSize;
    int localControlPort;
    
    InfoPlaneDelegate infoPlaneDelegate;
    
    static Logger LOGGER = LoggerFactory.getLogger("ZMQControlPlaneProducer");
    
    public AbstractZMQControlPlaneProducer(int maxPoolSize, int port) {
        this.localControlPort = port;
        this.zmqRouter = new ZMQRouter(localControlPort);
        this.maxPoolSize = maxPoolSize;
    }
    

    @Override
    public boolean connect() {
	try {
            zmqRouter.bind();
            
            if (controlTransmittersPool == null) {
                // creating a pool for Control Messages transmission
                // 8 seems to match the max size of the threadPool created by the RestConsole
                controlTransmittersPool = new ZMQRequesterPool(this, maxPoolSize, zmqRouter.getContext());
            }       
            return true;
            
	} catch (IOException ioe) {
	    LOGGER.error("Error while connecting " + ioe.getMessage());
	    return false;
	}
    }

    @Override
    public boolean disconnect() {
        try {
            controlTransmittersPool.disconnect();
            zmqRouter.disconnect();
	    return true;
	} catch (IOException ieo) {
	    return false;
	}
    }

    
    @Override
    public boolean announce() {
        // sending announce messages is not expected for a Control Plane Producer
	return false;
    }

    @Override
    public boolean dennounce() {
        // sending deannounce messages is not expected for a Control Plane Producer
	return false;
    }

    @Override
    public abstract Object synchronousTransmit(ControlPlaneMessage dpm, MetaData metaData) throws IOException, ControlPlaneConsumerException;

    
    @Override
    public abstract Object receivedReply(ByteArrayInputStream bis, MetaData metaData, int seqNo) throws IOException;
    
    
    @Override
    public Map getControlEndPoint() {
        throw new UnsupportedOperationException("Abstract UDP Control Plane Producer: getting control endpoint is not supported");
    }
    

    @Override
    public InfoPlaneDelegate getInfoPlaneDelegate() {
        return infoPlaneDelegate;
    }

    @Override
    public void setInfoPlaneDelegate(InfoPlaneDelegate im) {
        this.infoPlaneDelegate = im;
    }
    
}
