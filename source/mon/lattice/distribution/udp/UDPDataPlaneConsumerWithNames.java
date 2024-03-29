// UDPDataPlaneConsumerWithNames.java
// Author: Stuart Clayman
// Email: sclayman@ee.ucl.ac.uk
// Date: Feb 2010

package mon.lattice.distribution.udp;

import mon.lattice.distribution.MeasurementDecoderWithNames;
import mon.lattice.distribution.ConsumerMeasurementWithMetaData;
import mon.lattice.distribution.MessageMetaData;
import mon.lattice.distribution.MetaData;
import mon.lattice.distribution.Receiving;
import mon.lattice.xdr.XDRDataInputStream;
import mon.lattice.distribution.MeasurementDecoder;
import mon.lattice.core.plane.MessageType;
import mon.lattice.core.plane.DataPlane;
import mon.lattice.core.Measurement;
import mon.lattice.core.MeasurementReporting;
import mon.lattice.core.ID;
import mon.lattice.core.TypeException;
import java.io.DataInput;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;

public class UDPDataPlaneConsumerWithNames extends AbstractUDPDataPlaneConsumer implements DataPlane, MeasurementReporting, Receiving {
    /**
     * Construct a UDPDataPlaneConsumerWithNames.
     */
    public UDPDataPlaneConsumerWithNames(InetSocketAddress addr) {
        super(addr);
    }

    public UDPDataPlaneConsumerWithNames(int port) {
        super(port);
    }

    /**
     * This method is called just after a packet
     * has been received from some underlying transport
     * at a particular address.
     * The expected message is XDR encoded and it's structure is:
     * +---------------------------------------------------------------------+
     * | data source id (2 X long) | msg type (int) | seq no (int) | payload |
     * +---------------------------------------------------------------------+
     */
    public void received(ByteArrayInputStream bis, MetaData metaData) throws  IOException, TypeException {

	try {
	    DataInput dataIn = new XDRDataInputStream(bis);

	    //System.err.println("DC: datainputstream available = " + dataIn.available());

	    // get the DataSource id
            long dataSourceIDMSB = dataIn.readLong();
            long dataSourceIDLSB = dataIn.readLong();
	    ID dataSourceID = new ID(dataSourceIDMSB, dataSourceIDLSB);

	    // check message type
	    int type = dataIn.readInt();

	    MessageType mType = MessageType.lookup(type);

	    // delegate read to right object
	    if (mType == null) {
		//System.err.println("type = " + type);
		return;
	    }

	    // get seq no
	    int seq = dataIn.readInt();

	    /*
	     * Check the DataSource seq no.
	     */
	    if (seqNoMap.containsKey(dataSourceID)) {
		// we've seen this DataSource before
		int prevSeqNo = seqNoMap.get(dataSourceID);

		if (seq == prevSeqNo + 1) {
		    // we got the correct message from that DataSource
		    // save this seqNo
		    seqNoMap.put(dataSourceID, seq);
		} else {
		    // a DataSource message is missing
		    // TODO: decide what to do
		    // currently: save this seqNo
		    seqNoMap.put(dataSourceID, seq);
		}
	    } else {
		// this is a new DataSource
		seqNoMap.put(dataSourceID, seq);
	    }

	    // Message meta data
	    MessageMetaData msgMetaData = new MessageMetaData(dataSourceID, seq, mType);

	    // read object and check it's type
	    switch (mType) {

	    case ANNOUNCE:
		System.err.println("ANNOUNCE not implemented yet!");
		break;

	    case MEASUREMENT:
		// decode the bytes into a measurement object
		MeasurementDecoder decoder = new MeasurementDecoderWithNames();
		Measurement measurement = decoder.decode(dataIn);

		if (measurement instanceof ConsumerMeasurementWithMetaData) {
		    // add the meta data into the Measurement
		    ((ConsumerMeasurementWithMetaData)measurement).setMessageMetaData(msgMetaData);
		    ((ConsumerMeasurementWithMetaData)measurement).setTransmissionMetaData(metaData);
		}

		
		//System.err.println("DC: datainputstream left = " + dataIn.available());
		// report the measurement
		report(measurement);
		//System.err.println("DC: m = " + measurement);
		break;
	    }


	} catch (IOException ioe) {
	    System.err.println("DataConsumer: failed to process measurement input. The Measurement data is likely to be bad.");
	    throw ioe;
	} catch (Exception e) {
	    System.err.println("DataConsumer: failed to process measurement input. The Measurement data is likely to be bad.");
            throw new TypeException(e.getMessage());
        }
    }

}
