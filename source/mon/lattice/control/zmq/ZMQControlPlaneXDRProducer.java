/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mon.lattice.control.zmq;

import mon.lattice.control.ControlPlaneConsumerException;
import mon.lattice.control.ControlServiceException;
import mon.lattice.im.delegate.SocketControlEndPointMetaData;
import mon.lattice.im.delegate.DCNotFoundException;
import mon.lattice.im.delegate.DSNotFoundException;
import mon.lattice.im.delegate.ProbeNotFoundException;
import mon.lattice.im.delegate.ReporterNotFoundException;
import mon.lattice.im.delegate.ZMQControlEndPointMetaData;
import mon.lattice.core.ID;
import mon.lattice.core.Measurement;
import mon.lattice.core.Rational;
import mon.lattice.core.Timestamp;
import mon.lattice.core.plane.ControlPlaneMessage;
import mon.lattice.core.plane.ControlOperation;
import mon.lattice.core.plane.ControllerControlPlane;
import mon.lattice.core.plane.MessageType;
import mon.lattice.distribution.MetaData;
import mon.lattice.xdr.XDRDataInputStream;
import mon.lattice.xdr.XDRDataOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;


public class ZMQControlPlaneXDRProducer extends AbstractZMQControlPlaneProducer implements ControllerControlPlane {
    
    /**
     * Creates a Producer without announce/deannounce management capabilities
     * @param maxPoolSize is the size of the UDP Transmitters pool
     */
    public ZMQControlPlaneXDRProducer(int maxPoolSize, int port) {
        super(maxPoolSize, port);
    }
    

    @Override
    public Object synchronousTransmit(ControlPlaneMessage cpMessage, MetaData metadata) throws IOException, ControlPlaneConsumerException {
        Object result=null;
        
        // convert the object to a byte []
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutput dataOutput = new XDRDataOutputStream(byteStream);

        try {
            // write type
            dataOutput.writeInt(cpMessage.getType().getValue());            

            // write object
            dataOutput.writeUTF(cpMessage.getControlOperation().getValue());            

            // writing message seqNo
            int seqNo = cpMessage.getSequenceNumber();
            dataOutput.writeInt(seqNo);

            // convert args to byte          
            dataOutput.write(cpMessage.getMethodArgsAsByte());

            LOGGER.debug("--------- Sending Control Message with seqNo: " + seqNo + " ----------");

            // getting a Transmitter from the Pool
            ZMQRequester connection = controlTransmittersPool.getConnection();
            result = connection.transmitAndWaitReply(byteStream, (ZMQControlMetaData)metadata, seqNo);

            // putting the Transmitter back to the Pool
            controlTransmittersPool.releaseConnection(connection);
        } catch (InterruptedException ex) {
            LOGGER.info("interrupted " + ex.getMessage());
        }
        
        if (result instanceof ControlPlaneConsumerException) {
            throw ((ControlPlaneConsumerException) result);
        }
        
    return result;    
    }

    // called when a control reply message is received
    @Override
    public Object receivedReply(ByteArrayInputStream bis, MetaData metaData, int seqNo) throws IOException {
        Object result=null;
        
        DataInput dataIn = new XDRDataInputStream(bis);
        
        // check message type
        int type = dataIn.readInt();            
        MessageType mType = MessageType.lookup(type);

        if (mType == null) {
            throw new IOException("Message type is null");
        }

        else if (mType == MessageType.CONTROL_REPLY) {
                LOGGER.debug("-------- Control Reply Message Received ---------");
                LOGGER.debug("From: " + metaData);

                int replyMessageSeqNo = dataIn.readInt();

                String ctrlOperation = dataIn.readUTF();
                ControlOperation ctrlOperationName = ControlOperation.lookup(ctrlOperation);

                byte [] args = new byte[8192];
                dataIn.readFully(args);
                
                try {
                    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(args));
                    result = (Object) ois.readObject();
                    ois.close();
                } catch (ClassNotFoundException e) {
                    throw new IOException(e.getMessage());
                  }

                if (replyMessageSeqNo == seqNo)
                    if (result instanceof Exception )
                        LOGGER.error("Exception received as reply for request with seqNo: " + replyMessageSeqNo + " - Operation: " + ctrlOperationName + " - Result: " + result.toString());
                    else
                        LOGGER.info("Received reply for request with seqNo: " + replyMessageSeqNo + " - Operation: " + ctrlOperationName + " - Result: " + result.toString());
                else
                    // we should not likely arrive here
                    throw new IOException("Message Sequence number mismatch! " + replyMessageSeqNo + " not equal to " + seqNo);
        }
        
        return result;
    }

    
    @Override
    public boolean transmitted(int id) {
        LOGGER.info("just transmitted Control Message with seqNo: " + id);
        return true;
    }
    
    
    
   /* The methods below may be moved to the abstract super class as they are not XDR related
    * DS Control Service methods 
    */

    @Override
    public ID loadProbe(ID dataSourceID, String probeClassName, Object ... probeArgs) throws ControlServiceException {
        List<Object> args = new ArrayList();
        args.add(dataSourceID);
        args.add(probeClassName);
        args.add(probeArgs);
        
        ID probeID = null;
        
        ControlPlaneMessage m=new ControlPlaneMessage(ControlOperation.LOAD_PROBE, args);
        
        try {
            ZMQControlEndPointMetaData dstAddr = (ZMQControlEndPointMetaData)infoPlaneDelegate.getDSAddressFromID(dataSourceID);
            
            MetaData mData;
            if (dstAddr.getType().equals("zmq")) {
                mData = new ZMQControlMetaData(dataSourceID.toString());
                //we return the ID of the new created probe as result
                probeID = (ID) synchronousTransmit(m, mData);
            }  
        } catch (IOException | DSNotFoundException | ControlPlaneConsumerException ex) {
            LOGGER.error("Error while performing load probe command " + ex.getMessage());
            throw new ControlServiceException(ex);
          }
        return probeID;
    }

    @Override
    public boolean unloadProbe(ID probeID) throws ControlServiceException {
        List<Object> args = new ArrayList();
        args.add(probeID);
        Boolean result = false;
        
        ControlPlaneMessage m=new ControlPlaneMessage(ControlOperation.UNLOAD_PROBE, args);
        try {
            ZMQControlEndPointMetaData dstAddr = (ZMQControlEndPointMetaData)infoPlaneDelegate.getDSAddressFromProbeID(probeID);
            
            MetaData mData = new ZMQControlMetaData(dstAddr.getId().toString());
            result = (Boolean) synchronousTransmit(m, mData);
            
        } catch (IOException | DSNotFoundException | ProbeNotFoundException | ControlPlaneConsumerException ex) {
            LOGGER.error("Error while performing unload probe command " + ex.getMessage());
            throw new ControlServiceException(ex);
          }
        
        return result;
    }
    
    @Override
    public String getProbeName(ID probeID) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean setProbeName(ID probeID, String name) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean setProbeServiceID(ID probeID, ID id) throws ControlServiceException {
        List<Object> args = new ArrayList();
        args.add(probeID);
        args.add(id);
        Boolean result = false;
        
        ControlPlaneMessage m=new ControlPlaneMessage(ControlOperation.SET_PROBE_SERVICE_ID, args);
        try {
            ZMQControlEndPointMetaData dstAddr = (ZMQControlEndPointMetaData)infoPlaneDelegate.getDSAddressFromProbeID(probeID);
            
            MetaData mData = new ZMQControlMetaData(dstAddr.getId().toString());
            result = (Boolean) synchronousTransmit(m, mData);
            
        } catch (IOException | DSNotFoundException | ProbeNotFoundException | ControlPlaneConsumerException ex) {
            LOGGER.error("Error while performing set probe service ID command " + ex.getMessage());
            throw new ControlServiceException(ex);
          }
        
        return result;
        
    }
    
    
    @Override
    public ID getProbeServiceID(ID probeID) throws ControlServiceException {
        List<Object> args = new ArrayList();
        args.add(probeID);
        ID result;
        
        ControlPlaneMessage m=new ControlPlaneMessage(ControlOperation.GET_PROBE_SERVICE_ID, args);
        
        try {
            ZMQControlEndPointMetaData dstAddr = (ZMQControlEndPointMetaData)infoPlaneDelegate.getDSAddressFromProbeID(probeID);
            
            MetaData mData = new ZMQControlMetaData(dstAddr.getId().toString());
            result = (ID) synchronousTransmit(m, mData);
        } 
          catch (IOException | DSNotFoundException | ProbeNotFoundException | ControlPlaneConsumerException ex) {
            LOGGER.error("Error while performing get probe Service ID command " + ex.getMessage());
            throw new ControlServiceException(ex);
          }
        
        return result;
        
    }
    

    @Override
    public ID getProbeGroupID(ID probeID) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean setProbeGroupID(ID probeID, ID id) throws ControlServiceException {
        List<Object> args = new ArrayList();
        args.add(probeID);
        args.add(id);
        Boolean result = false;
        
        ControlPlaneMessage m=new ControlPlaneMessage(ControlOperation.SET_PROBE_GROUP_ID, args);
        try {
            ZMQControlEndPointMetaData dstAddr = (ZMQControlEndPointMetaData)infoPlaneDelegate.getDSAddressFromProbeID(probeID);
            
            MetaData mData = new ZMQControlMetaData(dstAddr.getId().toString());
            result = (Boolean) synchronousTransmit(m, mData);
           
        } catch (IOException | DSNotFoundException | ProbeNotFoundException | ControlPlaneConsumerException ex) {
            LOGGER.error("Error while performing set probe group ID command " + ex.getMessage());
            throw new ControlServiceException(ex);
          }
        
        return result;    
    }
    
    @Override
    public Rational getProbeDataRate(ID probeID) throws ControlServiceException {
        List<Object> args = new ArrayList();
        args.add(probeID);
        Rational result;
        
        ControlPlaneMessage m=new ControlPlaneMessage(ControlOperation.GET_PROBE_DATA_RATE, args);
        
        try {
            ZMQControlEndPointMetaData dstAddr = (ZMQControlEndPointMetaData)infoPlaneDelegate.getDSAddressFromProbeID(probeID);
            
            MetaData mData = new ZMQControlMetaData(dstAddr.getId().toString());
            result = (Rational) synchronousTransmit(m, mData);
        } 
          catch (IOException | DSNotFoundException | ProbeNotFoundException | ControlPlaneConsumerException ex) {
            LOGGER.error("Error while performing get probe data rate  command " + ex.getMessage());
            throw new ControlServiceException(ex);
          }
        
        return result;
        
    }

    @Override
    public boolean setProbeDataRate(ID probeID, Rational dataRate) throws ControlServiceException {
        List<Object> args = new ArrayList();
        args.add(probeID);
        args.add(dataRate);
        Boolean result = false;

        ControlPlaneMessage m=new ControlPlaneMessage(ControlOperation.SET_PROBE_DATA_RATE, args);
        
        try {
            ZMQControlEndPointMetaData dstAddr = (ZMQControlEndPointMetaData)infoPlaneDelegate.getDSAddressFromProbeID(probeID);
            
            MetaData mData = new ZMQControlMetaData(dstAddr.getId().toString());
            result = (Boolean) synchronousTransmit(m, mData);
        } 
          catch (IOException | DSNotFoundException | ProbeNotFoundException | ControlPlaneConsumerException ex) {
            LOGGER.error("Error while performing set probe data rate command " + ex.getMessage());
            throw new ControlServiceException(ex);
          }
        
        return result;
    }

    @Override
    public Measurement getProbeLastMeasurement(ID probeID) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Timestamp getProbeLastMeasurementCollection(ID probeID) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean turnOnProbe(ID probeID) throws ControlServiceException {
        List<Object> args = new ArrayList();
        args.add(probeID);
        Boolean result = false;
        
        ControlPlaneMessage m=new ControlPlaneMessage(ControlOperation.TURN_ON_PROBE, args);
        try {
            ZMQControlEndPointMetaData dstAddr = (ZMQControlEndPointMetaData)infoPlaneDelegate.getDSAddressFromProbeID(probeID);
            
            MetaData mData = new ZMQControlMetaData(dstAddr.getId().toString());
            result = (Boolean) synchronousTransmit(m, mData);
            
            return result;
            
        } catch (IOException | DSNotFoundException | ProbeNotFoundException | ControlPlaneConsumerException ex) {
            LOGGER.error("Error while performing turn on probe command " + ex.getMessage());
            throw new ControlServiceException(ex);
          }
        }

    @Override
    public boolean turnOffProbe(ID probeID) throws ControlServiceException {
        List<Object> args = new ArrayList();
        args.add(probeID);
        Boolean result = false;
        
        ControlPlaneMessage m=new ControlPlaneMessage(ControlOperation.TURN_OFF_PROBE, args);
        try {
            ZMQControlEndPointMetaData dstAddr = (ZMQControlEndPointMetaData)infoPlaneDelegate.getDSAddressFromProbeID(probeID);
            
            MetaData mData = new ZMQControlMetaData(dstAddr.getId().toString());
            result = (Boolean) synchronousTransmit(m, mData);
            
            return result;
        } catch (IOException | DSNotFoundException | ProbeNotFoundException | ControlPlaneConsumerException ex) {
            LOGGER.error("Error while performing turn off probe command " + ex.getMessage());
            throw new ControlServiceException(ex);
          }
        }
    

    @Override
    public boolean isProbeOn(ID probeID) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean activateProbe(ID probeID) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean deactivateProbe(ID probeID) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isProbeActive(ID probeID) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getDataSourceInfo(ID id) throws ControlServiceException {
        List<Object> args = new ArrayList();
        args.add(id);
        
        String name = null;
        
        ControlPlaneMessage m=new ControlPlaneMessage(ControlOperation.GET_DS_NAME, args);
        
        try {
            ZMQControlEndPointMetaData dstAddr = (ZMQControlEndPointMetaData)infoPlaneDelegate.getDSAddressFromID(id);
            
            MetaData mData = new ZMQControlMetaData(dstAddr.getId().toString());
            name = (String) synchronousTransmit(m, mData);
            
            return name;
            
        } catch (IOException | DSNotFoundException | ControlPlaneConsumerException ex) {
            LOGGER.error("Error while performing getDataSourceName command " + ex.getMessage());
            throw new ControlServiceException(ex);
          }
    }

    @Override
    public boolean setDataSourceName(String name) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    
    /* DC Control Service methods */
    
    @Override
    public Rational getDCMeasurementsRate(ID dcId) throws ControlServiceException {
        List<Object> args = new ArrayList();
        args.add(dcId);
        
        Rational rate;
        
        ControlPlaneMessage m=new ControlPlaneMessage(ControlOperation.GET_DC_RATE, args);
        
        try {
            ZMQControlEndPointMetaData dstAddr = (ZMQControlEndPointMetaData)infoPlaneDelegate.getDCAddressFromID(dcId);
            MetaData mData = new ZMQControlMetaData(dstAddr.getId().toString());    
            rate = (Rational) synchronousTransmit(m, mData);
        } catch (IOException | DCNotFoundException | ControlPlaneConsumerException ex) {
            LOGGER.error("Error while performing getDCMeasurementsRate command " + ex.getMessage());
            throw new ControlServiceException(ex);
          }
        return rate;
    }

    @Override
    public ID loadReporter(ID dataConsumerID, String reporterClassName, Object... reporterArgs) throws ControlServiceException {
        List<Object> args = new ArrayList();
        args.add(dataConsumerID);
        args.add(reporterClassName);
        args.add(reporterArgs);
        
        ID reporterID = null;
        
        ControlPlaneMessage m=new ControlPlaneMessage(ControlOperation.LOAD_REPORTER, args);
        
        try {
            ZMQControlEndPointMetaData dstAddr = (ZMQControlEndPointMetaData)infoPlaneDelegate.getDCAddressFromID(dataConsumerID);
            MetaData mData = new ZMQControlMetaData(dstAddr.getId().toString());
            //we return the ID of the new created reporter as result
            reporterID = (ID) synchronousTransmit(m, mData);
        } catch (IOException | DCNotFoundException | ControlPlaneConsumerException ex) {
            LOGGER.error("Error while performing loadReporter command " + ex.getMessage());
            throw new ControlServiceException(ex);
          }
        return reporterID;
    }
    
    
    @Override
    public boolean unloadReporter(ID reporterID) throws ControlServiceException {
        List<Object> args = new ArrayList();
        args.add(reporterID);
        Boolean result = false;
        
        ControlPlaneMessage m=new ControlPlaneMessage(ControlOperation.UNLOAD_REPORTER, args);
        try {
            ZMQControlEndPointMetaData dstAddr = (ZMQControlEndPointMetaData)infoPlaneDelegate.getDCAddressFromReporterID(reporterID);
            MetaData mData = new ZMQControlMetaData(dstAddr.getId().toString());
            result = (Boolean) synchronousTransmit(m, mData);
        } catch (IOException | DCNotFoundException | ReporterNotFoundException | ControlPlaneConsumerException ex) {
            LOGGER.error("Error while performing unloadReporter command " + ex.getMessage());
            throw new ControlServiceException(ex);
          }
        return result;
    }
    
}
